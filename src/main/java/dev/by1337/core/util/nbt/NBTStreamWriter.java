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

    default void namedByte(String key, byte b) {
        pushKey(key);
        pushByte(b);
    }

    default void namedByte(String key, int b) {
        pushKey(key);
        pushByte(b);
    }

    default void pushByte(int v) {
        pushByte((byte) v);
    }

    void pushShort(short v);

    default void namedShort(String key, short b) {
        pushKey(key);
        pushShort(b);
    }

    void pushInt(int v);

    default void namedInt(String key, int b) {
        pushKey(key);
        pushInt(b);
    }

    void pushLong(long v);

    default void namedLong(String key, long b) {
        pushKey(key);
        pushLong(b);
    }

    void pushFloat(float v);

    default void namedFloat(String key, float b) {
        pushKey(key);
        pushFloat(b);
    }

    void pushDouble(double v);

    default void namedDouble(String key, double b) {
        pushKey(key);
        pushDouble(b);
    }

    void pushByteArray(byte[] v);

    default void namedByteArray(String key, byte[] b) {
        pushKey(key);
        pushByteArray(b);
    }

    void pushIntArray(int[] v);

    default void namedIntArray(String key, int[] b) {
        pushKey(key);
        pushIntArray(b);
    }

    void pushLongArray(long[] v);

    default void namedLongArray(String key, long[] b) {
        pushKey(key);
        pushLongArray(b);
    }

    void pushString(String v);

    default void namedString(String key, String b) {
        pushKey(key);
        pushString(b);
    }

    default void pushBool(boolean v) {
        pushByte(v ? 1 : 0);
    }

    default void namedBool(String key, boolean b) {
        pushKey(key);
        pushBool(b);
    }

    void pushList(int type);

    void popList();

    void close();

    void accept(NBTWalker walker);
}
