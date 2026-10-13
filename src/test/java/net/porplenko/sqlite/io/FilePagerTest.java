package net.porplenko.sqlite.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilePagerTest {
    private FilePager pager;

    static Path resource(String fileInResource) throws URISyntaxException {
        URL url = FilePager.class.getResource(fileInResource);
        assertThat(url).isNotNull();
        return Path.of(url.toURI());
    }

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        pager = FilePager.open(resource("/sample-dbs/sample.db"));
    }

    @AfterEach
    void tearDown() throws IOException {
        pager.close();
    }

    @Test
    void readFirstPage() {
        ByteBuffer actual = pager.readPage(1);
        assertThat(pager.getPageSize()).isEqualTo(4096);
        assertThat(actual.remaining()).isEqualTo(pager.getPageSize());
        String greeting = StandardCharsets.UTF_8.decode(actual.slice(0, 16)).toString();
        assertThat(greeting).isEqualTo("SQLite format 3\0");
    }

    @Test
    void readNotExistPageThrow() {
        assertThatThrownBy(() -> pager.readPage(100))
            .isInstanceOf(UncheckedIOException.class)
            .hasMessageContaining("Can not read page");
    }

    @Test
    void readZeroPageNumThrow() {
        assertThatThrownBy(() -> pager.readPage(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid page number");
    }

    @Test
    void readLastPage() {
        ByteBuffer actual = pager.readPage(4);
        assertThat(pager.getPageSize()).isEqualTo(4096);
        assertThat(actual.remaining()).isEqualTo(pager.getPageSize());
    }

    @Test
    void readPageWithNoPageSizeThrow() throws Exception {
        assertThatThrownBy(() ->
            FilePager.open(resource("/sample-dbs/empty_file.db")))
            .isInstanceOf(EOFException.class)
            .hasMessageContaining("Can not read page size for path");
    }
}
