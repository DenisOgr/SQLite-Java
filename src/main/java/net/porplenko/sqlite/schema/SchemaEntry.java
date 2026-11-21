package net.porplenko.sqlite.schema;

import java.util.Locale;
import java.util.Optional;
import net.porplenko.sqlite.common.exceptions.CorruptDatabaseException;
import net.porplenko.sqlite.record.Value;

public record SchemaEntry(SchemaEntryType type, String name, String tblName, long rootPage, String sql) {
    public static SchemaEntry parse(Value[] values) {
        if (values.length != 5) {
            throw new CorruptDatabaseException(
                "Invalid record size. Expected number of columns 5, actual %d.".formatted(
                    values.length));
        }

        long rootPage = number(values[3], "rootpage");
        if (rootPage < 0) {
            throw new CorruptDatabaseException("The column `rootpage` is invalid: negative value.");
        }
        SchemaEntryType type = entityType(values[0], "type");

        // the order of columns are fixed from doc.
        return new SchemaEntry(
            type,
            text(values[1], "name"),
            text(values[2], "tbl_name"),
            rootPage,
            nullableText(values[4], "sql")
        );
    }

    private static SchemaEntryType entityType(Value v, String col) {
        return switch (v) {
            case Value.Text(String inner) -> {
                try {
                    yield SchemaEntryType.valueOf(inner.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new CorruptDatabaseException(
                        "Column `%s` has invalid value. Unknown type.".formatted(col), e);
                }
            }
            default -> throw new CorruptDatabaseException(
                "Column `%s` has invalid type. Should be Value.Text, got %s.".formatted(col, v));
        };
    }

    private static String text(Value v, String col) {
        return switch (v) {
            case Value.Text(String inner) -> inner;
            default -> throw new CorruptDatabaseException(
                "Column `%s` has invalid type. Should be Value.Text, got %s.".formatted(col, v));
        };
    }

    private static long number(Value v, String col) {
        return switch (v) {
            case Value.Int(long inner) -> inner;
            default -> throw new CorruptDatabaseException(
                "Column `%s` has invalid type. Should be Value.Int, got %s.".formatted(col, v));
        };
    }

    private static String nullableText(Value v, String col) {
        return switch (v) {
            case Value.Text(String inner) -> inner;
            case Value.Null() -> null;
            default -> throw new CorruptDatabaseException(
                "Column `%s` has invalid type. Should be Value.Text or Value.Null, got %s.".formatted(col, v));
        };
    }


    public boolean isTableOrView() {
        return type == SchemaEntryType.TABLE || type == SchemaEntryType.VIEW;
    }

    public Optional<String> getSqlOptional() {
        return Optional.ofNullable(sql);
    }

    public enum SchemaEntryType {
        TABLE, INDEX, VIEW, TRIGGER;

    }
}
