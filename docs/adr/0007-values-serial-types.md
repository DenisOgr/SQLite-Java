# ADR-0007 · Values and serial types

- **Status:** Accepted
- **Date:** 2026-10-19

## Context

To read data from the Cell(Record) the app should know how to convert bytes to some Java objects. For doing this, there
are two "objects": SerialType and Values. The SerialType converts the type code (from Varint as Java long) to meaningful
type with the size. The size is needed to know number of bytes to reads for decoding value for that type.The Value is a
application type (separate record) with Java object inside (kind of wrapper for Java object, wrapper with same
interface); it knows how to convert bytes to Java objects.

## Decision

- For represent value use sealed interface (Value) and inner implementation(Value.Null, Value.Int, Value.Double,
  Value.Text, Value.Blob).
- For representing SerialType use single record + enum. Record stores info about kind (enum) and size (Java int not
  long). The size of serial type IS NOT hardcoded to the enum. It is calculated on the record level, not Enum level.
- The workflow for converting 3 and 6 bytes to signed long uses following steps: assembly bytes with mask 0xFF, left
  shift and right shift. This allows to propagate bit with sign info from 23/47-th position to the 63-th position.
- Convert long to int in the SerialType.fromCode. The one int can store more than 1 billion bytes size Str or
  ByteBuffer. It satisfies default limit in config ` SQLITE_MAX_LENGTH = 1,000,000,000.`.
- The Value.Blob record uses array of byte as internal value, not ByteBuffer. Is it safer than using slice of byte
  buffer. The client code can not mutate origin page/header byte buffer. Due to using byte[], we have to override
  equals, hashCode and toString for the Value.Blob.
- For invalid type codes in Serial type (10,11,ANY<0 and ANY>Integer.MAX_VALUE) throws CorruptDatabaseException.
- The parse method for Values is in the interface. In accepts byte buffer, serial type and database encoding. It returns
  Value and should shift buffer position to the start position of the next value.
- The app does not throw exception when read textual data from file with malformed chars.

## Alternatives considered

- Don't wrap Values in the interface and add suffix to the values, for example DoubleCustom or LongCustom
- For SerialType, the first 10 types has a static size, they can be hardcoded into ENUM, for Text and Blob use -1 (mock
  fake value).
- Use static function, instead of record + enum for Serial type: 2 functions accept Varint(Java long) and return Enum
  value or size. No new object to create (record), but duplicates code in the two function; size and type live
  separately, it is easily to change one and dont change another.
- Use not sealed interface for the Values (and dont group them in the interface). Can not check all existing
  implementations of the Values, force to use default.
- For Values, use single record + enum. The value is an Object. Can not check errors during compile time, force to
  safely check type for object in Value.

## Consequences

- (+) All possible Values are in the same place (inside Interface), it allows to cover all cases in switch.
- (+) The text values are decoded with correct encoding (from database settings), not hardcoded utf-8
- (+) Read 3 and 6 bytes from ByteBuffer to Java long with saving sign.
- (+) Don't hardcoded size for SerialType - avoid confusing with "-1" in Text.size()/Blob.size() for Text and Blob (
  dynamic size)
- (-) One allocation per column per row (a record per value) on the scan path.
- (-) The package record now depends on page.TextEncoding. ADR-0001 allows this.
- (-) The text/blob size limit of about 1 GB.
- (-) The query with malformed char does not work properly: if the textual value meet the malformed char it silently
  replaces for `U+FFFD`. 
