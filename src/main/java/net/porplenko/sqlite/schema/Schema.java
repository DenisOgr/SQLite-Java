package net.porplenko.sqlite.schema;

import java.util.ArrayList;
import net.porplenko.sqlite.btree.Cursor;
import net.porplenko.sqlite.io.Pager;
import net.porplenko.sqlite.page.DatabaseHeader;

public class Schema {
    final private static long PAGE_NUM = 1L;
    final private Pager pager;
    final private DatabaseHeader databaseHeader;

    public Schema(Pager pager) {
        this.pager = pager;
        databaseHeader = DatabaseHeader.parse(pager.readPage(PAGE_NUM));

    }

    public SchemaEntry[] getSchemas() {
        Cursor cursor = new Cursor(pager, PAGE_NUM, databaseHeader);
        ArrayList<SchemaEntry> entries = new ArrayList<>();
        while (cursor.hasNext()) {
            entries.add(SchemaEntry.parse(cursor.next().values()));
        }
        return entries.toArray(SchemaEntry[]::new);
    }
}
