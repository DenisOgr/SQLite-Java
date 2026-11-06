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
            SchemaEntry[] schemas = schema.getSchemas();
            assertThat(schemas.length).isEqualTo(2);

            SchemaEntry superhero = schemas[0];
            assertThat(superhero.name()).isEqualTo("superheroes");
            assertThat(superhero.type()).isEqualTo(SchemaEntry.SchemaEntryType.TABLE);
            assertThat(superhero.tblName()).isEqualTo("superheroes");
            assertThat(superhero.rootPage()).isEqualTo(2);
            assertThat(superhero.sql().startsWith("CREATE TABLE \"superheroes\"")).isTrue();

            SchemaEntry sqlite_sequences = schemas[1];
            assertThat(sqlite_sequences.name()).isEqualTo("sqlite_sequence");
            assertThat(sqlite_sequences.type()).isEqualTo(SchemaEntry.SchemaEntryType.TABLE);
            assertThat(sqlite_sequences.tblName()).isEqualTo("sqlite_sequence");
            assertThat(sqlite_sequences.rootPage()).isEqualTo(3);
            assertThat(sqlite_sequences.sql()).isEqualTo("CREATE TABLE sqlite_sequence(name,seq)");
        }
    }
}
