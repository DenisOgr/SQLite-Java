# ADR-0005 · Database header

- **Status:** Proposed
- **Date:** 2026-10-14

## Context
Parse a database header.  The header has many fields and we need a few. 
The Pager already reads the page size.

## Decision
- The page size is parsed in two places (duplicates): in the FilePager class and in the DatabaseHeader class. It is 
acceptable for now.
- The DatabaseHeader object does not have all possible fields, it has only 5: magic, page size, text encoding, page count 
and reserved space for page. If new fields are needed in the future, add new field to record 
and add parsing for bytes with specific offset.
- Create a separate CorruptDatabaseException instead of built-in and less informative exception. 
Because it will be used in several places (separate layers) added it to the common package.
- The DatabaseHeader builds object from the ByteBuffer, not Pager object. In this case, it creates weak relationship 
between two classes (low coupling) - helps in testing and extending.

## Alternatives considered
- Parse all fields from database header. 
- Eliminate duplication in parsing page size: move the DatabaseHeader object to io package and use it into Pager
(inject to the factory method).
- Use string type as text encoding field for DatabaseHeader object. 
- Use Pager object to create DatabaseHeader object.

## Consequences
- (+) DatabaseHeader object uses only needed fields: less code to maintenance and tests to cover the DatabaseHeader class.
- (+) Use enum as field type for text encoding - it prevents invalid values.
- (-) Duplicates in code: parsing page size. They both should always return the same page size. There is test to testing it.