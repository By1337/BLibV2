package dev.by1337.core.util.nbt;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinaryNbt {
    public static final byte TAG_END = 0;
    public static final byte TAG_BYTE = 1;
    public static final byte TAG_SHORT = 2;
    public static final byte TAG_INT = 3;
    public static final byte TAG_LONG = 4;
    public static final byte TAG_FLOAT = 5;
    public static final byte TAG_DOUBLE = 6;
    public static final byte TAG_BYTE_ARRAY = 7;
    public static final byte TAG_STRING = 8;
    public static final byte TAG_LIST = 9;
    public static final byte TAG_COMPOUND = 10;
    public static final byte TAG_INT_ARRAY = 11;
    public static final byte TAG_LONG_ARRAY = 12;
    public static final int MAX_DEPTH = 512;


    public static void writeUnnamedTag(ByteBuf buf, NbtTag tag) throws EncoderException {
        byte type = tag.getId();
        buf.writeByte(type);
        if (type != 0) {
            buf.writeShort(0);
            tag.write(buf);
        }
    }

    public static void writeUnnamedTag(DataOutput buf, NbtTag tag) throws IOException {
        byte type = tag.getId();
        buf.writeByte(type);
        if (type != 0) {
            buf.writeShort(0);
            tag.write(buf);
        }
    }

    public static NbtTag readUnnamedTag(DataInput buf, int depth) throws IOException {
        byte b = buf.readByte();
        if (b != 0) {
            buf.readShort();
        }
        return readByType(buf, b, depth);
    }

    public static NbtTag readUnnamedTag(ByteBuf buf, int depth) throws IOException {
        byte b = buf.readByte();
        if (b != 0) {
            buf.readShort();
        }
        return readByType(buf, b, depth);
    }

    public static NbtTag readByType(ByteBuf buf, byte type, int depth) {
        return switch (type) {
            case TAG_END -> EndTag.INSTANCE;
            case TAG_BYTE -> ByteTag.read(buf);
            case TAG_SHORT -> ShortTag.read(buf);
            case TAG_INT -> IntTag.read(buf);
            case TAG_LONG -> LongTag.read(buf);
            case TAG_FLOAT -> FloatTag.read(buf);
            case TAG_DOUBLE -> DoubleTag.read(buf);
            case TAG_BYTE_ARRAY -> ByteArrayTag.read(buf);
            case TAG_STRING -> StringTag.read(buf);
            case TAG_INT_ARRAY -> IntArrayTag.read(buf);
            case TAG_LONG_ARRAY -> LongArrayTag.read(buf);
            case TAG_LIST -> ListTag.read(buf, depth);
            case TAG_COMPOUND -> CompoundTag.read(buf, depth);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public static NbtTag readByType(DataInput buf, byte type, int depth) throws IOException {
        return switch (type) {
            case TAG_END -> EndTag.INSTANCE;
            case TAG_BYTE -> ByteTag.read(buf);
            case TAG_SHORT -> ShortTag.read(buf);
            case TAG_INT -> IntTag.read(buf);
            case TAG_LONG -> LongTag.read(buf);
            case TAG_FLOAT -> FloatTag.read(buf);
            case TAG_DOUBLE -> DoubleTag.read(buf);
            case TAG_BYTE_ARRAY -> ByteArrayTag.read(buf);
            case TAG_STRING -> StringTag.read(buf);
            case TAG_INT_ARRAY -> IntArrayTag.read(buf);
            case TAG_LONG_ARRAY -> LongArrayTag.read(buf);
            case TAG_LIST -> ListTag.read(buf, depth);
            case TAG_COMPOUND -> CompoundTag.read(buf, depth);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public interface NbtTag {
        byte getId();

        void write(DataOutput output) throws IOException;

        void write(ByteBuf buf) throws EncoderException;
    }

    public static final class CompoundTag implements NbtTag {
        private final Map<String, NbtTag> tags = new HashMap<>();

        public CompoundTag() {
        }

        public int size() {
            return tags.size();
        }

        public boolean isEmpty() {
            return tags.isEmpty();
        }

        public NbtTag get(String key) {
            return tags.get(key);
        }

        public void put(String key, NbtTag value) {
            tags.put(key, value);
        }

        public void remove(String key) {
            tags.remove(key);
        }

        public boolean contains(String key) {
            return tags.containsKey(key);
        }

        public Map<String, NbtTag> tags() {
            return tags;
        }

        @Override
        public byte getId() {
            return TAG_COMPOUND;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            for (Map.Entry<String, NbtTag> entry : tags.entrySet()) {
                NbtTag tag = entry.getValue();
                output.writeByte(tag.getId());
                output.writeUTF(entry.getKey());
                tag.write(output);
            }
            output.writeByte(0);
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            for (Map.Entry<String, NbtTag> entry : tags.entrySet()) {
                NbtTag tag = entry.getValue();
                buf.writeByte(tag.getId());
                DataIOLike.writeUTF(buf, entry.getKey());
                tag.write(buf);
            }
            buf.writeByte(0);
        }

        public static CompoundTag read(ByteBuf buf, int depth) throws EncoderException {
            depth--;
            if (depth < 0) throw new IllegalStateException("Nbt depth exceeded: " + depth);
            CompoundTag res = new CompoundTag();
            byte type;
            while ((type = buf.readByte()) != 0) {
                String key = DataIOLike.readUTF(buf);
                res.put(key, readByType(buf, type, depth));
            }
            return res;
        }

        public static CompoundTag read(DataInput buf, int depth) throws IOException {
            depth--;
            if (depth < 0) throw new IllegalStateException("Nbt depth exceeded: " + depth);
            CompoundTag res = new CompoundTag();
            byte type;
            while ((type = buf.readByte()) != 0) {
                String key = buf.readUTF();
                res.put(key, readByType(buf, type, depth));
            }
            return res;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            for (Map.Entry<String, NbtTag> entry : tags.entrySet()) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                sb.append("\"").append(StringEscaper.escape(entry.getKey())).append("\"")
                        .append(':').append(entry.getValue());
            }
            return sb.append('}').toString();
        }
    }

    public static final class ListTag implements NbtTag {
        private final List<NbtTag> tags;
        private byte elementType = 0;

        public ListTag(List<NbtTag> tags) {
            this.tags = tags;
        }

        public ListTag() {
            tags = new ArrayList<>();
        }

        public void add(NbtTag tag) {
            if (elementType == 0) {
                elementType = tag.getId();
            } else if (elementType != tag.getId()) {
                elementType = 10;
            }
            tags.add(tag);
        }

        public List<NbtTag> tags() {
            return tags;
        }

        @Override
        public byte getId() {
            return TAG_LIST;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeByte(elementType);
            output.writeInt(tags.size());
            if (elementType != 10) {
                for (NbtTag tag : tags) {
                    tag.write(output);
                }
            } else {
                for (NbtTag tag : tags) {
                    if (tag instanceof CompoundTag c) {
                        if (!isWrapper(c)) {
                            c.write(output);
                            continue;
                        }
                    }
                    //wrap
                    output.writeByte(tag.getId());
                    output.writeShort(0); //empty name
                    tag.write(output);
                    output.writeByte(0);
                }
            }
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeByte(elementType);
            buf.writeInt(tags.size());
            if (elementType != TAG_COMPOUND) {
                for (NbtTag tag : tags) {
                    tag.write(buf);
                }
            } else {
                for (NbtTag tag : tags) {
                    if (tag instanceof CompoundTag c) {
                        if (!isWrapper(c)) {
                            c.write(buf);
                            continue;
                        }
                    }
                    //wrap
                    buf.writeByte(tag.getId());
                    buf.writeShort(0); //empty name
                    tag.write(buf);
                    buf.writeByte(0);
                }
            }
        }

        private static boolean isWrapper(CompoundTag tag) {
            return tag.size() == 1 && tag.contains("");
        }

        public static ListTag read(DataInput buf, int depth) throws IOException {
            depth--;
            if (depth < 0) throw new IllegalStateException("Nbt depth exceeded: " + depth);
            byte type = buf.readByte();
            int size = buf.readInt();
            if (type == 0 && size > 0) {
                throw new IOException("Missing type on ListTag");
            }
            ListTag listTag = new ListTag(new ArrayList<>(size));
            for (int i = 0; i < size; i++) {
                NbtTag tag = readByType(buf, type, depth);
                if (tag instanceof CompoundTag c && isWrapper(c)) {
                    listTag.add(c.get(""));

                } else {
                    listTag.add(tag);
                }
            }
            return listTag;
        }

        public static ListTag read(ByteBuf buf, int depth) throws EncoderException {
            depth--;
            if (depth < 0) throw new IllegalStateException("Nbt depth exceeded: " + depth);
            byte type = buf.readByte();
            int size = buf.readInt();
            if (type == 0 && size > 0) {
                throw new EncoderException("Missing type on ListTag");
            }
            ListTag listTag = new ListTag(new ArrayList<>(size));
            for (int i = 0; i < size; i++) {
                NbtTag tag = readByType(buf, type, depth);
                if (tag instanceof CompoundTag c && isWrapper(c)) {
                    listTag.add(c.get(""));
                } else {
                    listTag.add(tag);
                }
            }
            return listTag;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            for (NbtTag tag : tags) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                sb.append(tag);
            }
            return sb.append(']').toString();
        }
    }

    public static final class EndTag implements NbtTag {
        public static EndTag INSTANCE = new EndTag();

        @Override
        public byte getId() {
            return TAG_END;
        }

        @Override
        public void write(DataOutput output) throws IOException {
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
        }
    }

    public record StringTag(String value) implements NbtTag {

        public static StringTag valueOf(String data) {
            return new StringTag(data);
        }

        @Override
        public byte getId() {
            return TAG_STRING;
        }

        @Override

        public void write(DataOutput output) throws IOException {
            output.writeUTF(this.value);
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            DataIOLike.writeUTF(buf, this.value);
        }

        public static StringTag read(ByteBuf buf) throws EncoderException {
            return new StringTag(DataIOLike.readUTF(buf));
        }

        public static StringTag read(DataInput input) throws IOException {
            return new StringTag(input.readUTF());
        }

        @Override
        public @NotNull String toString() {
            return "\"" + StringEscaper.escape(value) + "\"";
        }
    }

    public record ShortTag(short value) implements NbtTag {
        static final ShortTag[] cache = new ShortTag[1153];

        public static ShortTag valueOf(short data) {
            return data >= -128 && data <= 1024 ? cache[data - -128] : new ShortTag(data);
        }

        @Override
        public byte getId() {
            return TAG_SHORT;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeShort(this.value);
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeShort(this.value);
        }

        public static ShortTag read(ByteBuf buf) throws EncoderException {
            return valueOf(buf.readShort());
        }

        public static ShortTag read(DataInput input) throws IOException {
            return valueOf(input.readShort());
        }

        @Override
        public @NotNull String toString() {
            return value + "s";
        }

        static {
            for (int i = 0; i < cache.length; ++i) {
                cache[i] = new ShortTag((short) (-128 + i));
            }
        }
    }

    public record LongTag(long value) implements NbtTag {
        static final LongTag[] cache = new LongTag[1153];

        public static LongTag valueOf(long data) {
            return data >= -128L && data <= 1024L ? cache[(int) data - -128] : new LongTag(data);
        }

        @Override
        public byte getId() {
            return TAG_LONG;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeLong(this.value);
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeLong(this.value);
        }

        public static LongTag read(ByteBuf buf) throws EncoderException {
            return valueOf(buf.readLong());
        }

        public static LongTag read(DataInput input) throws IOException {
            return valueOf(input.readLong());
        }

        @Override
        public @NotNull String toString() {
            return value + "L";
        }

        static {
            for (int i = 0; i < cache.length; ++i) {
                cache[i] = new LongTag((long) (-128 + i));
            }
        }
    }

    public record LongArrayTag(long[] value) implements NbtTag {

        public static LongArrayTag valueOf(long[] data) {
            return new LongArrayTag(data);
        }

        @Override
        public byte getId() {
            return TAG_LONG_ARRAY;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeInt(value.length);
            for (long l : value) {
                output.writeLong(l);
            }
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeInt(value.length);
            for (long l : value) {
                buf.writeLong(l);
            }
        }

        public static LongArrayTag read(ByteBuf buf) throws EncoderException {
            int length = buf.readInt();
            long[] array = new long[length];
            for (int i = 0; i < length; i++) {
                array[i] = buf.readLong();
            }
            return new LongArrayTag(array);
        }

        public static LongArrayTag read(DataInput input) throws IOException {
            int length = input.readInt();
            long[] array = new long[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readLong();
            }
            return new LongArrayTag(array);
        }

        @Override
        public @NotNull String toString() {
            StringBuilder sb = new StringBuilder("[");
            sb.append("L;");
            for (long l : value) {
                if (sb.length() > 3) {
                    sb.append(',');
                }
                sb.append(l);
            }
            return sb.append(']').toString();
        }
    }

    public record IntTag(int value) implements NbtTag {
        static final IntTag[] cache = new IntTag[1153];

        public static IntTag valueOf(int data) {
            return data >= -128 && data <= 1024 ? cache[data - -128] : new IntTag(data);
        }

        @Override
        public byte getId() {
            return TAG_INT;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeInt(this.value);
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeInt(this.value);
        }

        public static IntTag read(ByteBuf buf) throws EncoderException {
            return valueOf(buf.readInt());
        }

        public static IntTag read(DataInput input) throws IOException {
            return valueOf(input.readInt());
        }

        @Override
        public @NotNull String toString() {
            return value + "";
        }

        static {
            for (int i = 0; i < cache.length; ++i) {
                cache[i] = new IntTag(-128 + i);
            }
        }
    }

    public record IntArrayTag(int[] value) implements NbtTag {

        public static IntArrayTag valueOf(int[] data) {
            return new IntArrayTag(data);
        }

        @Override
        public byte getId() {
            return TAG_INT_ARRAY;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeInt(value.length);
            for (int l : value) {
                output.writeInt(l);
            }
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeInt(value.length);
            for (int l : value) {
                buf.writeInt(l);
            }
        }

        public static IntArrayTag read(ByteBuf buf) throws EncoderException {
            int length = buf.readInt();
            int[] array = new int[length];
            for (int i = 0; i < length; i++) {
                array[i] = buf.readInt();
            }
            return new IntArrayTag(array);
        }

        public static IntArrayTag read(DataInput input) throws IOException {
            int length = input.readInt();
            int[] array = new int[length];
            for (int i = 0; i < length; i++) {
                array[i] = input.readInt();
            }
            return new IntArrayTag(array);
        }

        @Override
        public @NotNull String toString() {
            StringBuilder sb = new StringBuilder("[");
            sb.append("I;");
            for (long l : value) {
                if (sb.length() > 3) {
                    sb.append(',');
                }
                sb.append(l);
            }
            return sb.append(']').toString();
        }
    }

    public record FloatTag(float value) implements NbtTag {
        private static final FloatTag ZERO = new FloatTag(0);

        public static FloatTag valueOf(float data) {
            return data == 0.0F ? ZERO : new FloatTag(data);
        }

        @Override
        public byte getId() {
            return TAG_FLOAT;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeFloat(this.value);
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeFloat(this.value);
        }

        public static FloatTag read(ByteBuf buf) throws EncoderException {
            return new FloatTag(buf.readFloat());
        }

        public static FloatTag read(DataInput input) throws IOException {
            return new FloatTag(input.readFloat());
        }

        @Override
        public @NotNull String toString() {
            return value + "f";
        }
    }

    public record DoubleTag(double value) implements NbtTag {
        private static final DoubleTag ZERO = new DoubleTag(0);

        public static DoubleTag valueOf(double data) {
            return data == 0.0F ? ZERO : new DoubleTag(data);
        }

        @Override
        public byte getId() {
            return TAG_DOUBLE;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeDouble(this.value);
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeDouble(this.value);
        }

        public static DoubleTag read(ByteBuf buf) throws EncoderException {
            return new DoubleTag(buf.readDouble());
        }

        public static DoubleTag read(DataInput input) throws IOException {
            return new DoubleTag(input.readDouble());
        }

        @Override
        public @NotNull String toString() {
            return value + "d";
        }
    }

    public record ByteTag(byte value) implements NbtTag {
        static final ByteTag[] cache = new ByteTag[256];

        public static ByteTag valueOf(byte data) {
            return cache[128 + data];
        }

        @Override
        public byte getId() {
            return TAG_BYTE;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeByte(this.value);
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeByte(this.value);
        }

        public static ByteTag read(ByteBuf buf) throws EncoderException {
            return valueOf(buf.readByte());
        }

        public static ByteTag read(DataInput input) throws IOException {
            return valueOf(input.readByte());
        }

        @Override
        public @NotNull String toString() {
            return value + "b";
        }

        static {
            for (int i = 0; i < cache.length; ++i) {
                cache[i] = new ByteTag((byte) (i - 128));
            }
        }
    }

    public record ByteArrayTag(byte[] value) implements NbtTag {

        public static ByteArrayTag valueOf(byte[] data) {
            return new ByteArrayTag(data);
        }

        @Override
        public byte getId() {
            return TAG_BYTE_ARRAY;
        }

        @Override
        public void write(DataOutput output) throws IOException {
            output.writeInt(value.length);
            output.write(value);
        }

        @Override
        public void write(ByteBuf buf) throws EncoderException {
            buf.writeInt(value.length);
            buf.writeBytes(value);
        }

        public static ByteArrayTag read(ByteBuf buf) throws EncoderException {
            int length = buf.readInt();
            byte[] array = new byte[length];
            buf.readBytes(array);
            return new ByteArrayTag(array);
        }

        public static ByteArrayTag read(DataInput input) throws IOException {
            int length = input.readInt();
            byte[] array = new byte[length];
            input.readFully(array);
            return new ByteArrayTag(array);
        }

        @Override
        public @NotNull String toString() {
            StringBuilder sb = new StringBuilder("[");
            sb.append("B;");
            for (long l : value) {
                if (sb.length() > 3) {
                    sb.append(',');
                }
                sb.append(l);
            }
            return sb.append(']').toString();
        }
    }


    private static class DataIOLike {
        public static void writeUTF(ByteBuf buf, String s) throws EncoderException {
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

            buf.writeShort(utfLen); // 2 байта длина

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

        public static String readUTF(ByteBuf buf) throws EncoderException {
            int utfLen = buf.readUnsignedShort();
            byte[] byteArr = new byte[utfLen];
            buf.readBytes(byteArr);

            char[] charArr = new char[utfLen];
            int c, char2, char3;
            int count = 0;
            int charCount = 0;

            while (count < utfLen) {
                c = byteArr[count] & 0xFF;
                if (c > 127) break;
                count++;
                charArr[charCount++] = (char) c;
            }

            while (count < utfLen) {
                c = byteArr[count] & 0xFF;
                switch (c >> 4) {
                    case 0x0:
                    case 0x1:
                    case 0x2:
                    case 0x3:
                    case 0x4:
                    case 0x5:
                    case 0x6:
                    case 0x7:
                        count++;
                        charArr[charCount++] = (char) c;
                        break;
                    case 0xC:
                    case 0xD:
                        count += 2;
                        if (count > utfLen)
                            throw new EncoderException("Malformed input: partial character at end");
                        char2 = byteArr[count - 1];
                        charArr[charCount++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
                        break;
                    case 0xE:
                        count += 3;
                        if (count > utfLen)
                            throw new EncoderException("Malformed input: partial character at end");
                        char2 = byteArr[count - 2];
                        char3 = byteArr[count - 1];
                        charArr[charCount++] = (char) (((c & 0x0F) << 12) |
                                ((char2 & 0x3F) << 6) |
                                (char3 & 0x3F));
                        break;
                    default:
                        throw new EncoderException("Malformed input around byte " + count);
                }
            }

            return new String(charArr, 0, charCount);
        }
    }

    private static class StringEscaper{
        public static String escape(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"':
                        sb.append("\\\"");
                        break;
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
