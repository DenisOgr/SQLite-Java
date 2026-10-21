package net.porplenko.sqlite.record;

import java.util.stream.Stream;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SerialTypeTest {

    static Stream<Arguments> provideSuccessCodes() {
        return Stream.of(
            Arguments.of(0L, SerialType.Kind.NULL, 0),
            Arguments.of(1L, SerialType.Kind.INT_8, 1),
            Arguments.of(2L, SerialType.Kind.INT_16, 2),
            Arguments.of(3L, SerialType.Kind.INT_24, 3),
            Arguments.of(4L, SerialType.Kind.INT_32, 4),
            Arguments.of(5L, SerialType.Kind.INT_48, 6),
            Arguments.of(6L, SerialType.Kind.INT_64, 8),
            Arguments.of(7L, SerialType.Kind.DOUBLE, 8),
            Arguments.of(8L, SerialType.Kind.ZERO, 0),
            Arguments.of(9L, SerialType.Kind.ONE, 0),
            Arguments.of(12L, SerialType.Kind.BLOB, 0),
            Arguments.of(13L, SerialType.Kind.TEXT, 0),
            Arguments.of(Integer.MAX_VALUE, SerialType.Kind.TEXT, 1073741817)
        );
    }

    static Stream<Arguments> provideErrorCodes() {
        return Stream.of(
            Arguments.of(-1L, CorruptDatabaseException.class, "Invalid serial type code: negative. Value"),
            Arguments.of(10L, CorruptDatabaseException.class, "Invalid serial type code: use reserved number"),
            Arguments.of(11L, CorruptDatabaseException.class, "Invalid serial type code: use reserved number"),
            Arguments.of(Long.MAX_VALUE, CorruptDatabaseException.class, "Invalid serial type code"),
            Arguments.of(Long.MIN_VALUE, CorruptDatabaseException.class, "Invalid serial type code: negative. Value"),
            Arguments.of(1L << 31, CorruptDatabaseException.class, "Invalid serial type code")
        );
    }

    @ParameterizedTest
    @MethodSource("provideSuccessCodes")
    void fromCodeSuccess(long code, SerialType.Kind kind) {
        assertThat(SerialType.Kind.fromCode(code)).isEqualTo(kind);
    }

    @ParameterizedTest
    @MethodSource("provideErrorCodes")
    void fromCodeError(long code, Class<? extends Throwable> e, String message) {
        assertThatThrownBy(() -> SerialType.Kind.fromCode(code)).isInstanceOf(e).hasMessageContaining(message);

    }

    @ParameterizedTest
    @MethodSource("provideSuccessCodes")
    void ofSuccess(long code, SerialType.Kind kind, int size) {
        assertThat(SerialType.of(code)).isEqualTo(new SerialType(kind, size));
    }

    @Test
    void ofSuccessText() {
        assertThat(SerialType.of(27)).isEqualTo(new SerialType(SerialType.Kind.TEXT, (27 - 13) / 2));
    }

    @Test
    void ofSuccessBlob() {
        assertThat(SerialType.of(44)).isEqualTo(new SerialType(SerialType.Kind.BLOB, (44 - 12) / 2));
    }

    @ParameterizedTest
    @MethodSource("provideErrorCodes")
    void ofCodeError(long code, Class<? extends Throwable> e, String message) {
        assertThatThrownBy(() -> SerialType.of(code)).isInstanceOf(e).hasMessageContaining(message);

    }
}
