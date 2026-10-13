package net.porplenko.sqlite.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FilePager implements Pager {
    private static final int PAGE_SIZE_OFFSET = 16;
    private static final int PAGE_SIZE_ENCODED_AS_ONE = 1;
    private static final int PAGE_SIZE_MAX = 65536;
    private final Path path;
    private final FileChannel file;
    private final int pageSize;

    private FilePager(Path path, FileChannel file, int pageSize) {
        this.path = path;
        this.file = file;
        this.pageSize = pageSize;
    }

    public static FilePager open(Path path) throws IOException {
        FileChannel file = FileChannel.open(path, StandardOpenOption.READ);
        try {
            ByteBuffer pageSizeBuf = ByteBuffer.allocate(2);
            int pageSizeReadBytes = file.read(pageSizeBuf, PAGE_SIZE_OFFSET);
            if (pageSizeReadBytes != pageSizeBuf.capacity()) {
                throw new EOFException("Can not read page size for path: " + path);
            }
            int rawPageSize = pageSizeBuf.getShort(0) & 0xFFFF;
            int pageSize = rawPageSize == PAGE_SIZE_ENCODED_AS_ONE ? PAGE_SIZE_MAX : rawPageSize;

            return new FilePager(path, file, pageSize);
        } catch (Throwable e) {
            try {
                file.close();
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }
    }

    @Override
    public ByteBuffer readPage(int pageNum) {
        if (pageNum < 1) {
            throw new IllegalArgumentException(String.format("Invalid page number: %d.", pageNum));
        }
        ByteBuffer buf = ByteBuffer.allocate(this.pageSize);
        try {
            long position = (long) (pageNum - 1) * this.pageSize;
            // file.read can return less than this.pageSize even when file and request are correct
            // as future improvement use loop-approach to handle this issue
            int readBytes = file.read(buf, position);
            if (readBytes != this.pageSize) {
                throw new EOFException(String.format(
                    "Can not read page #%d from the file %s. Expected bytes: %d, actual: %d", pageNum, this.path,
                    this.pageSize, readBytes
                ));
            }
            buf.position(0);
        } catch (IOException e) {
            throw new UncheckedIOException("Can not read page #" + pageNum, e);
        }
        return buf;
    }

    @Override
    public int getPageSize() {
        return this.pageSize;
    }

    @Override
    public void close() throws IOException {
        this.file.close();
    }
}
