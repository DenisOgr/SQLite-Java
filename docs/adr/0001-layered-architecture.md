# ADR-0001 · Layered architecture with one-way dependencies

- **Status:** Accepted
- **Date:** 2026-09-29

## Context
A storage engine naturally decomposes into abstraction levels: bytes → pages → records →
b-tree → schema → queries. We need to choose how to organize dependencies between levels so
that each one can be tested in isolation and the code structure matches the task structure.

## Decision
Layered architecture, dependencies point down only:

```
cli → query → schema → btree(cursor) → record → page → io(Pager)
```

An upper layer knows about the lower one, never the other way around. Each layer is a separate
package. `Pager` sits behind an interface (file-based in prod, in-memory in tests).

Cross-layer packages hold classes used by several layers
- common.exceptions - stores exceptions

## Consequences
- (+) Each layer is testable without the layers above it (Pager is mocked with a buffer).
- (+) Layers = ticket boundaries in `BACKLOG.md`. Architecture and backlog are one structure.
- (+) Easy to swap a lower layer's implementation (e.g. an mmap Pager) without touching the top.
- (−) Discipline: no reverse dependencies. If `record` wants to know about `query`, that is a
  signal of a design error, not a reason to add an import.

---

## ADRs you'll write yourself (this is the architecture practice)
- **ADR-0002** · Serial types as a sealed `Value` + records (why not `enum` + `Object`).
- **ADR-0003** · Cursor as `Iterator<Row>` (a deliberate bridge to the Volcano model the course teaches).
- **ADR-0004** · Pager behind an interface (where exactly the file I/O boundary lies).

Same format: Context / Decision / Consequences, 5–15 lines.
