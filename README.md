# SQLite-Java

A port of the SQLite read path (.db reader) from Rust to Java. A learning project — a Java
refresh before a Database Internals course. Work plan — `BACKLOG.md`, architecture — `docs/adr/`.

## Requirements
- **JDK 21+** (records, sealed types, pattern matching in `switch`). Want a newer LTS —
  install 25 and change `maven.compiler.release` in `pom.xml`.
- **Maven 3.9+**

## Getting started
```bash
mvn compile      # build
mvn test         # tests (currently only SanityTest — must be green)
mvn exec:java    # run the CLI (usage stub for now)
```

## Test .db
```bash
sqlite3 sample-dbs/sample.db \
  "CREATE TABLE t(id INTEGER, name TEXT); INSERT INTO t VALUES (1,'a'),(2,'b');"
```
Use it in Pager tests (SJ-2) and beyond.

## Structure
```
src/main/java/net/porplenko/sqlite/
  cli/         entry point
  io/          Pager                       (SJ-2)
  page/        b-tree page parsing         (SJ-4)
  page/writable/  slotted writable page    (SJ-15)
  record/      varint, serial types, Value (SJ-5, SJ-6)
  schema/      sqlite_master               (SJ-7)
  query/       cursors, SELECT/WHERE       (SJ-8..SJ-14)
docs/adr/      architecture decisions
BACKLOG.md     tickets
CLAUDE.md      mentor mode for Claude Code
```
Packages are a guideline (= layers from ADR-0001). Create them as tickets need them, not upfront.

## Workflow
1. Take the top unblocked ticket from `BACKLOG.md`.
2. Write it yourself. Claude Code is a mentor (explains APIs, reviews, doesn't write for you).
3. Finished a stage → bring it for review.
