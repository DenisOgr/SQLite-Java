package net.porplenko.sqlite.io;

import java.io.Closeable;
import java.nio.ByteBuffer;

public interface Pager extends Closeable {
    ByteBuffer readPage(int pageNum);

    int getPageSize();
}
