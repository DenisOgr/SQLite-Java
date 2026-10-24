package net.porplenko.sqlite.schema;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import net.porplenko.sqlite.io.FilePager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaTest {
    @Test
    void getSchemasSuccess() throws URISyntaxException, IOException {
        URL url = FilePager.class.getResource("/sample-dbs/superheroes.db");
        assertThat(url).isNotNull();
        try (FilePager pager = FilePager.open(Path.of(url.toURI()))) {
            Schema schema = new Schema(pager);
            SchemaEntry[] tables = schema.getSchemas();
            assertThat(tables.length).isEqualTo(2);
        }

    }
}
