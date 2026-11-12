package org.example.features.taxreturn.evidence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Google OAuth 2.0 / Drive API v3 を java.net.http で呼び出す実装。
 * 接続先は Google の固定ホストのみで、利用者指定の URL は扱わない。
 */
@Component
public class GoogleDriveHttpGateway implements DriveGateway {
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String FILES_URL = "https://www.googleapis.com/drive/v3/files";
    private static final String UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files";
    private static final Pattern FILE_ID = Pattern.compile("[A-Za-z0-9_-]{1,200}");
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final GoogleDriveSettings settings;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final SecureRandom random = new SecureRandom();

    public GoogleDriveHttpGateway(GoogleDriveSettings settings) {
        this.settings = settings;
    }

    @Override
    public String authorizationUrl(String state, String codeChallenge) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", settings.getClientId());
        params.put("redirect_uri", settings.getRedirectUri());
        params.put("response_type", "code");
        params.put("scope", GoogleDriveSettings.DRIVE_FILE_SCOPE);
        params.put("state", state);
        params.put("code_challenge", codeChallenge);
        params.put("code_challenge_method", "S256");
        params.put("access_type", "offline");
        params.put("prompt", "consent");
        return AUTH_URL + "?" + form(params);
    }

    @Override
    public TokenGrant exchangeCode(String code, String codeVerifier) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code);
        params.put("client_id", settings.getClientId());
        params.put("client_secret", settings.getClientSecret());
        params.put("redirect_uri", settings.getRedirectUri());
        params.put("grant_type", "authorization_code");
        params.put("code_verifier", codeVerifier);
        JsonNode json = postForm(TOKEN_URL, params, "認可コードの交換");
        return new TokenGrant(text(json, "refresh_token"), text(json, "access_token"), text(json, "scope"));
    }

    @Override
    public String accessToken(String refreshToken) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", settings.getClientId());
        params.put("client_secret", settings.getClientSecret());
        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");
        String token = text(postForm(TOKEN_URL, params, "アクセストークンの更新"), "access_token");
        if (token == null) {
            throw new DriveException("Google のアクセストークンを取得できませんでした。", false);
        }
        return token;
    }

    @Override
    public String createFolder(String accessToken, String name) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "name", name, "mimeType", "application/vnd.google-apps.folder"));
            HttpRequest request = authorized(accessToken, FILES_URL + "?fields=id")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            return requireFileId(text(readJson(send(request), "フォルダー作成"), "id"));
        } catch (IOException ex) {
            throw new DriveException("フォルダー作成の要求を作成できませんでした。", false);
        }
    }

    @Override
    public StoredFile upload(String accessToken, String folderId, String name, String mimeType, byte[] content) {
        requireFileId(folderId);
        try {
            byte[] boundaryBytes = new byte[16];
            random.nextBytes(boundaryBytes);
            String boundary = "evidence-" + HexFormat.of().formatHex(boundaryBytes);
            String metadata = mapper.writeValueAsString(Map.of(
                    "name", name, "mimeType", mimeType, "parents", List.of(folderId)));
            ByteArrayOutputStream body = new ByteArrayOutputStream(content.length + 1024);
            body.writeBytes(("--" + boundary + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"
                    + metadata + "\r\n--" + boundary + "\r\nContent-Type: " + mimeType + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.writeBytes(content);
            body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest request = authorized(accessToken, UPLOAD_URL + "?uploadType=multipart&fields=id,size")
                    .header("Content-Type", "multipart/related; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();
            JsonNode json = readJson(send(request), "アップロード");
            return new StoredFile(requireFileId(text(json, "id")), parseLong(text(json, "size")));
        } catch (IOException ex) {
            throw new DriveException("アップロード要求を作成できませんでした。", false);
        }
    }

    @Override
    public RemoteStatus status(String accessToken, String fileId) {
        HttpRequest request = authorized(accessToken,
                FILES_URL + "/" + requireFileId(fileId) + "?fields=id,trashed,size").GET().build();
        HttpResponse<InputStream> response = send(request);
        if (response.statusCode() == 404) {
            discard(response);
            return new RemoteStatus(RemoteState.MISSING, 0);
        }
        JsonNode json = readJson(response, "ファイル状態の確認");
        boolean trashed = json.path("trashed").asBoolean(false);
        return new RemoteStatus(trashed ? RemoteState.TRASHED : RemoteState.PRESENT, parseLong(text(json, "size")));
    }

    @Override
    public byte[] download(String accessToken, String fileId, long maxBytes) {
        HttpRequest request = authorized(accessToken,
                FILES_URL + "/" + requireFileId(fileId) + "?alt=media").GET().build();
        HttpResponse<InputStream> response = send(request);
        if (response.statusCode() != 200) {
            discard(response);
            throw failure(response.statusCode(), "ダウンロード");
        }
        try (InputStream in = response.body()) {
            byte[] data = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, maxBytes + 1));
            if (data.length > maxBytes) {
                throw new DriveException("保管ファイルが上限サイズを超えています。", false);
            }
            return data;
        } catch (IOException ex) {
            throw new DriveException("ダウンロード中に通信が切断されました。", false);
        }
    }

    @Override
    public void delete(String accessToken, String fileId) {
        HttpRequest request = authorized(accessToken, FILES_URL + "/" + requireFileId(fileId)).DELETE().build();
        HttpResponse<InputStream> response = send(request);
        discard(response);
        int status = response.statusCode();
        if (status != 204 && status != 200 && status != 404) {
            throw failure(status, "削除");
        }
    }

    @Override
    public void revoke(String token) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(REVOKE_URL))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(Map.of("token", token))))
                .build();
        discard(send(request));
    }

    private HttpRequest.Builder authorized(String accessToken, String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + accessToken);
    }

    private JsonNode postForm(String url, Map<String, String> params, String action) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(params)))
                .build();
        HttpResponse<InputStream> response = send(request);
        if (response.statusCode() == 400 || response.statusCode() == 401) {
            String error = "";
            try (InputStream in = response.body()) {
                error = text(mapper.readTree(in.readNBytes(16 * 1024)), "error");
            } catch (IOException ignored) {
                // 応答本文は記録しない。
            }
            boolean revoked = "invalid_grant".equals(error);
            throw new DriveException(action + "に失敗しました。"
                    + (revoked ? "Google Drive の連携をやり直してください。" : ""), revoked);
        }
        return readJson(response, action);
    }

    private HttpResponse<InputStream> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException ex) {
            throw new DriveException("Google Drive に接続できませんでした。時間をおいて再試行してください。", false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DriveException("Google Drive への通信が中断されました。", false);
        }
    }

    private JsonNode readJson(HttpResponse<InputStream> response, String action) {
        if (response.statusCode() / 100 != 2) {
            discard(response);
            throw failure(response.statusCode(), action);
        }
        try (InputStream in = response.body()) {
            return mapper.readTree(in.readNBytes(256 * 1024));
        } catch (IOException ex) {
            throw new DriveException(action + "の応答を読み取れませんでした。", false);
        }
    }

    private DriveException failure(int status, String action) {
        if (status == 401) {
            return new DriveException(action + "の認可が無効です。Google Drive の連携をやり直してください。", true);
        }
        if (status == 403) {
            return new DriveException(action + "が拒否されました。権限または利用上限を確認してください。", false);
        }
        if (status == 404) {
            return new DriveException(action + "の対象が Google Drive に見つかりません。", false);
        }
        return new DriveException(action + "に失敗しました（HTTP " + status + "）。", false);
    }

    private static void discard(HttpResponse<InputStream> response) {
        try (InputStream in = response.body()) {
            in.skip(Long.MAX_VALUE);
        } catch (IOException ignored) {
            // 破棄のみ。
        }
    }

    private static String form(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json == null ? null : json.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static long parseLong(String value) {
        try {
            return value == null ? -1 : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    static String requireFileId(String id) {
        if (id == null || !FILE_ID.matcher(id).matches()) {
            throw new DriveException("Google Drive のファイルIDが不正です。", false);
        }
        return id;
    }
}
