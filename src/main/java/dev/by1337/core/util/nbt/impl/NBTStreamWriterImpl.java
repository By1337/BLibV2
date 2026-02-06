package dev.by1337.core.util.nbt.impl;

import dev.by1337.core.util.nbt.NBTStream;
import dev.by1337.core.util.nbt.NBTStreamWriter;
import dev.by1337.core.util.nbt.NBTWalker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public final class NBTStreamWriterImpl extends NBTStream implements NBTStreamWriter {
    public static byte TAG_BYTE = 1;
    public static byte TAG_SHORT = 2;
    public static byte TAG_INT = 3;
    public static byte TAG_LONG = 4;
    public static byte TAG_FLOAT = 5;
    public static byte TAG_DOUBLE = 6;
    public static byte TAG_BYTE_ARRAY = 7;
    public static byte TAG_STRING = 8;
    public static byte TAG_LIST = 9;
    public static byte TAG_COMPOUND = 10;
    public static byte TAG_INT_ARRAY = 11;
    public static byte TAG_LONG_ARRAY = 12;

    private final ByteBuf buf;
    private NBTStreamWriter current;

    // NBTStreamWriter stream = new NBTStreamWriter(buf);
    // stream.pushObject();
    // stream.pushKey("aByte");
    // stream.pushByte(13);
    // stream.pushKey("aList");
    // stream.pushList(TAG_COMPOUND);
    // stream.pushList(TAG_COMPOUND);
    // stream.pushString("test");
    // stream.popList();
    // stream.popList();
    // stream.popObject();
    // stream.close();
    public NBTStreamWriterImpl(ByteBuf buffer) {
        this.buf = buffer;
        current = new CompoundStream(null);
    }

    private void writeUTF(String s){
        writeUTF(buf, s);
    }
    private void writeByte(int b){
        buf.writeByte(b);
    }
    private void writeByte(byte b){
        buf.writeByte(b);
    }
    private void writeBytes(byte[] b){
        buf.writeBytes(b);
    }
    private void writeShort(int v) {
        buf.writeShort(v);
    }
    private void writeShort(short v) {
        buf.writeShort(v);
    }

    private void writeInt(int v) {
        buf.writeInt(v);
    }
    private void writeLong(long v) {
        buf.writeLong(v);
    }
    private void writeFloat(float v) {
        buf.writeFloat(v);
    }
    private void writeDouble(double v) {
        buf.writeDouble(v);
    }

    private final class CompoundStream implements NBTStreamWriter {
        private final @Nullable NBTStreamWriter perv;
        private @NotNull String key = "";

        private CompoundStream(@Nullable NBTStreamWriter perv) {
            this.perv = perv;
        }

        @Override
        public void accept(NBTWalker walker) {
            NBTStreamWriterImpl.this.accept(walker);
        }

        @Override
        public void pushKey(String key) {
            this.key = key;
        }

        @Override
        public void pushObject() {
            writeByte(TAG_COMPOUND);
            writeUTF(key);
            current = new CompoundStream(this);
        }

        @Override
        public void popObject() {
            current = perv;
            writeByte(0);
        }

        @Override
        public void pushByte(byte v) {
            writeByte(TAG_BYTE);
            writeUTF(key);
            writeByte(v);
        }

        @Override
        public void pushShort(short v) {
            writeByte(TAG_SHORT);
            writeUTF(key);
            writeShort(v);
        }

        @Override
        public void pushInt(int v) {
            writeByte(TAG_INT);
            writeUTF(key);
            writeInt(v);
        }

        @Override
        public void pushLong(long v) {
            writeByte(TAG_LONG);
            writeUTF(key);
            writeLong(v);
        }

        @Override
        public void pushFloat(float v) {
            writeByte(TAG_FLOAT);
            writeUTF(key);
            writeFloat(v);
        }

        @Override
        public void pushDouble(double v) {
            writeByte(TAG_DOUBLE);
            writeUTF(key);
            writeDouble(v);
        }

        @Override
        public void pushByteArray(byte[] v) {
            writeByte(TAG_BYTE_ARRAY);
            writeUTF(key);
            writeInt(v.length);
            writeBytes(v);
        }

        @Override
        public void pushIntArray(int[] v) {
            writeByte(TAG_INT_ARRAY);
            writeUTF(key);
            writeInt(v.length);
            for (int i : v) {
                writeInt(i);
            }
        }

        @Override
        public void pushLongArray(long[] v) {
            writeByte(TAG_LONG_ARRAY);
            writeUTF(key);
            writeInt(v.length);
            for (var i : v) {
                writeLong(i);
            }
        }

        @Override
        public void pushString(String v) {
            writeByte(TAG_STRING);
            writeUTF(key);
            writeUTF(v);
        }

        @Override
        public void pushList(int type) {
            writeByte(TAG_LIST);
            writeUTF(key);
            current = new ListStream(this, type, false);
        }

        @Override
        public void popList() {
            throw new UnsupportedOperationException("Only in list!");
        }
        @Override
        public void close() {
            current = perv;
        }
    }

    private final class ListStream implements NBTStreamWriter {
        private final @Nullable NBTStreamWriter perv;
        private @Nullable String key;
        private int size;
        private int sizePtr;
        private final int elementType;
        private final boolean reqCloseCompound;

        private ListStream(@Nullable NBTStreamWriter perv, int elementType, boolean reqCloseCompound) {
            this.perv = perv;
            this.elementType = elementType;
            this.reqCloseCompound = reqCloseCompound;
            writeByte(elementType);
            sizePtr = buf.writerIndex();
            buf.writeInt(0);
        }

        @Override
        public void accept(NBTWalker walker) {
            NBTStreamWriterImpl.this.accept(walker);
        }

        @Override
        public void pushKey(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void pushObject() {
            size++;
            current = new CompoundStream(this);
        }

        @Override
        public void popObject() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void pushByte(byte v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_BYTE);
                writeShort(0);
                writeByte(v);
                writeByte(0);
            } else {
                writeByte(v);
            }
        }

        @Override
        public void pushShort(short v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_SHORT);
                writeShort(0);
                writeShort(v);
                writeByte(0);
            } else {
                writeShort(v);
            }
        }

        @Override
        public void pushInt(int v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_INT);
                writeShort(0);
                writeInt(v);
                writeByte(0);
            } else {
                writeInt(v);
            }
        }

        @Override
        public void pushLong(long v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_LONG);
                writeShort(0);
                writeLong(v);
                writeByte(0);
            } else {
                writeLong(v);
            }
        }

        @Override
        public void pushFloat(float v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_FLOAT);
                writeShort(0);
                writeFloat(v);
                writeByte(0);
            } else {
                writeFloat(v);
            }
        }

        @Override
        public void pushDouble(double v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_DOUBLE);
                writeShort(0);
                writeDouble(v);
                writeByte(0);
            } else {
                writeDouble(v);
            }
        }

        @Override
        public void pushByteArray(byte[] v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_BYTE_ARRAY);
                writeShort(0);
                pushByteArray0(v);
                writeByte(0);
            } else {
                pushByteArray0(v);
            }
        }

        public void pushByteArray0(byte[] v) {
            writeInt(v.length);
            writeBytes(v);
        }

        @Override
        public void pushIntArray(int[] v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_INT_ARRAY);
                writeShort(0);
                pushIntArray0(v);
                writeByte(0);
            } else {
                pushIntArray0(v);
            }
        }

        public void pushIntArray0(int[] v) {
            writeInt(v.length);
            for (int i : v) {
                writeInt(i);
            }
        }

        @Override
        public void pushLongArray(long[] v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_LONG_ARRAY);
                writeShort(0);
                pushLongArray0(v);
                writeByte(0);
            } else {
                pushLongArray0(v);
            }
        }

        public void pushLongArray0(long[] v) {
            writeInt(v.length);
            for (var i : v) {
                writeLong(i);
            }
        }

        @Override
        public void pushString(String v) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_STRING);
                writeShort(0);
                writeUTF(v);
                writeByte(0);
            } else {
                writeUTF(v);
            }
        }

        @Override
        public void pushList(int type) {
            size++;
            if (elementType == TAG_COMPOUND) {
                //wrap
                writeByte(TAG_LIST);
                writeShort(0);
                current = new ListStream(this, type, true);
            } else {
                current = new ListStream(this, type, false);
            }
        }

        @Override
        public void popList() {
            buf.setInt(sizePtr, size);
            if (reqCloseCompound) {
                writeByte(0);
            }
            current = perv;
        }

        @Override
        public void close() {
            current = perv;
        }
    }

    public void pushKey(String key) {
        current.pushKey(key);
    }

    public void pushObject() {
        current.pushObject();
    }

    public void pushByte(byte v) {
        current.pushByte(v);
    }

    public void pushByte(int v) {
        current.pushByte(v);
    }

    public void pushShort(short v) {
        current.pushShort(v);
    }

    public void pushInt(int v) {
        current.pushInt(v);
    }

    public void pushLong(long v) {
        current.pushLong(v);
    }

    public void pushFloat(float v) {
        current.pushFloat(v);
    }

    public void pushDouble(double v) {
        current.pushDouble(v);
    }

    public void pushByteArray(byte[] v) {
        current.pushByteArray(v);
    }

    public void pushIntArray(int[] v) {
        current.pushIntArray(v);
    }

    public void pushLongArray(long[] v) {
        current.pushLongArray(v);
    }

    public void pushString(String v) {
        current.pushString(v);
    }

    public void pushList(int type) {
        current.pushList(type);
    }

    public void popList() {
        current.popList();
    }

    public void popObject() {
        current.popObject();
    }

    public void close() {
        current.close();
        if (current != null) {
            throw new IllegalStateException("Не закрыто?");
        }
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
