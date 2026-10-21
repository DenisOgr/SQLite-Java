package net.porplenko.sqlite.record;

import java.nio.ByteBuffer;

class ConverterBytesToSignLong {
    static long convert(ByteBuffer buf, int size) {
        if (size < 1 || size > 7) {
            throw new IllegalArgumentException("Invalid size, allowed: 1-7, provided %d".formatted(size));
        }
        long result = 0;
        for (int i = 0; i < size; i++) {
            result = (result << 8) | (buf.get() & 0xFF);
        }
        int shift = Long.SIZE - 8 * size;
        return (result << shift) >> shift;
    }
}
