package net.porplenko.sqlite.schema;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;
import net.porplenko.sqlite.io.Pager;
import net.porplenko.sqlite.page.BTreePage;
import net.porplenko.sqlite.page.DatabaseHeader;
import net.porplenko.sqlite.record.Record;

public class Schema {
    final private static long PAGE_NUM = 1L;
    final private BTreePage page;

    public Schema(Pager pager) {
        ByteBuffer buf = pager.readPage(PAGE_NUM);
        page = new BTreePage(buf, PAGE_NUM, DatabaseHeader.parse(buf));
    }

    public SchemaEntry[] getSchemas() {
        return IntStream.range(0, page.getHeader().cellCount())
            .mapToObj(page::getCell)
            .map(cell -> Record.parse(cell.payload(), page.getDbHeader().textEncoding()))
            .map(SchemaEntry::parse)
            .toArray(SchemaEntry[]::new);
    }
}
