package net.porplenko.sqlite.record;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.stream.Stream;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;
import net.porplenko.sqlite.page.TextEncoding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueTest {
    static Stream<Arguments> provideBytesToSignLong() {
        return Stream.of(
            // size 1
            Arguments.of("00", 1, 0L),                                        // zero
            Arguments.of("7F", 1, 127L),                                      // max positive
            Arguments.of("80", 1, -128L),                                     // min negative
            Arguments.of("FF", 1, -1L),                                       // all ones
            // size 2
            Arguments.of("01 00", 2, 256L),                                  // big-endian order
            Arguments.of("7F FF", 2, 32767L),                                 // max
            Arguments.of("80 00", 2, -32768L),                                // min
            Arguments.of("FF FF", 2, -1L),
            // size 3
            Arguments.of("01 02 03", 3, 0x010203L),                           // byte order (66051)
            Arguments.of("00 80 00", 3,
                32768L),                              // high bit of a middle byte must not sign-extend
            Arguments.of("7F FF FF", 3, 8388607L),                            // max 24-bit
            Arguments.of("80 00 00", 3, -8388608L),                           // min 24-bit
            Arguments.of("FF FF FF", 3,
                -1L),                                 // main sign-extension case (bug result: 16777215)
            // size 4
            Arguments.of("00 00 00 80", 4, 128L),                             // high bit of the last byte
            Arguments.of("7F FF FF FF", 4, (long) Integer.MAX_VALUE),
            Arguments.of("80 00 00 00", 4, (long) Integer.MIN_VALUE),
            Arguments.of("FF FF FF FF", 4, -1L),                              // (bug result: 4294967295)

            // size 5
            Arguments.of("01 02 03 04 05", 5, 0x0102030405L),
            Arguments.of("80 02 03 04 05", 5, -549722061819L),

            // size 6
            Arguments.of("00 00 80 00 00 00", 6,
                0x80000000L),                // int overflow trap: (b & 0xFF) << 24 in int arithmetic
            Arguments.of("00 00 00 00 00 80", 6, 128L),
            Arguments.of("7F FF FF FF FF FF", 6, 0x7FFFFFFFFFFFL),            // max 48-bit
            Arguments.of("80 00 00 00 00 00", 6, -0x800000000000L),           // min 48-bit
            Arguments.of("FF FF FF FF FF FE", 6, -2L),
            Arguments.of("FF FF FF FF FF FF", 6, -1L),

            // size 7
            Arguments.of("01 02 03 04 05 06 07", 7, 283686952306183L),
            Arguments.of("80 02 03 04 05 06 07", 7, -36026585043368441L)
        );
    }

    static Stream<Arguments> providerParseSuccess() {
        return Stream.of(
            Arguments.of(new SerialType(SerialType.Kind.ZERO, 0), "FF FF", TextEncoding.UTF_8, new Value.Int(0)),
            Arguments.of(new SerialType(SerialType.Kind.ONE, 0), "FF FF", TextEncoding.UTF_8, new Value.Int(1)),
            Arguments.of(new SerialType(SerialType.Kind.NULL, 0), "FF FF", TextEncoding.UTF_8, new Value.Null()),
            Arguments.of(new SerialType(SerialType.Kind.INT_8, 1), "FF FF A1", TextEncoding.UTF_8, new Value.Int(-1)),
            Arguments.of(new SerialType(SerialType.Kind.INT_16, 2), "FF 1F FF A2", TextEncoding.UTF_8,
                new Value.Int(-225)),
            Arguments.of(new SerialType(SerialType.Kind.INT_24, 3), "1F 1F FF A2 FF 1F FF A2", TextEncoding.UTF_8,
                new Value.Int(2039807)),
            Arguments.of(new SerialType(SerialType.Kind.INT_32, 4), "1F 1F FF A2 FF 1F FF A2", TextEncoding.UTF_8,
                new Value.Int(522190754)),
            Arguments.of(new SerialType(SerialType.Kind.INT_48, 6), "1F 1F FF A2 FF 1F FF A2", TextEncoding.UTF_8,
                new Value.Int(34222293319455L)),
            Arguments.of(new SerialType(SerialType.Kind.INT_64, 8), "01 01 01 01 01 01 01 01", TextEncoding.UTF_8,
                new Value.Int(72340172838076673L)),
            Arguments.of(new SerialType(SerialType.Kind.DOUBLE, 8), "40 08 80 00 00 00 00 00", TextEncoding.UTF_8,
                new Value.Double(3.0625)),
            Arguments.of(new SerialType(SerialType.Kind.BLOB, 6),
                "00 4E 00 6F 00 74",
                TextEncoding.UTF_8,
                new Value.Blob(new byte[] {0x00, 0x4E, 0x00, 0x6F, 0x00, 0x74}))
        );
    }

    static Stream<Arguments> providerTextParseSuccess() {
        return Stream.of(
            Arguments.of(new SerialType(SerialType.Kind.TEXT, 13), "hello, Denys!".getBytes(StandardCharsets.UTF_8),
                TextEncoding.UTF_8,
                new Value.Text("hello, Denys!")),
            Arguments.of(new SerialType(SerialType.Kind.TEXT, 22), "hey, Denys!".getBytes(StandardCharsets.UTF_16BE),
                TextEncoding.UTF_16_BE, new Value.Text("hey, Denys!")),
            Arguments.of(new SerialType(SerialType.Kind.TEXT, 48),
                "nice to meet you, Denys!".getBytes(StandardCharsets.UTF_16LE),
                TextEncoding.UTF_16_LE, new Value.Text("nice to meet you, Denys!")),
            Arguments.of(new SerialType(SerialType.Kind.TEXT, 29),
                "Résumé of François Müller".getBytes(StandardCharsets.UTF_8),
                TextEncoding.UTF_8, new Value.Text("Résumé of François Müller"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideBytesToSignLong")
    void convertBytesToSignLongTest(String hex, int size, long result) {
        assertThat(
            ConverterBytesToSignLong.convert(ByteBuffer.wrap(HexFormat.ofDelimiter(" ").parseHex(hex)),
                size)).isEqualTo(
            result);
    }

    @Test
    void convertBytesToSignLongInvalidTest() {
        ByteBuffer buf = ByteBuffer.allocate(0);
        assertThatThrownBy(() -> ConverterBytesToSignLong.convert(buf, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid size, allowed: 1-7, provided 0");

        ByteBuffer buf2 = ByteBuffer.allocate(8);
        assertThatThrownBy(() -> ConverterBytesToSignLong.convert(buf2, 8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid size, allowed: 1-7, provided 8");
    }

    @ParameterizedTest
    @MethodSource("providerParseSuccess")
    void parseSuccess(SerialType type, String hex, TextEncoding encoding, Value result) {
        ByteBuffer buf = ByteBuffer.wrap(HexFormat.ofDelimiter(" ").parseHex(hex));
        int position = buf.position();

        assertThat(Value.parse(type, buf, encoding)).isEqualTo(result);
        assertThat(buf.position()).isEqualTo(position + type.size());
    }

    @ParameterizedTest
    @MethodSource("providerTextParseSuccess")
    void parseTextSuccess(SerialType type, byte[] raw, TextEncoding encoding, Value result) {
        ByteBuffer buf = ByteBuffer.wrap(raw);
        int position = 0;
        assertThat(Value.parse(type, buf, encoding)).isEqualTo(result);
        assertThat(buf.position()).isEqualTo(position + type.size());
    }

    @Test
    void parseSmallBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(2);
        assertThatThrownBy(
            () -> Value.parse(new SerialType(SerialType.Kind.INT_32, 4), buf, TextEncoding.UTF_8))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining(
                "Not enough bytes for INT_32, need: 4, remain: 2");
    }

    @Test
    void parseMalformedChars() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] {0x48, (byte) 0xFF, 0x69});
        Value actual = Value.parse(new SerialType(SerialType.Kind.TEXT, 3), buf, TextEncoding.UTF_8);
        assertThat(actual).isEqualTo(new Value.Text("H\uFFFDi"));
    }
}
