package dev.by1337.core.nbt;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;

import java.util.function.Consumer;

public class NBTWriter {
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

    private final ByteBuf buf;
    private String key;
    private int size;
    private int listType = -1;

    public NBTWriter(ByteBuf buf) {
        this.buf = buf;
    }

    public NBTWriter named(String name) {
        key = name;
        return this;
    }

    public NBTWriter unnamed() {
        key = "";
        return this;
    }

    public NBTWriter noName() {
        key = null;
        return this;
    }

    public void pushObject(Consumer<NBTWriter> consumer) {
        int oldSize = size;
        var oldListType = listType;
        listType = -1;
        size = 0;
        buf.writeByte(TAG_COMPOUND);
        if (key != null) {
            writeUTF(key);
        }
        consumer.accept(this);
        buf.writeByte(0);
        size = oldSize;
        listType = oldListType;
    }

    public void pushList(int innerType, Consumer<NBTWriter> consumer) {
        int oldSize = size;
        var oldListType = listType;
        listType = innerType;
        size = 0;
        buf.writeByte(TAG_LIST);
        if (key != null) {
            writeUTF(key);
        }
        buf.writeByte(innerType);
        int sizePtr = buf.writerIndex();
        buf.writeInt(0);
        consumer.accept(this);
        buf.setInt(sizePtr, size);
        size = oldSize;
        listType = oldListType;
    }

    public void namedByte(String name, byte value) {
        named(name);
        pushByte(value);
    }

    public void unnamedByte(byte value) {
        unnamed();
        pushByte(value);
    }

