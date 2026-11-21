# ADR-0009 · Cursor

- **Status:** Proposed
- **Date:** 2026-11-21

## Context

The data are stored in cell and record. There is a trade-off: store all rows into memory or read row when client
request. The table can store thousand of rows using interior pages (table spread rows over several pages). Using lazy
approach is more reasonable and memory efficiency approach. In this context, the DoD when app works well with single
page table.

## Decision

- Row lives in the btree package, because rowid is the key of the table b-tree, stored in the cell. Row combines it with
  the record's values.
- The Cursor is a Iterator, lives in the btree package too.
- BtreePage is not iterable, because in this case lower layer (BtreePage) have to import classes from the higher layer (
  btree) - it is broken the initial agreement.
- Schema use Cursor instead of own implementation. It dont force developer to supporting two different approaches for
  the same operation (getting rows from table).
- Cursor (as other object) can work only with Leaf table types. The app throws Exception when client reads rows from non
  Leaf table. It will be fixed in SJ-10.
- The cursor iterator uses lazy loading, it saves memory.
- The Row object does not store column name.
- The Cursor is build from Pager, number of root page and database header. It allows to use non-leaf pages in the
  future.

## Alternatives considered

- BTreePage is an Iterable object, that use Iterator Cursor. This break the dependency agreement.
- Two different approaches for reading rows from tables: sqlite_master and other tables.
- Use eager loading in Cursor: read all rows into memory.
- Store column along with values for each value in the Row object.
- Use BTreePage object as an input to the Cursor and change in SJ-10.

## Consequences

- (+) Use same approach for reading rows from any table simplify supporting that approach.
- (+) Using lazy loading in Cursor keep memory utilization low.
- (+) The row does not store same repetitive info about column name.It makes column smaller and utilize low
  memory.
- (+) The using Pager as input param to the Cursor prepare Cursor for working with non leaf table in the future (SJ-10)
- (-) The cursor is single pass. New scan needs to initialize new cursor object.
- (-) Current implementation works only with Leaf table.
- (-) Cursor is not thread safe.
