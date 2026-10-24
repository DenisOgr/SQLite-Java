# ADR-0008 · Print tables from sqlite_master

- **Status:** Accepted
- **Date:** 2026-10-21

## Context

The table `sqlite_master` is stored on the page #1 with fixed columns order and names. Reading it needs SJ-4 (the page),
SJ-5 (varints) and SJ-6 (values), plus two new formats: the cell format and the record format. The DoD is to match the
output of sqlite3 `.tables`. The SQLite record format is the following: header size (varint), sequence of
serial types (each varint, until header size end), values in the body.

## Decision

- The cli layer does not work with the query layer, it bypasses it. The query layer works only with input SQL queries,
  but `.tables` is a pure command, not SQL query. It does not break the main rule: dependencies point down.
- For testing Schema class, create pager using real database - it is ok, but more complex than using ByteBuffer.
- The cli layer only create a Pager object and pass it to the next layer (schema) even if the schema needs only first
  single page. The cli does not know about what page to read (single responsibility). The sqlite_master schema can have
  interior type (in case it hold huge amount of tables that can not fit in single page) - cli does not know about it and
  can not work with it. It is responsibility of following layers.
- The cli layer is simple: create Pager from input file, get list of schema objects, then filter and print them. It
  shows only entries with `type=table`, hides entries that name starts with `sqlite_%`. Also, it sorts entries by name.
- The schema layer gets object of the whole database (Pager class), get first page, convert to BTreePage object, iterate
  over cell points. It creates record Cell for each byte buffer, next it creates the record `Record` from byte buffer
  for each cell. Convert Record to SchemaEntry. Note: use own view of byte buffer, for cell, instead of share the common
  byte buffer - avoid changing the state for each cell processing.
- Add method to BTreePage for creating LeafTableCell. There are four types of cells: interior table, interior index,
  leaf table, and leaf index. Currently, app works with leaf table only. Cell stores only row_id and byte buffer
  for payload.
- The Record objects stores list of value. The name and order of the column names are hardcoded in the code - they are
  the same and fixed for all `sqlite_master` tables. For other tables, the app should use the `CREATE TABLE` definition
  for clarifying name and order for values.
- To create the record `Record` it needs text encoding info; it uses during converting bytes to text value. The schema
  layer should read and provide text encoding. This is addition reason for providing Pager to schema layer instead of
  pure byte buffer.
- The page layer does not work (import and return) Record objects, because page is lower than record. The page returns
  raw bytes and schema info; the schema layer creates `Record` based on bytes from page. It supports and does not break
  the main dependency flow: from up to down.
- Provide Database header to the BTree page object. The method getCell by index have to use pageSize and reserved bytes
  from database header to detect overflow pages.
- Move Varint record from record package to page. The class Btree page uses Varint to read payload size and row id. To
  avoid breaking dependency rule - moved record to lower layer.
- The sqlite_master table has the following table definition:

```sqlite
CREATE TABLE sqlite_master
(
    type     text,
    name     text,
    tbl_name text,
    rootpage integer,
    sql      text
);
```

The column `sql` can have null for `sqlite_autoindex_*` entries; column `rootpage` can be 0 for views and triggers. The
SchemaEntry type for `sql` columns is nullable `String`; it has public method `Optional<String> getSqlOptional()`. The
schema for all column in schema entries:
`SchemaEntry(SchemaEntryType type, String name, String tblName, long rootPage, String sql)`. The enum SchemaEntryType(
table, index, view, trigger)) defines types. Add test for specific case:
`table with a UNIQUE column creates an autoindex row`

## Alternatives considered

- The cli layer can use only query layer. Create a proxy method in query layer for working with schema.
- The cli layer get first page and propagate the byte buffer to the schema. The sqlite_master table can be interior (
  split over several pages), in this case cli have to know how to work - it is a lot of responsibilities for cli layer.
- The schema layer filters and prints schema objects. Hard to test method that returns None and prints results to
  stdout. It is better return objects to upper layer.
- For testing schema layer use separate method, that accept BTreePage and TextEncoding - using separate method ONLY for
  testing is a bad practice.
- Return list of list of values to the cli layer along with serial type. It complicates logic of cli: it has to match
  schema with values.
- Dont separate cell and record and store all data from cell in the cell object. There are four types of cell, so they
  are different - we can not use single cell object for representing record data for leaf and interior pages. Also,
  this approach break main dependency flow (from ADR 0001): layer can not import and uses objects from the upper layer.
  Record layer goes BEFORE the page, so the page (and cell inside) layer can not use record.
- Move record Varint from record package to common. It can lead that common package will grow a lot; also Varint uses
  only in low level layers: page and record - no sense to move to common.
- For detecting overflow page (page which row does not fit into single page, so the size of row greater than page size),
  there are two alternatives ways: provide needed database header info directly to the method or return as it is and
  client (schema layer) should detect overflow page or not. The cons of those approaches that database header info is
  used by another functionality in the BTreePage object - so it will be duplication. Also, the creating the cell is
  responsibility of the page layer, not clients of the page layer.

## Consequences

- (-) No handling the overflow pages (single row can be too large to fit in single page). The app throws
  UnsupportedOperationException. It will be fixed in the future tasks.
- (-) The app works with leaf type for sqlite_master page. For interior page it throws UnsupportedOperationException. It
  will be fixed in the future.
- (+) Keep cli layer as simple as possible: read input arguments, clarify command to run, call classes from the lower
  layers, print result.
- (-) For testing the schema layer - need to create a Pager object instead of byte buffer - it make unit testing harder.
  Run integration tests with real .db file is acceptable in this case.
- (+) Use slice of byte buffer for cell processing - minimize cases when the cell processing changes the state of the
  page byte buffer.
- (-) For working with `sqlite_master` schema, the app uses hardcoded name and order. For the other tables, the order
  and names should come from the `CREATE TABLE` definition, not hardcoded in the code.
