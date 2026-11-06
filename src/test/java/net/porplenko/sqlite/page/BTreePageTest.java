package net.porplenko.sqlite.page;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;
import net.porplenko.sqlite.io.FilePager;
import net.porplenko.sqlite.io.Pager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BTreePageTest {
    private FilePager pager;
    private DatabaseHeader dbHeader;

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        URL url = FilePager.class.getResource("/sample-dbs/superheroes.db");
        assertThat(url).isNotNull();
        pager = FilePager.open(Path.of(url.toURI()));
        dbHeader = DatabaseHeader.parse(pager.readPage(1L));
    }

    @AfterEach
    void tearDown() throws IOException {
        pager.close();
    }

    @Test
    void createWithInvalidSizeBuf() {
        ByteBuffer buf = ByteBuffer.allocate(5);
        assertThatThrownBy(() -> new BTreePage(buf, 1, dbHeader))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid page size");
    }

    @Test
    void createWithInvalidPageNum() {
        ByteBuffer buf = ByteBuffer.allocate(dbHeader.pageSizeWithoutReserved());
        assertThatThrownBy(() -> new BTreePage(buf, 0, dbHeader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid page number");

        assertThatThrownBy(() -> new BTreePage(buf, -1, dbHeader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid page number");
    }

    @Test
    void createWithInvalidHeader() {
        ByteBuffer buf = ByteBuffer.allocate(dbHeader.pageSizeWithoutReserved());
        assertThatThrownBy(() -> new BTreePage(buf, 2, dbHeader))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid header");
    }


    @Test
    void createFromFilePage1() {
        ByteBuffer buf = pager.readPage(1);
        BTreePage page = new BTreePage(buf, 1, dbHeader);
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
        BTreePage page = new BTreePage(buf, 2, dbHeader);
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
        BTreePage page = new BTreePage(buf, 58, dbHeader);
        int[] pointers = page.getCellPointers();
        assertThat(pointers).hasSize(14);
        assertThat(pointers[0]).isEqualTo(4086);
        assertThat(pointers[13]).isEqualTo(3956);
    }

    @Test
    void getHeader() {
        ByteBuffer buf = pager.readPage(8);
        BTreePage page = new BTreePage(buf, 8, dbHeader);
        BTreePage.Header header = page.getHeader();
        assertThat(header.type()).isEqualTo(BTreePageType.LEAF_TABLE);
        assertThat(header.cellCount()).isEqualTo(65);
        assertThat(header.getSize()).isEqualTo(8);
        assertThat(header.rightMostPointer()).isEqualTo(0);
    }

    @Test
    void getCellPointerTest() {
        ByteBuffer buf = pager.readPage(8);
        BTreePage page = new BTreePage(buf, 8, dbHeader);
        assertThat(page.getCellPointer(0)).isEqualTo(4009);
        assertThat(page.getCellPointer(1)).isEqualTo(3966);
        assertThat(page.getCellPointer(64)).isEqualTo(177);
    }

    @Test
    void getInvalidCellPointerTest() {
        ByteBuffer buf = pager.readPage(8);
        BTreePage page = new BTreePage(buf, 8, dbHeader);
        assertThatThrownBy(() -> page.getCellPointer(65))
            .isInstanceOf(IndexOutOfBoundsException.class)
            .hasMessageContaining("Invalid index for cell.");
    }

    @Test
    void getPageNum() {
        ByteBuffer buf = pager.readPage(10);
        BTreePage page = new BTreePage(buf, 10, dbHeader);
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
        assertThat(header.cellContentArea()).isEqualTo(3450);
    }

    @Test
    void createHeaderSuccessInteriorIndexPage() {
        ByteBuffer buf = pager.readPage(58);
        BTreePage.Header header = BTreePage.Header.parse(buf.slice(0, 12));
        assertThat(header.type()).isEqualTo(BTreePageType.INTERIOR_INDEX);
        assertThat(header.cellCount()).isEqualTo(14);
        assertThat(header.rightMostPointer()).isEqualTo(301);
        assertThat(header.getSize()).isEqualTo(12);
        assertThat(header.cellContentArea()).isEqualTo(3956);
    }

    @Test
    void createHeaderSuccessLeafTablePage() {
        ByteBuffer buf = pager.readPage(4);
        BTreePage.Header header = BTreePage.Header.parse(buf.slice(0, 8));
        assertThat(header.type()).isEqualTo(BTreePageType.LEAF_TABLE);
        assertThat(header.cellCount()).isEqualTo(57);
        assertThat(header.rightMostPointer()).isEqualTo(0);
        assertThat(header.getSize()).isEqualTo(8);
        assertThat(header.cellContentArea()).isEqualTo(146);
    }

    @Test
    void createHeaderSuccessLeafIndexPage() {
        ByteBuffer buf = pager.readPage(7);
        BTreePage.Header header = BTreePage.Header.parse(buf.slice(0, 8));
        assertThat(header.type()).isEqualTo(BTreePageType.LEAF_INDEX);
        assertThat(header.cellCount()).isEqualTo(448);
        assertThat(header.rightMostPointer()).isEqualTo(0);
        assertThat(header.getSize()).isEqualTo(8);
        assertThat(header.cellContentArea()).isEqualTo(1408);
    }

    @Test
    void getCellForOverflowPage() throws URISyntaxException, IOException {
        URL url = FilePager.class.getResource("/sample-dbs/overflow.db");
        assertThat(url).isNotNull();
        try (Pager pager = FilePager.open(Path.of(url.toURI()))) {
            DatabaseHeader dbHeader = DatabaseHeader.parse(pager.readPage(1L));
            int pageNum = 2;
            ByteBuffer buf = pager.readPage(pageNum);
            BTreePage page = new BTreePage(buf, pageNum, dbHeader);
            assertThatThrownBy(() -> page.getCell(0))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(
                    "The page is overflow. Current implementation can not works with overflow page");
        }
    }

    @Test
    void getSuccessCellForOverflowEdgePage() throws URISyntaxException, IOException {
        URL url = FilePager.class.getResource("/sample-dbs/overflow_edge.db");
        assertThat(url).isNotNull();
        try (Pager pager = FilePager.open(Path.of(url.toURI()))) {
            DatabaseHeader dbHeader = DatabaseHeader.parse(pager.readPage(1L));
            int pageNum = 2;
            ByteBuffer buf = pager.readPage(pageNum);
            BTreePage page = new BTreePage(buf, pageNum, dbHeader);
            BTreePage.LeafTableCell cell = page.getCell(0);
            assertThat(cell.rowId()).isEqualTo(1);
            assertThat(cell.payload().limit()).isEqualTo(4049);
        }
    }

    @Test
    void getFailureCellForOverflowEdgePage() throws URISyntaxException, IOException {
        // created with macOS /usr/bin/sqlite3 (reserved = 12)
        URL url = FilePager.class.getResource("/sample-dbs/overflow_edge.db");
        assertThat(url).isNotNull();
        try (Pager pager = FilePager.open(Path.of(url.toURI()))) {
            DatabaseHeader dbHeader = DatabaseHeader.parse(pager.readPage(1L));
            assertThat(dbHeader.reservedSpacePerPage())
                .as("created with macOS /usr/bin/sqlite3 (reserved=12)")
                .isEqualTo(12);
            int pageNum = 3;
            ByteBuffer buf = pager.readPage(pageNum);
            BTreePage page = new BTreePage(buf, pageNum, dbHeader);
            assertThatThrownBy(() -> page.getCell(0))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(
                    "The page is overflow. Current implementation can not works with overflow page");
        }
    }

    @ParameterizedTest
    @CsvSource({
        "7", // Index Leaf
        "2", // Table Interior
        "58" // Index Interior
    })
    void getCellForUnsupportedPageTypes(int pageNum) {
        ByteBuffer buf = pager.readPage(pageNum);
        BTreePage page = new BTreePage(buf, pageNum, dbHeader);
        assertThatThrownBy(() -> page.getCell(0))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining(
                "The page type is not lead table. Current implementation can work only with  lead table page type.");
    }

    @Test
    void getCellSuccess() {
        int pageNum = 4;
        ByteBuffer buf = pager.readPage(pageNum);
        BTreePage page = new BTreePage(buf, pageNum, dbHeader);
        BTreePage.LeafTableCell cell = page.getCell(0);
        assertThat(cell.rowId()).isEqualTo(1);
        assertThat(cell.payload().limit()).isEqualTo(62);
    }

    @Test
    void getInvalidCellIndex() {
        int pageNum = 4;
        ByteBuffer buf = pager.readPage(pageNum);
        BTreePage page = new BTreePage(buf, pageNum, dbHeader);
        assertThatThrownBy(() -> page.getCell(-1))
            .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> page.getCell(page.getHeader().cellCount()))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void getDBHeaderTest() {
        int pageNum = 4;
        ByteBuffer buf = pager.readPage(pageNum);
        BTreePage page = new BTreePage(buf, pageNum, dbHeader);
        assertThat(page.getDbHeader()).isEqualTo(dbHeader);
    }
}
