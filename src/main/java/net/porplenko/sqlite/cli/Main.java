package net.porplenko.sqlite.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import net.porplenko.sqlite.io.FilePager;
import net.porplenko.sqlite.io.Pager;
import net.porplenko.sqlite.schema.Schema;
import net.porplenko.sqlite.schema.SchemaEntry;

/**
 * Bootstrap placeholder. The real CLI (.tables, SELECT ...) arrives with SJ-7+.
 * You write the storage logic — see BACKLOG.md.
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("usage: sqlite-java <path-to-db> <command>");
            return;
        }
        try (Pager pager = FilePager.open(Path.of(args[0]))) {
            switch (args[1]) {
                case ".tables" -> {
                    Schema schema = new Schema(pager);
                    Arrays.stream(schema.getSchemas())
                        .filter(SchemaEntry::isTableOrView)
                        .filter(entry -> !entry.name().startsWith("sqlite"))
                        .sorted(Comparator.comparing(SchemaEntry::name))
                        .forEach(entry -> System.out.println(entry.name()));
                }
                default -> throw new UnsupportedOperationException("Invalid command: %s".formatted(args[1]));
            }

        }

    }
}
