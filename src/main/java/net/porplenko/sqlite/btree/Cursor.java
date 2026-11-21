package net.porplenko.sqlite.btree;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.porplenko.sqlite.io.Pager;
import net.porplenko.sqlite.page.BTreePage;
import net.porplenko.sqlite.page.BTreePageType;
import net.porplenko.sqlite.page.DatabaseHeader;
import net.porplenko.sqlite.record.Record;

public class Cursor implements Iterator<Row> {
    final private BTreePage page;
    private int curCellNum = 0;

    public Cursor(Pager pager, long rootPage, DatabaseHeader databaseHeader) {
        page = new BTreePage(pager.readPage(rootPage), rootPage, databaseHeader);
        if (page.getHeader().type() != BTreePageType.LEAF_TABLE) {
            throw new UnsupportedOperationException(
                "The page type is not lead table. Current implementation can work only with  lead table page type.");
        }
    }

    @Override
    public boolean hasNext() {
        return page.getHeader().cellCount() > curCellNum;
    }

    @Override
    public Row next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more rows.");
        }
        BTreePage.LeafTableCell cell = page.getCell(curCellNum);
        curCellNum++;
        return new Row(cell.rowId(), Record.parse(cell.payload(), page.getDbHeader().textEncoding()).values());
    }
}
