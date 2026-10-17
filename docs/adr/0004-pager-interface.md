# ADR-0004 · Pager behind an interface (where the file I/O boundary lies)

- **Status:** Accepted
- **Date:** 2026-10-12

## Context

The b-tree code needs the bytes of page N. It must not know where they come from: a file in
production, or a buffer in memory in tests. A file brings OS resources that must be closed,
I/O errors, and a `FileChannel.read` that may return fewer bytes than requested.

## Decision

- `io.Pager` interface: `ByteBuffer readPage(int pageNum)`, `int getPageSize()`. It extends
  `Closeable`, because upper layers hold a `Pager`, not a `FilePager`.
- `FilePager.open(Path)` opens the file once and reads the page size from the header
  (offset 16, unsigned; raw value 1 means 65536). If this fails, `open` closes the channel itself.
- `open` throws checked `IOException`: only `cli` calls it, and a wrong path is a user mistake
  that `cli` must report clearly. `readPage` throws `UncheckedIOException`: it will be called
  from `Iterator.next()` / `Predicate`, which cannot throw checked exceptions.
- A read that returns fewer bytes than a page is an error (no retry loop).

## Alternatives considered

- The caller passes the page size: the caller would have to read the file itself, so file I/O leaks upward.
- Open the file on every `readPage`: one open/close per page, too slow for a full scan.
- `FileBTree` / `RamBTree`: the tree-walking code would be copied; only the byte source differs.
- Checked exceptions everywhere: they don't fit `Iterator` and `Predicate`.

## Consequences

- (+) One b-tree implementation for any byte source; tests can use an in-memory Pager (not written yet).
- (+) The file format detail (page size location) stays in `io`.
- (−) The file stays open while the Pager lives; the owner must close it.
- (−) Two exception styles in one class: checked `open`, unchecked `readPage`.
- (−) A short read and a truncated file give the same error. A retry loop is a future improvement.
- (−) An in-memory Pager must implement a no-op `close()`.
- (−) Tests for page size 32768 and raw value 1 (65536) are postponed to SJ-3.

## Amended

- Changed type page number in Pager.readPage interface: from int to long. Because the right most pointer in the header
  of interior pages is long (unsigned int on disk).
- 