package net.porplenko.sqlite.btree;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import net.porplenko.sqlite.io.FilePager;
import net.porplenko.sqlite.page.DatabaseHeader;
import net.porplenko.sqlite.record.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorTest {
    private FilePager pager;
    private DatabaseHeader databaseHeader;

    static Path resource(String fileInResource) throws URISyntaxException {
        URL url = FilePager.class.getResource(fileInResource);
        assertThat(url).isNotNull();
        return Path.of(url.toURI());
    }

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        pager = FilePager.open(resource("/sample-dbs/superheroes.db"));
        databaseHeader = DatabaseHeader.parse(pager.readPage(1L));
    }

    @AfterEach
    void tearDown() throws IOException {
        pager.close();
        databaseHeader = null;
    }

    @Test
    void iterateWithSuccess() throws URISyntaxException, IOException {
        FilePager pagerSampleDb = FilePager.open(resource("/sample-dbs/sample.db"));
        DatabaseHeader databaseHeaderSampleDb = DatabaseHeader.parse(pager.readPage(1L));
        Cursor cursor = new Cursor(pagerSampleDb, 2L, databaseHeaderSampleDb);
        ArrayList<Row> arr = new ArrayList();
        cursor.forEachRemaining(arr::add);
        assertThat(arr.size()).isEqualTo(4);
        Row first = arr.getFirst();
        assertThat(first.rowId()).isEqualTo(1);
        assertThat(first.values()).containsExactly(new Value.Null(), new Value.Text("Granny Smith"),
            new Value.Text("Light Green"));
    }

    @Test
    void callTwoTimesHasNext() {
        Cursor cursor = new Cursor(pager, 4L, databaseHeader);
        boolean is1 = cursor.hasNext();
        boolean is2 = cursor.hasNext();
        assertThat(is1).isTrue();
        assertThat(is2).isTrue();

        Row first = cursor.next();
        assertThat(first.rowId()).isEqualTo(1);
        assertThat(first.values()).containsExactly(
            new Value.Null(),
            new Value.Text("Batman (Bruce Wayne)"),
            new Value.Text("Blue Eyes"),
            new Value.Text("Black Hair"),
            new Value.Int(3093),
            new Value.Text("1939, May"),
            new Value.Text("1939")
        );
    }

    @Test
    void iterateWithNoElementError() {
        Cursor cursor = new Cursor(pager, 4L, databaseHeader);
        while (cursor.hasNext()) {
            cursor.next();
        }
        assertThatThrownBy(cursor::next).isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("No more rows.");

    }

    @Test
    void iterateOverNonSupportedPageType() {
        assertThatThrownBy(() -> new Cursor(pager, 2L, databaseHeader))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("The page type is not lead table");
    }
}
