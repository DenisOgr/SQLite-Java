package net.porplenko.sqlite.record;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;
import net.porplenko.sqlite.page.TextEncoding;


public sealed interface Value {


    static Value parse(SerialType type, ByteBuffer buf, TextEncoding encoding) {
        if (buf.remaining() < type.size()) {
            throw new CorruptDatabaseException(
                "Not enough bytes for %s, need: %d, remain: %d".formatted(
                    type.kind(), type.size(), buf.remaining()));
        }
        return switch (type.kind()) {
            case NULL -> new Null();
            case INT_8 -> new Int(buf.get());
            case INT_16 -> new Int(buf.getShort());
            case INT_24 -> new Int(ConverterBytesToSignLong.convert(buf, 3));
            case INT_32 -> new Int(buf.getInt());
            case INT_48 -> new Int(ConverterBytesToSignLong.convert(buf, 6));
            case INT_64 -> new Int(buf.getLong());
            case DOUBLE -> new Double(buf.getDouble());
            case ZERO -> new Int(0);
            case ONE -> new Int(1);
            case BLOB -> {
                byte[] tmp = new byte[type.size()];
                buf.get(tmp);
                yield new Blob(tmp);
            }
            case TEXT -> {
                Charset charset = switch (encoding) {
                    case TextEncoding.UTF_8 -> StandardCharsets.UTF_8;
                    case UTF_16_LE -> StandardCharsets.UTF_16LE;
                    case UTF_16_BE -> StandardCharsets.UTF_16BE;
                };
                byte[] tmp = new byte[type.size()];
                buf.get(tmp);
                yield new Text(charset.decode(ByteBuffer.wrap(tmp)).toString());
            }
        };
    }

    record Null() implements Value {
    }

    record Int(long value) implements Value {
    }

    record Double(double value) implements Value {
    }

    record Text(String value) implements Value {
    }

    record Blob(byte[] value) implements Value {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Blob blob = (Blob) o;
            return Objects.deepEquals(value, blob.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return "Blob{" +
                "value=" + Arrays.toString(value) +
                '}';
        }
    }
}
