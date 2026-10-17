# ADR-0006 · Page header and array with cell pointers (offsets)

- **Status:** Accepted
- **Date:** 2026-10-15

## Context

Parse a page header and array of cell pointers. The page header has 6 fields; one is optional depends on page type.
Some page information does not need now. Array of cells pointers are 2 bytes integer, but the right most pointer from
page header is 4 byte unsign integer. For storing it in Java, we have to use long; because it points to the page number
we have to handle it with `Pager.readPage()` method.

## Decision

- Create BTreePage class as representation of the bytes for Page. It uses ByteBuffer and page number to build an object.
  It has a following behavior:
    - build and return header of page as separate Java object
    - get array of cell offsets
    - get cell offset by number
- The BTreePageHeader is INNER static class of BTreePage. The header is tightly coupled with page (it is a part of the
  page) - so they are implemented and used together.
- Calculate BTreePageHeader once during creating the BTreePage object. It allow catch Corrupted database exception in
  the early stage (not from random function in the future).
- Calculate cell offset and array of offset on the fly. Dont use separate record object for wrapping array and dont
  pre-calculate array of cell offsets in advance, because the client can mutate it.
- The type of page is enum (4 values only), enum has addition helper functions.
- Due to the value from the page header `right-most-pointer` is unsign 4 bytes - Changed interface for Pager.readPage
  for using long instead of int. Changed client code, FilePager implementation and tests as well.

## Alternatives considered

- Separate BTreePage and BTreePageHeader object: create in separate classes.
- Create record wrapper for the array of cell offsets.
- Pre-calculate array of offset and store into internal attribute of BTreePage. Use copy for reject any mutation of
  source.
- Dont use enum and use integer for page type.
- Dont change interface for Pager.readPage from int to long. Use `Math.toIntExact(long)` instead.

## Consequences

- (+) Parse BTreePageHeader only once in the constructor of class BTreePage. Detect corrupted page header in early
  stage.
- (+) The array of cell pointers can not be mutated or changed from outside. It always shows the correct offsets.
- (-) Change type from int to long in existing code.
- (+) Dont create separate files for BTreePageHeader: it is clearly part of the page.
- (+) Limit possible values for page types: fewer places for errors.
