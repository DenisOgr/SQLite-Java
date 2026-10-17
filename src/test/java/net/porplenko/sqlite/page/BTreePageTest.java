package net.porplenko.sqlite.page;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;
import net.porplenko.sqlite.io.FilePager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BTreePageTest {
    private FilePager pager;

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        URL url = FilePager.class.getResource("/sample-dbs/superheroes.db");
        assertThat(url).isNotNull();
        pager = FilePager.open(Path.of(url.toURI()));
    }

    @AfterEach
    void tearDown() throws IOException {
        pager.close();
    }

    @Test
    void createWithInvalidSizeBuf() {
        ByteBuffer buf = ByteBuffer.allocate(5);
        assertThatThrownBy(() -> new BTreePage(buf, 1))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid page size");
    }

    @Test
    void createWithInvalidPageNum() {
        ByteBuffer buf = ByteBuffer.allocate(120);
        assertThatThrownBy(() -> new BTreePage(buf, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid page number");

        assertThatThrownBy(() -> new BTreePage(buf, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid page number");
    }

    @Test
    void createWithInvalidHeader() {
        ByteBuffer buf = ByteBuffer.allocate(120);
        assertThatThrownBy(() -> new BTreePage(buf, 2))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid header");
    }


    @Test
    void createFromFilePage1() {
        ByteBuffer buf = pager.readPage(1);
        BTreePage page = new BTreePage(buf, 1);
        assertThat(page.getPageNum()).isEqualTo(1);
        BTreePage.Header header = page.getHeader();
        assertThat(header.type()).isEqualTo(BTreePageType.LEAF_TABLE);
        assertThat(header.cellCount()).isEqualTo(2);
        int[] pointers = page.getCellPointers();
        assertThat(pointers).hasSize(2);
        assertThat(pointers[0]).isEqualTo(3726);
        assertThat(pointers[1]).isEqualTo(3959);
    }

    @Test
    void createFromFilePage2() {
        ByteBuffer buf = pager.readPage(2);
        BTreePage page = new BTreePage(buf, 2);
        assertThat(page.getPageNum()).isEqualTo(2);
        BTreePage.Header header = page.getHeader();
        assertThat(header.type()).isEqualTo(BTreePageType.INTERIOR_TABLE);
        assertThat(header.cellCount()).isEqualTo(108);
        int[] pointers = page.getCellPointers();
        assertThat(pointers).hasSize(108);
        assertThat(pointers[0]).isEqualTo(4091);
        assertThat(pointers[1]).isEqualTo(4086);
    }

    @Test
    void getCellPointersTest() {
        ByteBuffer buf = pager.readPage(58);
        BTreePage page = new BTreePage(buf, 58);
        int[] pointers = page.getCellPointers();
        assertThat(pointers).hasSize(14);
        assertThat(pointers[0]).isEqualTo(4086);
        assertThat(pointers[13]).isEqualTo(3956);
    }

    @Test
    void getHeader() {
        ByteBuffer buf = pager.readPage(8);
        BTreePage page = new BTreePage(buf, 8);
        BTreePage.Header header = page.getHeader();
        assertThat(header.type()).isEqualTo(BTreePageType.LEAF_TABLE);
        assertThat(header.cellCount()).isEqualTo(65);
        assertThat(header.getSize()).isEqualTo(8);
        assertThat(header.rightMostPointer()).isEqualTo(0);
    }

    @Test
    void getCellPointerTest() {
        ByteBuffer buf = pager.readPage(8);
        BTreePage page = new BTreePage(buf, 8);
        assertThat(page.getCellPointer(0)).isEqualTo(4009);
        assertThat(page.getCellPointer(1)).isEqualTo(3966);
        assertThat(page.getCellPointer(64)).isEqualTo(177);
    }

    @Test
    void getInvalidCellPointerTest() {
        ByteBuffer buf = pager.readPage(8);
        BTreePage page = new BTreePage(buf, 8);
        assertThatThrownBy(() -> page.getCellPointer(65))
            .isInstanceOf(IndexOutOfBoundsException.class)
            .hasMessageContaining("Invalid index for cell.");
    }

    @Test
    void getPageNum() {
        ByteBuffer buf = pager.readPage(10);
        BTreePage page = new BTreePage(buf, 10);
        assertThat(page.getPageNum()).isEqualTo(10);
    }

    // header tests
    @Test
    void createHeaderInvalidBuf() {
        ByteBuffer buf = ByteBuffer.allocate(2);
        assertThatThrownBy(() -> BTreePage.Header.parse(buf))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid header (small size)");
    }

    @Test
    void createHeaderInvalidBufInteriorPage() {
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put((byte) 0x05);
        assertThatThrownBy(() -> BTreePage.Header.parse(buf))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid header (small size, should be 12).");
    }

    @Test
    void createHeaderSuccessInteriorTablePage() {
        ByteBuffer buf = pager.readPage(2);
        BTreePage.Header header = BTreePage.Header.parse(buf.slice(0, 12));
        assertThat(header.type()).isEqualTo(BTreePageType.INTERIOR_TABLE);
        assertThat(header.cellCount()).isEqualTo(108);
        assertThat(header.rightMostPointer()).isEqualTo(232);
        assertThat(header.getSize()).isEqualTo(12);
    }

    @Test
    void createHeaderSuccessInteriorIndexPage() {
        ByteBuffer buf = pager.readPage(58);
        BTreePage.Header header = BTreePage.Header.parse(buf.slice(0, 12));
        assertThat(header.type()).isEqualTo(BTreePageType.INTERIOR_INDEX);
        assertThat(header.cellCount()).isEqualTo(14);
        assertThat(header.rightMostPointer()).isEqualTo(301);
        assertThat(header.getSize()).isEqualTo(12);
    }

    @Test
    void createHeaderSuccessLeafTablePage() {
        ByteBuffer buf = pager.readPage(4);
        BTreePage.Header header = BTreePage.Header.parse(buf.slice(0, 8));
        assertThat(header.type()).isEqualTo(BTreePageType.LEAF_TABLE);
        assertThat(header.cellCount()).isEqualTo(57);
        assertThat(header.rightMostPointer()).isEqualTo(0);
        assertThat(header.getSize()).isEqualTo(8);
    }

    @Test
    void createHeaderSuccessLeafIndexPage() {
        ByteBuffer buf = pager.readPage(7);
        BTreePage.Header header = BTreePage.Header.parse(buf.slice(0, 8));
        assertThat(header.type()).isEqualTo(BTreePageType.LEAF_INDEX);
        assertThat(header.cellCount()).isEqualTo(448);
        assertThat(header.rightMostPointer()).isEqualTo(0);
        assertThat(header.getSize()).isEqualTo(8);
    }
}
