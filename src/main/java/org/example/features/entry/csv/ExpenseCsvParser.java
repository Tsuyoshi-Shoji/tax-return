package org.example.features.entry.csv;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 支出CSVインポートのフォーマット定義と構文解析。
 * 値の妥当性（マスタ・日付・金額等）は EntryService 側で検証する。
 */
public final class ExpenseCsvParser {
    public static final long MAX_BYTES = 1024L * 1024;
    public static final int MAX_ROWS = 500;
    public static final List<String> HEADERS = List.of(
            "区分", "項目", "支出発生日", "金額", "支払方法", "申告対象外", "家事按分", "支出詳細");

    private static final int MAX_CELL_LENGTH = 1024;
    private static final int MAX_ERRORS = 100;
    private static final Charset WINDOWS_31J = Charset.forName("Windows-31J");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private ExpenseCsvParser() {
    }

    /** Excel で開いても文字化けしないよう BOM 付き UTF-8 で返す。「#」で始まる行は取込時に無視される。 */
    public static byte[] templateBytes() {
        String body = String.join(",", HEADERS) + "\r\n"
                + "# 記入例（「#」で始まる行は取り込まれません。この2行は削除して構いません）\r\n"
                + "# 経費,消耗品費,2026-01-15,1980,クレジットカード,0,0,事務用品の購入\r\n";
        byte[] text = body.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[UTF8_BOM.length + text.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(text, 0, result, UTF8_BOM.length, text.length);
        return result;
    }

    public static Result parse(byte[] content) {
        List<String> errors = new ArrayList<>();
        if (content == null || content.length == 0) {
            return Result.failed("CSVファイルが空です。");
        }
        if (content.length > MAX_BYTES) {
            return Result.failed("CSVファイルは1MB以下にしてください。");
        }
        String text = decode(content);
        if (text == null) {
            return Result.failed("文字コードを判別できません。UTF-8 または Shift_JIS で保存したCSVファイルを選択してください。");
        }
        if (text.indexOf('\0') >= 0) {
            return Result.failed("CSV形式のファイルではありません。");
        }

        List<Record> records;
        try {
            records = tokenize(text);
        } catch (IllegalArgumentException ex) {
            return Result.failed(ex.getMessage());
        }

        List<ExpenseCsvRow> rows = new ArrayList<>();
        boolean headerFound = false;
        for (Record record : records) {
            if (record.isBlank() || record.firstCell().startsWith("#")) {
                continue;
            }
            List<String> cells = record.trimmedCells();
            if (!headerFound) {
                if (!HEADERS.equals(cells)) {
                    return Result.failed(record.lineNumber + "行目の見出しがフォーマットと一致しません。"
                            + "CSVフォーマットをダウンロードして使用してください。");
                }
                headerFound = true;
                continue;
            }
            if (cells.size() != HEADERS.size()) {
                errors.add(record.lineNumber + "行目: 列数が" + HEADERS.size() + "列ではありません（" + cells.size() + "列）。");
            } else if (cells.stream().anyMatch(cell -> cell.length() > MAX_CELL_LENGTH)) {
                errors.add(record.lineNumber + "行目: 値が長すぎます。");
            } else {
                rows.add(new ExpenseCsvRow(record.lineNumber,
                        cells.get(0), cells.get(1), cells.get(2), cells.get(3),
                        cells.get(4), cells.get(5), cells.get(6), cells.get(7)));
            }
            if (rows.size() + errors.size() > MAX_ROWS) {
                return Result.failed("一度に取り込めるのは" + MAX_ROWS + "件までです。ファイルを分割してください。");
            }
            if (errors.size() >= MAX_ERRORS) {
                break;
            }
        }
        if (!headerFound) {
            return Result.failed("見出し行がありません。CSVフォーマットをダウンロードして使用してください。");
        }
        if (errors.isEmpty() && rows.isEmpty()) {
            return Result.failed("登録するデータ行がありません。");
        }
        return errors.isEmpty() ? new Result(rows, List.of()) : new Result(List.of(), errors);
    }

    private static String decode(byte[] content) {
        int offset = startsWithBom(content) ? UTF8_BOM.length : 0;
        ByteBuffer buffer = ByteBuffer.wrap(content, offset, content.length - offset);
        String utf8 = strictDecode(buffer.duplicate(), StandardCharsets.UTF_8);
        if (utf8 != null || offset > 0) {
            return utf8;
        }
        // Excel（日本語版）の「CSV（コンマ区切り）」保存は Shift_JIS 系になるため受け付ける。
        return strictDecode(buffer.duplicate(), WINDOWS_31J);
    }

    private static boolean startsWithBom(byte[] content) {
        return content.length >= 3 && content[0] == UTF8_BOM[0] && content[1] == UTF8_BOM[1] && content[2] == UTF8_BOM[2];
    }

    private static String strictDecode(ByteBuffer buffer, Charset charset) {
        try {
            return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buffer)
                    .toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    /** RFC 4180 準拠（ダブルクォートによる囲み・エスケープ、CRLF/LF/CR 改行）。 */
    private static List<Record> tokenize(String text) {
        List<Record> records = new ArrayList<>();
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        boolean cellQuoted = false;
        int line = 1;
        int recordStartLine = 1;
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < length && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    if (c == '\n' || (c == '\r' && (i + 1 >= length || text.charAt(i + 1) != '\n'))) {
                        line++;
                    }
                    cell.append(c);
                }
                continue;
            }
            if (c == '"') {
                if (cell.length() == 0 && !cellQuoted) {
                    inQuotes = true;
                    cellQuoted = true;
                } else {
                    throw new IllegalArgumentException(line + "行目: ダブルクォートの位置が不正です。");
                }
            } else if (c == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
                cellQuoted = false;
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < length && text.charAt(i + 1) == '\n') {
                    i++;
                }
                cells.add(cell.toString());
                records.add(new Record(recordStartLine, cells));
                cells = new ArrayList<>();
                cell.setLength(0);
                cellQuoted = false;
                line++;
                recordStartLine = line;
            } else {
                if (cellQuoted) {
                    throw new IllegalArgumentException(line + "行目: ダブルクォートの位置が不正です。");
                }
                cell.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException(recordStartLine + "行目: ダブルクォートが閉じられていません。");
        }
        if (cell.length() > 0 || !cells.isEmpty() || cellQuoted) {
            cells.add(cell.toString());
            records.add(new Record(recordStartLine, cells));
        }
        return records;
    }

    private record Record(int lineNumber, List<String> cells) {
        boolean isBlank() {
            return cells.stream().allMatch(value -> value.isBlank());
        }

        String firstCell() {
            return cells.isEmpty() ? "" : cells.get(0).strip();
        }

        /** Excel が付与する末尾の空列は無視する。 */
        List<String> trimmedCells() {
            List<String> trimmed = new ArrayList<>(cells.stream().map(String::strip).toList());
            while (trimmed.size() > HEADERS.size() && trimmed.get(trimmed.size() - 1).isEmpty()) {
                trimmed.remove(trimmed.size() - 1);
            }
            return trimmed;
        }
    }

    public record Result(List<ExpenseCsvRow> rows, List<String> errors) {
        public Result {
            rows = List.copyOf(rows);
            errors = List.copyOf(errors);
        }

        static Result failed(String message) {
            return new Result(List.of(), List.of(message));
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
