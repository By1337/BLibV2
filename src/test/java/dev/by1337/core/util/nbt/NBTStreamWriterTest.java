package dev.by1337.core.util.nbt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NBTStreamWriterTest {

    @Test
    public void test() throws IOException {
        ByteBuf buf = Unpooled.buffer();
        var stream = NBTStreamWriter.create(buf);
        stream.pushObject();
        stream.namedString("name", "value");
        stream.namedString("name1", "value");
        stream.namedString("name2", "value");
        stream.popObject();
        stream.close();

        assertEquals("{\"name\":\"value\",\"name2\":\"value\",\"name1\":\"value\"}", BinaryNbt.readUnnamedTag(buf, 512).toString());
        assertEquals(0, buf.readableBytes());
        buf.release();
    }


}