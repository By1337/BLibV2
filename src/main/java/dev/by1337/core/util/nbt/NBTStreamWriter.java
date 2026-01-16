package dev.by1337.core.util.nbt;

import dev.by1337.core.util.nbt.impl.NBTStreamWriterImpl;
import dev.by1337.core.util.nbt.impl.SNBTStreamWriter;
import io.netty.buffer.ByteBuf;

public interface NBTStreamWriter {
    byte TAG_BYTE = 1;
    byte TAG_SHORT = 2;
    byte TAG_INT = 3;
    byte TAG_LONG = 4;
    byte TAG_FLOAT = 5;
    byte TAG_DOUBLE = 6;
    byte TAG_BYTE_ARRAY = 7;
    byte TAG_STRING = 8;
    byte TAG_LIST = 9;
    byte TAG_COMPOUND = 10;
    byte TAG_INT_ARRAY = 11;
    byte TAG_LONG_ARRAY = 12;

    static NBTStreamWriter create(ByteBuf buffer) {
        return new NBTStreamWriterImpl(buffer);
    }

    static NBTStreamWriter createToSnbt(Appendable writer) {
        return new SNBTStreamWriter(writer);
    }

    void pushKey(String key);

    void pushObject();

    void popObject();

    void pushByte(byte v);

    default void pushByte(int v) {
        pushByte((byte) v);
    }

    void pushShort(short v);

    void pushInt(int v);

    void pushLong(long v);

    void pushFloat(float v);

    void pushDouble(double v);

    void pushByteArray(byte[] v);

    void pushIntArray(int[] v);

    void pushLongArray(long[] v);

    void pushString(String v);

    default void pushBool(boolean v) {
        pushByte(v ? 1 : 0);
    }

    void pushList(int type);

    void popList();

    void close();

    void accept(NBTWalker walker);
}
