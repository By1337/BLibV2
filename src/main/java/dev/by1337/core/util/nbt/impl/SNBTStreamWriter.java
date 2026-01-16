package dev.by1337.core.util.nbt.impl;

import dev.by1337.core.util.nbt.NBTStream;
import dev.by1337.core.util.nbt.NBTStreamWriter;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class SNBTStreamWriter extends NBTStream implements NBTStreamWriter {
    private final Appendable writer;
    private boolean separator;

    public SNBTStreamWriter(Appendable writer) {
        this.writer = writer;
    }

    @Override
    public void pushKey(String key) {
        try {
            if (separator) writer.append(',');
            //Pattern UNQUOTED_KEY_MATCH = Pattern.compile("[A-Za-z._]+[A-Za-z0-9._+-]*");
            // если условие походит то можно не оборачивать
            //!key.equalsIgnoreCase("true") && !key.equalsIgnoreCase("false") && UNQUOTED_KEY_MATCH.matcher(key).matches()
            writer.append("\"");
            appendEscaped(key);
            writer.append("\"").append(':');
            separator = false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushObject() {
        try {
            if (separator) writer.append(',');
            writer.append('{');
            separator = false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void popObject() {
        try {
            writer.append('}');
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushList(int type) {
        try {
            if (separator) writer.append(',');
            writer.append('[');
            separator = false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void popList() {
        try {
            writer.append(']');
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushByte(byte v) {
        try {
            if (separator) writer.append(',');
            writer.append(Byte.toString(v)).append("b");
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushShort(short v) {
        try {
            if (separator) writer.append(',');
            writer.append(Short.toString(v)).append("s");
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushInt(int v) {
        try {
            if (separator) writer.append(',');
            writer.append(Integer.toString(v));
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushLong(long v) {
        try {
            if (separator) writer.append(',');
            writer.append(Long.toString(v)).append("L");
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushFloat(float v) {
        try {
            if (separator) writer.append(',');
            writer.append(Float.toString(v)).append("f");
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushDouble(double v) {
        try {
            if (separator) writer.append(',');
            writer.append(Double.toString(v)).append("d");
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushByteArray(byte[] v) {
        try {
            if (separator) writer.append(',');
            writer.append("[B;");
            for (int i = 0; i < v.length; i++) {
                if (i != 0){
                    writer.append(",");
                }
                writer.append(Byte.toString(v[i])).append("B");
            }
            writer.append(']');
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushIntArray(int[] v) {
        try {
            if (separator) writer.append(',');
            writer.append("[I;");
            for (int i = 0; i < v.length; i++) {
                if (i != 0){
                    writer.append(",");
                }
                writer.append(Integer.toString(v[i]));
            }
            writer.append(']');
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushLongArray(long[] v) {

        try {
            if (separator) writer.append(',');
            writer.append("[L;");
            for (int i = 0; i < v.length; i++) {
                if (i != 0){
                    writer.append(",");
                }
                writer.append(Long.toString(v[i])).append("L");
            }
            writer.append(']');
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void pushString(String v) {
        try {
            if (separator) writer.append(',');
            writer.append("\"");
            appendEscaped(v);
            writer.append("\"");
            separator = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {

    }

    private void appendEscaped(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            appendEscaped(s.charAt(i));
        }
    }

    private void appendEscaped(char c) throws IOException {
        switch (c) {
            case '"':
                writer.append("\\\"");
                break;
            case '\\':
                writer.append("\\\\");
                break;
            case '\n':
                writer.append("\\n");
                break;
            case '\r':
                writer.append("\\r");
                break;
            case '\t':
                writer.append("\\t");
                break;
            default:
                writer.append(c);
        }

    }
}
