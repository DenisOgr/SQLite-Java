package net.porplenko.sqlite.schema;

import java.nio.ByteBuffer;
import java.util.Arrays;
import net.porplenko.sqlite.io.Pager;
import net.porplenko.sqlite.page.BTreePage;
import net.porplenko.sqlite.page.DatabaseHeader;
import net.porplenko.sqlite.record.Record;

public class Schema {
    final long PAGE_NUM = 1L;
    final BTreePage page;

    public Schema(Pager pager) {
        ByteBuffer buf = pager.readPage(PAGE_NUM);
        page = new BTreePage(buf, PAGE_NUM, DatabaseHeader.parse(buf));
    }

    public SchemaEntry[] getSchemas() {
        return Arrays.stream(page.getCellPointers())
            .mapToObj(page::getCell)
            .map(cell -> Record.parse(cell.payload(), page.getDbHeader().textEncoding()))
            .map(SchemaEntry::parse)
            .toArray(SchemaEntry[]::new);
    }
}
