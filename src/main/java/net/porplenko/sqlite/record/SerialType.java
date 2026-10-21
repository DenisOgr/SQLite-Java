package net.porplenko.sqlite.record;

import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;

public record SerialType(Kind kind, int size) {
    public static SerialType of(long code) {
        Kind kind = Kind.fromCode(code);
        int size = switch (kind) {
            case ONE, ZERO, NULL -> 0;
            case INT_8 -> 1;
            case INT_16 -> 2;
            case INT_24 -> 3;
            case INT_32 -> 4;
            case INT_48 -> 6;
            case INT_64, DOUBLE -> 8;
            case BLOB -> ((int) code - 12) / 2;
            case TEXT -> ((int) code - 13) / 2;
        };
        return new SerialType(kind, size);
    }

    public enum Kind {
        NULL, INT_8, INT_16, INT_24, INT_32, INT_48, INT_64, DOUBLE, ZERO, ONE, BLOB, TEXT;

        public static Kind fromCode(long code) {
            if (code < 0L) {
                throw new CorruptDatabaseException(
                    "Invalid serial type code: negative. Value: %d".formatted(code));
            }
            if (code == 10L || code == 11L) {
                throw new CorruptDatabaseException(
                    "Invalid serial type code: use reserved number. Value: %d".formatted(code));
            }
            if (code > Integer.MAX_VALUE) {
                throw new CorruptDatabaseException(
                    "Invalid serial type code: greater than integer max value. Value: %d".formatted(code));
            }
            return switch ((int) code) {
                case 0 -> NULL;
                case 1 -> INT_8;
                case 2 -> INT_16;
                case 3 -> INT_24;
                case 4 -> INT_32;
                case 5 -> INT_48;
                case 6 -> INT_64;
                case 7 -> DOUBLE;
                case 8 -> ZERO;
                case 9 -> ONE;
                default -> {
                    if (code % 2 == 0) {
                        yield BLOB;
                    }
                    yield TEXT;
                }
            };
        }
    }
}
