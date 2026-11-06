package net.porplenko.sqlite.page;

import java.nio.ByteBuffer;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;

public class BTreePage {
    private final static int TABLE_LEAF_MAX_LOCAL_OVERHEAD = 35;
    private final DatabaseHeader dbHeader;
    private final ByteBuffer buf;
    private final long pageNum;
    private final Header header;


    public BTreePage(ByteBuffer buf, long pageNum, DatabaseHeader dbHeader) {
        if (buf.limit() < dbHeader.pageSizeWithoutReserved()) {
            throw new CorruptDatabaseException(
                "Invalid page size. Actual: %d, required: at least: %d".formatted(buf.limit(),
                    dbHeader.pageSizeWithoutReserved()));
        }
        this.dbHeader = dbHeader;
        if (pageNum < 1) {
            throw new IllegalArgumentException("Invalid page number: " + pageNum);
        }
        this.pageNum = pageNum;
        int dbOffset = getDatabaseOffset();

        if (buf.limit() != dbHeader.pageSize()) {
            throw new CorruptDatabaseException("Invalid page size.");
        }
        this.buf = buf;
        try {
            this.header = Header.parse(buf.slice(dbOffset, buf.limit() - dbOffset));
        } catch (IllegalArgumentException e) {
            throw new CorruptDatabaseException("Invalid header.", e);
        }
        int endOfCellPointers = dbOffset + header.getSize() + header.cellCount() * 2;
        if (this.header.getNormalizedCellContentArea() < endOfCellPointers ||
            this.header.getNormalizedCellContentArea() > this.dbHeader.pageSizeWithoutReserved()) {
            throw new CorruptDatabaseException(
                "Invalid cellContentArea. Actual value: %d".formatted(this.header.getNormalizedCellContentArea()));
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
        int slotOffset = getDatabaseOffset() + header.getSize() + i * 2;
        int cellOffset = Short.toUnsignedInt(buf.getShort(slotOffset));
        if (cellOffset < this.header.getNormalizedCellContentArea() ||
            cellOffset >= this.dbHeader.pageSizeWithoutReserved()) {
            throw new CorruptDatabaseException(
                "The position (%d) for cell is invalid.".formatted(cellOffset));
        }
        return cellOffset;
    }

    public LeafTableCell getCell(int i) {
        if (header.type != BTreePageType.LEAF_TABLE) {
            throw new UnsupportedOperationException(
                "The page type is not lead table. Current implementation can work only with  lead table page type.");
        }

        int offset = getCellPointer(i);

        ByteBuffer newBuffer = buf.duplicate();
        newBuffer.position(offset);
        long payloadSize = Varint.read(newBuffer);
        long rowId = Varint.read(newBuffer);
        int maxLocalPayload = this.dbHeader.pageSizeWithoutReserved() - TABLE_LEAF_MAX_LOCAL_OVERHEAD;

        if (payloadSize > maxLocalPayload) {
            throw new UnsupportedOperationException(
                "The page is overflow. Current implementation can not works with overflow page.");
        }
        if (newBuffer.position() + payloadSize > dbHeader.pageSizeWithoutReserved()) {
            throw new CorruptDatabaseException(
                "Invalid size of the cell with position %d. Payload ends at %d, past the usable page size %d.".formatted(
                    offset, newBuffer.position() + payloadSize, dbHeader.pageSizeWithoutReserved()));
        }
        return new LeafTableCell(rowId, newBuffer.slice(newBuffer.position(), (int) payloadSize));
    }

    public record LeafTableCell(long rowId, ByteBuffer payload) {
    }

    public record Header(BTreePageType type, int cellCount, long rightMostPointer, int cellContentArea) {
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

            int cellContentArea = Short.toUnsignedInt(buf.getShort(5));
            return new Header(type, cellCount, rightMostPointer, cellContentArea);
        }

        public int getSize() {
            return type.getHeaderSize();
        }

        public int getNormalizedCellContentArea() {
            return (cellContentArea == 0) ? 65536 : cellContentArea;
        }
    }
}
