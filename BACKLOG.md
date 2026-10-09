# SQLite-Java — Backlog

A port of the read path of your Rust SQLite clone to Java + one write stage as a bridge into the course.

**Rules of the game:** take the top unblocked ticket → write it yourself → Claude Code mentors →
I review the finished stage. Method: recall the stage from Rust from memory → write it in Java →
a gap = a diagnosis of Java APIs, not of the concept.

**Legend:** *Goal* — what the ticket does · *DoD* — when it's closed · *Java focus* — which APIs
are rusty here and get warmed up · *Blocked by* — dependency.

---

## EPIC A · Foundation

### SJ-1 · Bootstrap + green build
- **Goal:** set up the Maven project; `mvn test` is green, `mvn exec:java` prints usage.
- **DoD:** both commands work on a clean clone.
- **Java focus:** Maven lifecycle, JUnit5, package structure.
- *Done by the scaffold — just verify and close.*

### SJ-2 · Pager: read a page by number
- **Goal:** `interface Pager { ByteBuffer readPage(int pageNumber) }` + `FilePager` over a .db. Page size is taken from the db header (offset 16, big-endian).
- **DoD:** a unit test reads page 1 from a real .db and returns a buffer of the correct size.
- **Java focus:** `RandomAccessFile`/`FileChannel`, `ByteBuffer`, seek arithmetic `(n-1)*pageSize`.
- **Blocked by:** SJ-1.

---

## EPIC B · Header & schema

### SJ-3 · Database header
- **Goal:** parse the first 100 bytes of page 1 (magic, page size, text encoding, page count).
- **DoD:** a test checks the fields against a known .db.
- **Java focus:** `ByteBuffer.order(BIG_ENDIAN)`, `Short/Integer.toUnsignedInt`.
- **Blocked by:** SJ-2.

### SJ-4 · B-tree page header + cell pointer array
- **Goal:** parse the b-tree page header (type: leaf/interior × table/index, cell count, cell content offset) and the cell pointer array. Remember the 100-byte offset on page 1.
- **DoD:** a test extracts N pointers for a known page.
- **Java focus:** unsigned reads, working with offsets inside a buffer.
- **Blocked by:** SJ-3.

### SJ-5 · Varint decoder
- **Goal:** a pure function `long readVarint(ByteBuffer)` — up to 9 bytes, high-bit continuation, the 9th byte is full.
- **DoD:** a parameterized test on boundaries: 0, 127, 128, multi-byte, negative via sign extension.
- **Java focus:** `<<`, `>>>` vs `>>`, masks `& 0x7f`, `long`. Isolated — the best warm-up.
- **Blocked by:** SJ-1. *(independent of B — can be taken early)*

### SJ-6 · Serial types → typed Value
- **Goal:** sealed `Value` (`Null`/`Int`/`Real`/`Text`/`Blob` as records) + a serial type → Value decoder.
- **DoD:** a test for each serial type, including sign-extended integers (1,2,3,4,6,8 bytes) and inline 0/1.
- **Java focus:** sealed interfaces, records, pattern-matching `switch`, sign extension.
- **Blocked by:** SJ-5.

### SJ-7 · sqlite_master → schema
- **Goal:** read the schema from page 1: `name`, `type`, `rootpage`, `sql` for every table/index.
- **Milestone:** the `.tables` command.
- **DoD:** `.tables` prints the table names of a real .db.
- **Java focus:** composition of SJ-4/5/6.
- **Blocked by:** SJ-4, SJ-6.

---

## EPIC C · Query (read)

### SJ-8 · Table cursor (single leaf)
- **Goal:** `Cursor implements Iterator<Row>` — iterates the rows of a table on a single leaf page (rowid + values).
- **DoD:** a test iterates the rows of a small table (fits in 1 page).
- **Java focus:** `Iterator`/`Iterable`, navigating cells in a `ByteBuffer`.
- **Blocked by:** SJ-7.

### SJ-9 · SELECT COUNT(*)
- **Goal:** count rows via the cursor.
- **Milestone:** `SELECT COUNT(*) FROM t`.
- **DoD:** the number matches `sqlite3`.
- **Java focus:** consuming an iterator.
- **Blocked by:** SJ-8.

### SJ-10 · Interior traversal (multi-leaf full scan)
- **Goal:** walk the interior table b-tree, stitching cursors across all leaves.
- **DoD:** `COUNT(*)` on a table that does NOT fit in 1 page matches `sqlite3`.
- **Java focus:** recursion/stack, iterator composition.
- **Blocked by:** SJ-9.

### SJ-11 · SELECT columns
- **Goal:** column projection by name (mapping via the schema). Minimal parsing of `SELECT a, b FROM t` — a split, **NOT** a real parser.
- **DoD:** prints the requested columns in the correct order.
- **Java focus:** string handling, index ↔ name mapping.
- **Blocked by:** SJ-10.

### SJ-12 · WHERE equality
- **Goal:** a filter iterator over the cursor, `Predicate<Row>`.
- **DoD:** `SELECT ... WHERE col = 'x'` matches `sqlite3` (via full scan).
- **Java focus:** `Predicate`, iterator chaining.
- **Blocked by:** SJ-11.

---

## EPIC D · Index (new compared to the Rust version)

### SJ-13 · Index b-tree cursor
- **Goal:** walk the index b-tree, collect rowids for a given key.
- **DoD:** a test returns the correct rowids for a known value.
- **Java focus:** `Comparable`/`Comparator`, generics with a bound `<K extends Comparable<K>>`.
- **Blocked by:** SJ-12.

### SJ-14 · Index-backed lookup
- **Goal:** if the WHERE column has an index — take rowids from the index, then fetch rows from the table b-tree by rowid, instead of a full scan.
- **Milestone:** index lookup.
- **DoD:** result = the full-scan version, but without walking all leaves (verify with a pages-read counter in Pager).
- **Java focus:** composing two cursors, point lookup by rowid.
- **Blocked by:** SJ-13.

---

## EPIC E · Seam into the course (write path)

### SJ-15 · Writable slotted page (bridge)
- **Goal:** a separate package `page.writable` — a fixed-size page with a page header, slot directory, free-space pointer; `insert`/`get`/`delete`/`update` of variable-length records. This is exactly the **Module-02** homework of the course. Does NOT touch the reader.
- **DoD:** a property test — after an arbitrary sequence of insert/delete, `get` returns correct records and free space is computed correctly.
- **Java focus:** mutable `ByteBuffer`, slot compaction.
- **Blocked by:** — *(independent; can be done in parallel with C/D).* **Stop here — the course takes over.**
