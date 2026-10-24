package net.porplenko.sqlite.record;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import net.porplenko.sqlite.page.TextEncoding;
import net.porplenko.sqlite.page.Varint;

public record Record(Value[] values) {
    public static Record parse(ByteBuffer buf, TextEncoding encoding) {
        long headerFinishPosition = buf.position() + Varint.read(buf);
        ArrayList<SerialType> serialTypes = new ArrayList<>();

        while (buf.position() < headerFinishPosition) {
            serialTypes.add(SerialType.of(Varint.read(buf)));
        }

        Value[] values = new Value[serialTypes.size()];
        for (int i = 0; i < serialTypes.size(); i++) {
            values[i] = Value.parse(serialTypes.get(i), buf, encoding);
        }
        return new Record(values);
    }
}
