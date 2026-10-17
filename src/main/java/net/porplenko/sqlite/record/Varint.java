package net.porplenko.sqlite.record;

import java.nio.ByteBuffer;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;

/**
 * Utility for reading SQLite variable-length integers (varints).
 *
 * <p>A varint is 1–9 bytes. Each of the first 8 bytes contributes 7 bits;
 * the high bit signals that more bytes follow. The 9th byte, if reached,
 * contributes all 8 bits.
 *
 * @see <a href="https://www.sqlite.org/fileformat.html#varint">SQLite file format — varint</a>
 */
public final class Varint {
    private Varint() {
    }

    /**
     * Reads one varint from {@code buf} at its current position, advancing it accordingly.
     * On return, the position is right after the last varint byte.
     *
     * @param buf source buffer, positioned at the first varint byte
     * @return the decoded 64-bit signed integer
     * @throws CorruptDatabaseException if the buffer is exhausted before the varint is complete
     */
    public static long read(ByteBuffer buf) {
        long result = 0L;
        for (int i = 1; i < 9; i++) {
            if (!buf.hasRemaining()) {
                throw new CorruptDatabaseException("Invalid varint value (too short, does not have finish byte).");
            }
            byte b = buf.get();
            result = (result << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        if (!buf.hasRemaining()) {
            throw new CorruptDatabaseException("Invalid varint value (too short, does not have finish byte).");
        }
        result = (result << 8) | (buf.get() & 0xFF);
        return result;
    }
}
