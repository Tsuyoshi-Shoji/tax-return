package org.example.features.taxreturn.evidence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * 証憑ファイルの受入検査。拡張子と実際の形式（先頭バイト）が一致する PDF/JPEG/PNG のみを受け付ける。
 * マルウェア検査は行わないため、PDF の能動的機能（JavaScript・起動・埋込みファイル）を含むものは拒否する。
 */
public final class EvidenceUploadValidator {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "pdf", "application/pdf",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png");
    private static final String[] PDF_ACTIVE_CONTENT = {"/JavaScript", "/Launch", "/EmbeddedFile", "/RichMedia"};

    private EvidenceUploadValidator() {
    }

    public static Result validate(String originalName, byte[] content) {
        if (content == null || content.length == 0) {
            return Result.error("ファイルを選択してください。");
        }
        if (content.length > MAX_BYTES) {
            return Result.error("ファイルサイズは10MB以下にしてください。");
        }
        String name = displayName(originalName);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        String declared = EXTENSIONS.get(extension);
        if (declared == null) {
            return Result.error("PDF・JPEG・PNG（拡張子 .pdf/.jpg/.jpeg/.png）のみアップロードできます。");
        }
        String detected = detect(content);
        if (!declared.equals(detected)) {
            return Result.error("ファイルの内容が拡張子と一致しません。PDF・JPEG・PNG の実ファイルを選択してください。");
        }
        if ("application/pdf".equals(detected) && containsActivePdfContent(content)) {
            return Result.error("JavaScript・外部起動・埋込みファイル等を含む PDF は受け付けません。印刷や画像化した PDF を使用してください。");
        }
        return new Result(null, name, detected, "application/pdf".equals(detected) ? "pdf"
                : "image/png".equals(detected) ? "png" : "jpg", sha256(content));
    }

    static String detect(byte[] c) {
        if (c.length >= 5 && c[0] == '%' && c[1] == 'P' && c[2] == 'D' && c[3] == 'F' && c[4] == '-') {
            return "application/pdf";
        }
        if (c.length >= 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (c.length >= 8 && (c[0] & 0xFF) == 0x89 && c[1] == 'P' && c[2] == 'N' && c[3] == 'G'
                && c[4] == 0x0D && c[5] == 0x0A && c[6] == 0x1A && c[7] == 0x0A) {
            return "image/png";
        }
        return null;
    }

    private static boolean containsActivePdfContent(byte[] content) {
        String latin = new String(content, StandardCharsets.ISO_8859_1);
        for (String marker : PDF_ACTIVE_CONTENT) {
            if (latin.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /** 表示専用の元ファイル名。パス・制御文字を除去し長さを制限する（保管名には使わない）。 */
    public static String displayName(String originalName) {
        String name = originalName == null ? "" : originalName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        name = name.substring(slash + 1).replaceAll("[\\p{Cntrl}\\p{Cf}]", "").strip();
        if (name.length() > 200) {
            name = name.substring(name.length() - 200);
        }
        return name.isEmpty() ? "evidence" : name;
    }

    public static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record Result(String error, String displayName, String mimeType, String extension, String sha256) {
        static Result error(String message) {
            return new Result(message, null, null, null, null);
        }

        public boolean ok() {
            return error == null;
        }
    }
}
