package net.porplenko.sqlite.page;

public enum BTreePageType {
    INTERIOR_INDEX(2, 12), INTERIOR_TABLE(5, 12),
    LEAF_INDEX(10, 8), LEAF_TABLE(13, 8);

    private final int code;
    private final int headerSize;

    BTreePageType(int code, int headerSize) {
        this.code = code;
        this.headerSize = headerSize;
    }

    public static BTreePageType fromCode(int code) {
        for (BTreePageType pageType : values()) {
            if (pageType.code == code) {
                return pageType;
            }
        }
        throw new IllegalArgumentException("Invalid code %d for page type.".formatted(code));
    }

    public boolean isLeaf() {
        return headerSize == 8;
    }

    public boolean isTable() {
        return code == 5 || code == 13;
    }

    public int getHeaderSize() {
        return headerSize;
    }
}
