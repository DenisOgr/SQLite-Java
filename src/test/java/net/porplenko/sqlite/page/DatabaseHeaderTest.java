package net.porplenko.sqlite.page;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;
import net.porplenko.sqlite.io.FilePager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseHeaderTest {
    private static FilePager filePager;
    ByteBuffer headerBytes;

    @BeforeAll
    static void initPager() throws URISyntaxException, IOException {
        URL url = FilePager.class.getResource("/sample-dbs/sample.db");
        assertThat(url).isNotNull();
        filePager = FilePager.open(Path.of(url.toURI()));
    }

    @AfterAll
    static void closePager() throws IOException {
        if (filePager != null) {
            filePager.close();
        }
    }

    @BeforeEach
    void initHeader() {
        headerBytes = filePager.readPage(1).slice(0, 100);
    }

    @Test
    void parseRealValidFile() {
        DatabaseHeader header = DatabaseHeader.parse(headerBytes);

        assertThat(header.pageSize()).isEqualTo(4096);
        assertThat(header.pageCount()).isEqualTo(4);
        assertThat(header.textEncoding()).isEqualTo(TextEncoding.UTF_8);
        assertThat(header.reservedSpacePerPage()).isEqualTo(0);
    }

    @Test
    void comparePagerAndHeaderPageSize() {
        DatabaseHeader header = DatabaseHeader.parse(headerBytes);
        assertThat(header.pageSize()).isEqualTo(filePager.getPageSize());
    }

    @Test
    void parse32768PageSize() {
        headerBytes.put(16, (byte) 0x80);
        headerBytes.put(17, (byte) 0x00);
        DatabaseHeader header = DatabaseHeader.parse(headerBytes);
        assertThat(header.pageSize()).isEqualTo(32768);
    }

    @Test
    void parse65536PageSize() {
        headerBytes.putShort(16, (short) 1);
        DatabaseHeader header = DatabaseHeader.parse(headerBytes);
        assertThat(header.pageSize()).isEqualTo(65536);
    }

    @Test
    void parse100PageSize() {
        headerBytes.putShort(16, (short) 100);
        assertThatThrownBy(() -> DatabaseHeader.parse(headerBytes))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid page size");
    }

    @Test
    void parseNotPowerOfTwoPageSize() {
        headerBytes.putShort(16, (short) 513);
        assertThatThrownBy(() -> DatabaseHeader.parse(headerBytes))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid page size");
    }

    @Test
    void parseWrongMagic() {
        headerBytes.putLong(0, 1234567890L);
        assertThatThrownBy(() -> DatabaseHeader.parse(headerBytes))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("The file is not a database (invalid magic).");
    }

    @Test
    void parseUnknownEncoding() {
        headerBytes.putInt(56, 10);
        assertThatThrownBy(() -> DatabaseHeader.parse(headerBytes))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("The file is not a database (invalid text encoding code).");
    }

    @Test
    void parseLargeCount() {
        // add OxFF FF FF FF to the position 28-31
        for (int pos = 28; pos <= 31; pos++) {
            headerBytes.put(pos, (byte) 0xff);
        }
        DatabaseHeader header = DatabaseHeader.parse(headerBytes);
        assertThat(header.pageCount()).isEqualTo(4294967295L);
    }

    @Test
    void parseInValidBuffer() {
        assertThatThrownBy(() -> DatabaseHeader.parse(ByteBuffer.allocate(99)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid buffer size");
    }

    @Test
    void parseReservedSpacePerPageNotZero() {
        headerBytes.put(20, (byte) 0xFF);
        DatabaseHeader header = DatabaseHeader.parse(headerBytes);
        assertThat(header.reservedSpacePerPage()).isEqualTo(255);
    }

    @Test
    void parseInvalidLimitBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.limit(10);
        assertThatThrownBy(() -> {
            DatabaseHeader.parse(buf);
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid buffer size");
    }
}
