package net.porplenko.sqlite.page;

import java.nio.ByteBuffer;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;

public class BTreePage {
    private final DatabaseHeader dbHeader;
    private final ByteBuffer buf;
    private final long pageNum;
    private final Header header;
    private final int TABLE_LEAF_MAX_LOCAL_OVERHEAD = 35;

    public BTreePage(ByteBuffer buf, long pageNum, DatabaseHeader dbHeader) {
        this.dbHeader = dbHeader;
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

    public DatabaseHeader getDbHeader() {
        return dbHeader;
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

    public LeafTableCell getCell(int i) {
        if (header.type != BTreePageType.LEAF_TABLE) {
            throw new UnsupportedOperationException(
                "The page type is not lead table. Current implementation can work only with  lead table page type.");
        }
        if (i < header.getSize() || i > buf.limit()) {
            throw new IllegalArgumentException(
                "The position (%d) for cell is invalid.".formatted(i));
        }
        ByteBuffer newBuffer = buf.duplicate();
        newBuffer.position(i);
        long payloadSize = Varint.read(newBuffer);
        if (payloadSize > (dbHeader.pageSize() - TABLE_LEAF_MAX_LOCAL_OVERHEAD - dbHeader.reservedSpacePerPage())) {
            throw new UnsupportedOperationException(
                "The page is overflow. Current implementation can not works with overflow page.");
        }
        if (newBuffer.position() + payloadSize > newBuffer.limit()) {
            throw new CorruptDatabaseException(
                "Invalid size of the cell with position %d. The payload size exceed the buffer limit.".formatted(i));
        }
        return new LeafTableCell(Varint.read(newBuffer), newBuffer.slice(newBuffer.position(), (int) payloadSize));
    }

    public record LeafTableCell(long rowId, ByteBuffer payload) {
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
