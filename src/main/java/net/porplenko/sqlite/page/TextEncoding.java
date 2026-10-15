package net.porplenko.sqlite.page;

public enum TextEncoding {
    UTF_8(1), UTF_16_LE(2), UTF_16_BE(3);
    private final int code;

    TextEncoding(int code) {
        this.code = code;
    }

    public static TextEncoding fromCode(int code) {
        for (TextEncoding encoding : values()) {
            if (encoding.code == code) {
                return encoding;
            }
        }
        throw new IllegalArgumentException("Invalid code: %d for database text encoding.".formatted(code));
    }

}
