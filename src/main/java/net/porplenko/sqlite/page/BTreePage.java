package net.porplenko.sqlite.page;

import java.nio.ByteBuffer;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;

public class BTreePage {
    private final ByteBuffer buf;
    private final long pageNum;
    private final Header header;

    public BTreePage(ByteBuffer buf, long pageNum) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("Invalid page number: " + pageNum);
        }
        this.pageNum = pageNum;
        int dbOffset = getDatabaseOffset();

        if (buf.limit() < dbOffset + 8) { // 108 or 8 are a minimum sizes of the header (depends on page)
            throw new CorruptDatabaseException("Invalid page size.");
        }
        this.buf = buf;
        try {
            this.header = Header.parse(buf.slice(dbOffset, buf.limit() - dbOffset));
        } catch (IllegalArgumentException e) {
            throw new CorruptDatabaseException("Invalid header.", e);
        }
    }

    private int getDatabaseOffset() {
        return (pageNum == 1) ? 100 : 0;
    }

    public Header getHeader() {
        return header;
    }

    public long getPageNum() {
        return pageNum;
    }

    public int[] getCellPointers() {
        int[] cellPointer = new int[header.cellCount];
        for (int i = 0; i < header.cellCount; i++) {
            cellPointer[i] = getCellPointer(i);
        }
        return cellPointer;
    }

    public int getCellPointer(int i) {
        if (!(i >= 0 && i < header.cellCount)) {
            throw new IndexOutOfBoundsException(
                "Invalid index for cell. Provided: %d should be in range [0..%d]".formatted(i, header.cellCount - 1));
        }
        int offset = getDatabaseOffset() + header.getSize() + i * 2;
        return Short.toUnsignedInt(buf.getShort(offset));
    }

    public record Header(BTreePageType type, int cellCount, long rightMostPointer) {
        static Header parse(ByteBuffer buf) {
            if (buf.limit() < 8) { // 8 is a minimum size of the header
                throw new IllegalArgumentException("Invalid header (small size).");
            }
            BTreePageType type = BTreePageType.fromCode(Byte.toUnsignedInt(buf.get(0)));
            if (buf.limit() < type.getHeaderSize()) {
                throw new IllegalArgumentException(
                    "Invalid header (small size, should be %d).".formatted(type.getHeaderSize()));
            }
            int cellCount = Short.toUnsignedInt(buf.getShort(3));
            long rightMostPointer = (!type.isLeaf()) ?
                Integer.toUnsignedLong(buf.getInt(8)) : 0L;

            return new Header(type, cellCount, rightMostPointer);
        }

        public int getSize() {
            return type.getHeaderSize();
        }
    }
}