    public void pushByte(byte v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_BYTE);
            if (key != null) {
                writeUTF(key);
            }
            buf.writeByte(v);
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_BYTE);
            buf.writeShort(0);
            buf.writeByte(v);
            buf.writeByte(0);
        } else {
            buf.writeByte(v);
        }
    }


    public void namedShort(String name, short value) {
        named(name);
        pushShort(value);
    }
    public void unnamedShort(short value) {
        unnamed();
        pushShort(value);
    }
    public void pushShort(short v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_SHORT);
            if (key != null) {
                writeUTF(key);
            }
            buf.writeShort(v);
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_SHORT);
            buf.writeShort(0);
            buf.writeShort(v);
            buf.writeByte(0);
        } else {
            buf.writeShort(v);
        }
    }

    public void namedInt(String name, int value) {
        named(name);
        pushInt(value);
    }
    public void unnamedInt(int value) {
        unnamed();
        pushInt(value);
    }
    public void pushInt(int v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_INT);
            if (key != null) {
                writeUTF(key);
            }
            buf.writeInt(v);
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_INT);
            buf.writeShort(0);
            buf.writeInt(v);
            buf.writeByte(0);
        } else {
            buf.writeInt(v);
        }
    }

    public void namedLong(String name, long value) {
        named(name);
        pushLong(value);
    }
    public void unnamedLong(long value) {
        unnamed();
        pushLong(value);
    }
    public void pushLong(long v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_LONG);
            if (key != null) {
                writeUTF(key);
            }
            buf.writeLong(v);
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_LONG);
            buf.writeShort(0);
            buf.writeLong(v);
            buf.writeByte(0);
        } else {
            buf.writeLong(v);
        }
    }

    public void namedFloat(String name, float value) {
        named(name);
        pushFloat(value);
    }
    public void unnamedFloat(float value) {
        unnamed();
        pushFloat(value);
    }
    public void pushFloat(float v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_FLOAT);
            if (key != null) {
                writeUTF(key);
            }
            buf.writeFloat(v);
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_FLOAT);
            buf.writeShort(0);
            buf.writeFloat(v);
            buf.writeByte(0);
        } else {
            buf.writeFloat(v);
        }
    }

    public void namedDouble(String name, double value) {
        named(name);
        pushDouble(value);
    }
    public void unnamedDouble(double value) {
        unnamed();
        pushDouble(value);
    }
    public void pushDouble(double v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_DOUBLE);
            if (key != null) {
                writeUTF(key);
            }
            buf.writeDouble(v);
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_DOUBLE);
            buf.writeShort(0);
            buf.writeDouble(v);
            buf.writeByte(0);
        } else {
            buf.writeDouble(v);
        }
    }

    public void namedString(String name, String value) {
        named(name);
        pushString(value);
    }
    public void unnamedString(String value) {
        unnamed();
        pushString(value);
    }
    public void pushString(String v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_STRING);
            if (key != null) {
                writeUTF(key);
            }
            writeUTF(v);
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_STRING);
            buf.writeShort(0);
            writeUTF(v);
            buf.writeByte(0);
        } else {
            writeUTF(v);
        }
    }

    public void namedIntArray(String name, int[] value) {
        named(name);
        pushIntArray(value);
    }
    public void unnamedIntArray(int[] value) {
        unnamed();
        pushIntArray(value);
    }
    public void pushIntArray(int[] v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_INT_ARRAY);
            if (key != null) {
                writeUTF(key);
            }
            buf.writeInt(v.length);
            for (int l : v) {
                buf.writeInt(l);
            }
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_INT_ARRAY);
            buf.writeShort(0);
            buf.writeInt(v.length);
            for (int l : v) {
                buf.writeInt(l);
            }
            buf.writeByte(0);
        } else {
            buf.writeInt(v.length);
            for (int l : v) {
                buf.writeInt(l);
            }
        }
    }

    public void namedLongArray(String name, long[] value) {
        named(name);
        pushLongArray(value);
    }
    public void unnamedLongArray(long[] value) {
        unnamed();
        pushLongArray(value);
    }
    public void pushLongArray(long[] v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_LONG_ARRAY);
            if (key != null) {
                writeUTF(key);
            }
            buf.writeInt(v.length);
            for (long l : v) {
                buf.writeLong(l);
            }
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_LONG_ARRAY);
            buf.writeShort(0);
            buf.writeInt(v.length);
            for (long l : v) {
                buf.writeLong(l);
            }
            buf.writeByte(0);
        } else {
            buf.writeInt(v.length);
            for (long l : v) {
                buf.writeLong(l);
            }
        }
    }

    public void namedByteArray(String name, byte[] value) {
        named(name);
        pushByteArray(value);
    }
    public void unnamedByteArray(byte[] value) {
        unnamed();
        pushByteArray(value);
    }
    public void pushByteArray(byte[] v) {
        size++;
        if (listType == -1) {
            buf.writeByte(TAG_BYTE_ARRAY);
            if (key != null) {
                writeUTF(key);
            }
            buf.writeInt(v.length);
            buf.writeBytes(v);
        } else if (listType == TAG_COMPOUND) {
            buf.writeByte(TAG_BYTE_ARRAY);
            buf.writeShort(0);
            buf.writeInt(v.length);
            buf.writeBytes(v);
            buf.writeByte(0);
        } else {
            buf.writeInt(v.length);
            buf.writeBytes(v);
        }
    }


    private void writeUTF(String name) {
        writeUTF(buf, name);
    }

    private static void writeUTF(ByteBuf buf, String s) throws EncoderException {
        if (s.isEmpty()) {
            buf.writeShort(0);
            return;
        }
        int utfLen = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                utfLen += 1;
            } else if (c > 0x07FF) {
                utfLen += 3;
            } else {
                utfLen += 2;
            }
        }

        if (utfLen > 65535) {
            throw new EncoderException("Encoded string too long: " + utfLen + " bytes");
        }

        buf.writeShort(utfLen);

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                buf.writeByte(c);
            } else if (c > 0x07FF) {
                buf.writeByte(0xE0 | ((c >> 12) & 0x0F));
                buf.writeByte(0x80 | ((c >> 6) & 0x3F));
                buf.writeByte(0x80 | (c & 0x3F));
            } else {
                buf.writeByte(0xC0 | ((c >> 6) & 0x1F));
                buf.writeByte(0x80 | (c & 0x3F));
            }
        }
    }
}
