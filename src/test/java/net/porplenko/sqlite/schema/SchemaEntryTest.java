package net.porplenko.sqlite.schema;

import java.util.Optional;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;
import net.porplenko.sqlite.record.Record;
import net.porplenko.sqlite.record.Value;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SchemaEntryTest {
    @Test
    void parseLesserSizeOfValues() {
        Value[] values = new Value[] {
            new Value.Int(100)
        };

        assertThatThrownBy(() -> SchemaEntry.parse(new Record(values)))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid record size. Expected number of columns 5, actual 1.");
    }

    @Test
    void parseGreaterSizeOfValues() {
        Value[] values = new Value[] {
            new Value.Int(100),
            new Value.Int(101),
            new Value.Int(102),
            new Value.Int(103),
            new Value.Int(104),
            new Value.Int(105),
        };

        assertThatThrownBy(() -> SchemaEntry.parse(new Record(values)))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Invalid record size. Expected number of columns 5, actual 6.");
    }

    @Test
    void parseInvalidTypesOfValues() {
        Value[] values = new Value[] {
            new Value.Int(100),
            new Value.Int(101),
            new Value.Int(102),
            new Value.Int(103),
            new Value.Int(104)
        };

        assertThatThrownBy(() -> SchemaEntry.parse(new Record(values)))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Column `type` has invalid type. Should be Value.Text, got Int[value=100].");
    }

    @Test
    void parseInvalidTypeTest() {
        Value[] values = new Value[] {
            new Value.Text("some_invalid_type"),
            new Value.Text("name1"),
            new Value.Text("name1"),
            new Value.Int(10),
            new Value.Text("CREATE TABLE..")
        };

        assertThatThrownBy(() -> SchemaEntry.parse(new Record(values)))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining(
                "Column `type` has invalid value. Unknown type");
    }

    @Test
    void parseInvalidRootPageTest() {
        Value[] values = new Value[] {
            new Value.Text("table"),
            new Value.Text("name1"),
            new Value.Text("name1"),
            new Value.Int(-100), // invalid
            new Value.Text("CREATE TABLE..")
        };

        assertThatThrownBy(() -> SchemaEntry.parse(new Record(values)))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("The column `root_page` is invalid: negative value.");
    }

    @Test
    void parseInvalidRootPageTypeTest() {
        Value[] values = new Value[] {
            new Value.Text("table"),
            new Value.Text("name1"),
            new Value.Text("name1"),
            new Value.Text("10"), // invalid
            new Value.Text("CREATE TABLE..")
        };

        assertThatThrownBy(() -> SchemaEntry.parse(new Record(values)))
            .isInstanceOf(CorruptDatabaseException.class)
            .hasMessageContaining("Column `root_page` has invalid type. Should be Value.Int, got Text[value=10].");
    }

    @Test
    void parseSuccessTest() {
        Value[] values = new Value[] {
            new Value.Text("table"),
            new Value.Text("name1"),
            new Value.Text("name2"),
            new Value.Int(100),
            new Value.Text("CREATE TABLE..")
        };

        SchemaEntry schema = SchemaEntry.parse(new Record(values));
        assertThat(schema.name()).isEqualTo("name1");
        assertThat(schema.tblName()).isEqualTo("name2");
        assertThat(schema.type()).isEqualTo(SchemaEntry.SchemaEntryType.TABLE);
        assertThat(schema.sql()).isEqualTo("CREATE TABLE..");
        assertThat(schema.rootPage()).isEqualTo(100);
    }

    @Test
    void parseNullableSqlSuccessTest() {
        Value[] values = new Value[] {
            new Value.Text("table"),
            new Value.Text("name1"),
            new Value.Text("name2"),
            new Value.Int(100),
            new Value.Null()
        };
        SchemaEntry schema = SchemaEntry.parse(new Record(values));
        assertThat(schema.sql()).isNull();
        assertThat(schema.getSqlOptional()).isEqualTo(Optional.empty());
    }

}
