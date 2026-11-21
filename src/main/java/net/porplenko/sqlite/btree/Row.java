package net.porplenko.sqlite.btree;

import net.porplenko.sqlite.record.Value;

public record Row(long rowId, Value[] values) {
}
