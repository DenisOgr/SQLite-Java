package net.porplenko.sqlite.page;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;

public record DatabaseHeader(
    int pageSize, // 16:18
    int reservedSpacePerPage, //20:21
    long pageCount,// 28:32
    TextEncoding textEncoding //56:60
) {
    private static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    public static DatabaseHeader parse(ByteBuffer buf) {
        if (buf.limit() < 100) {
            throw new IllegalArgumentException("Invalid buffer size, expects at least 100, actual: " + buf.limit());
        }
        String magic = StandardCharsets.UTF_8.decode(buf.slice(0, 16)).toString();
        if (!magic.equals("SQLite format 3\u0000")) {
            throw new CorruptDatabaseException("The file is not a database (invalid magic).");
        }
        int pageSize = Short.toUnsignedInt(buf.getShort(16));
        if (pageSize == 1) {
            pageSize = 65536;
        }
        boolean isPageValid = (pageSize == 65536 || (pageSize >= 512 && pageSize <= 32768 && isPowerOfTwo(pageSize)));

        if (!isPageValid) {
            throw new CorruptDatabaseException("Invalid page size.");
        }

        int reservedSpacePerPage = Byte.toUnsignedInt(buf.get(20));
        long pageCount = Integer.toUnsignedLong(buf.getInt(28));

        try {
            TextEncoding textEncoding = TextEncoding.fromCode(buf.getInt(56));
            return new DatabaseHeader(pageSize, reservedSpacePerPage, pageCount, textEncoding);
        } catch (IllegalArgumentException e) {
            throw new CorruptDatabaseException("The file is not a database (invalid text encoding code).", e);
        }
    }
}
