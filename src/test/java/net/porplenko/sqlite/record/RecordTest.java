package net.porplenko.sqlite.record;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import net.porplenko.sqlite.page.TextEncoding;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecordTest {
    @Test
    void parseSuccess() {
        ByteBuffer payload = ByteBuffer.wrap(HexFormat.ofDelimiter(" ").parseHex(
            "08 00 45 1f 21 02 21 15 4a 6f 68 6e 20 43 6f 6e 73 74 61 6e 74 69 6e 65 20 28 4e 65 77 20 45 61 72 74 68 29 "
                +
                "42 6c 75 65 20 45 79 65 73 42 6c 6f 6e 64 20 48 61 69 72 01 73 31 39 38 34 2c 20 4a 75 6e 65 31 39 38 34"));
        Record actualRecord = Record.parse(payload, TextEncoding.UTF_8);
        Value[] values = actualRecord.values();
        assertThat(values.length).isEqualTo(7);
        assertThat(values[0]).isEqualTo(new Value.Null());
        assertThat(values[1]).isEqualTo(new Value.Text("John Constantine (New Earth)"));
        assertThat(values[2]).isEqualTo(new Value.Text("Blue Eyes"));
        assertThat(values[3]).isEqualTo(new Value.Text("Blond Hair"));
        assertThat(values[4]).isEqualTo(new Value.Int(371));
        assertThat(values[5]).isEqualTo(new Value.Text("1984, June"));
        assertThat(values[6]).isEqualTo(new Value.Text("1984"));
    }
}
