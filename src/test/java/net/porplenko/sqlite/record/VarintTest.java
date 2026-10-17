package net.porplenko.sqlite.record;

import java.nio.ByteBuffer;
import java.util.stream.Stream;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VarintTest {
    static Stream<Arguments> source() {
        return Stream.of(
            Arguments.of(ByteBuffer.wrap(new byte[] {0x00}), 0L, 1),
            Arguments.of(ByteBuffer.wrap(new byte[] {0x7F}), 127L, 1),
            Arguments.of(ByteBuffer.wrap(new byte[] {(byte) 0x81, 0x00}), 128L, 2),
            Arguments.of(ByteBuffer.wrap(new byte[] {(byte) 0xFF, 0x7F}), 16383L, 2),
            Arguments.of(ByteBuffer.wrap(new byte[] {(byte) 0x81, (byte) 0x80, 0x00}), 16384L, 3),
            Arguments.of(ByteBuffer.wrap(
                new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0x7F}), 72057594037927935L, 8),
            Arguments.of(ByteBuffer.wrap(
                new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF}), -1L, 9),
            Arguments.of(ByteBuffer.wrap(
                new byte[] {(byte) 0xC0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                    (byte) 0x80, (byte) 0x00}), Long.MIN_VALUE, 9),
            Arguments.of(ByteBuffer.wrap(
                new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                    (byte) 0x80, (byte) 0x01}), 1L, 9),
            Arguments.of(ByteBuffer.wrap(
                new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                    (byte) 0x80, (byte) 0x80}), 128L, 9),
            Arguments.of(ByteBuffer.wrap(
                new byte[] {(byte) 0x05, (byte) 0x99}), 5L, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("source")
    void successRead(ByteBuffer buf, long expectedVal, int expectedPos) {
        assertThat(Varint.read(buf)).isEqualTo(expectedVal);
        assertThat(buf.position()).isEqualTo(expectedPos);
    }

    @Test
    void failureRead() {
        assertThatThrownBy(() -> Varint.read(ByteBuffer.allocate(0)))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid varint value (too short, does not have finish byte).");
        assertThatThrownBy(() -> Varint.read(ByteBuffer.wrap(new byte[] {(byte) 0x81})))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid varint value (too short, does not have finish byte).");

        assertThatThrownBy(() ->
            Varint.read(ByteBuffer.wrap(
                new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                    (byte) 0x80}))).isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid varint value (too short, does not have finish byte).");
    }

    @Test
    void middleBufRead() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] {0x05, (byte) 0x81, 0x00});
        buf.position(1);
        assertThat(Varint.read(buf)).isEqualTo(128);
        assertThat(buf.position()).isEqualTo(3);
    }
}
