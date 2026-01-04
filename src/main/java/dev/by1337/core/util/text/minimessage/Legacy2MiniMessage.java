package dev.by1337.core.util.text.minimessage;

public class Legacy2MiniMessage {

    public static String convert(String s) {
        if (!s.contains("&") && !s.contains("§")) return s;
        StringBuilder sb = new StringBuilder();
        Exp exp = new Exp(s);
        while (exp.hasNext()) {
            process(exp, sb);
        }
        return sb.toString();
    }

    private static void process(Exp exp, StringBuilder out) {
        Builder builder = new Builder(out);
        loop:
        while (exp.hasNext()) {
            char c = exp.next();
            if ((c == '&' || c == '§') && exp.hasNext()) {
                char code = exp.next();
                switch (code) {
                    case '0' -> {
                        builder.openTag("<black>");
                        builder.reqClose("</black>");
                        continue loop;
                    }
                    case '1' -> {
                        builder.openTag("<dark_blue>");
                        builder.reqClose("</dark_blue>");
                        continue loop;
                    }
                    case '2' -> {
                        builder.openTag("<dark_green>");
                        builder.reqClose("</dark_green>");
                        continue loop;
                    }
                    case '3' -> {
                        builder.openTag("<dark_aqua>");
                        builder.reqClose("</dark_aqua>");
                        continue loop;
                    }
                    case '4' -> {
                        builder.openTag("<dark_red>");
                        builder.reqClose("</dark_red>");
                        continue loop;
                    }
                    case '5' -> {
                        builder.openTag("<dark_purple>");
                        builder.reqClose("</dark_purple>");
                        continue loop;
                    }
                    case '6' -> {
                        builder.openTag("<gold>");
                        builder.reqClose("</gold>");
                        continue loop;
                    }
                    case '7' -> {
                        builder.openTag("<gray>");
                        builder.reqClose("</gray>");
                        continue loop;
                    }
                    case '8' -> {
                        builder.openTag("<dark_gray>");
                        builder.reqClose("</dark_gray>");
                        continue loop;
                    }
                    case '9' -> {
                        builder.openTag("<blue>");
                        builder.reqClose("</blue>");
                        continue loop;
                    }
                    case 'a' -> {
                        builder.openTag("<green>");
                        builder.reqClose("</green>");
                        continue loop;
                    }
                    case 'b' -> {
                        builder.openTag("<aqua>");
                        builder.reqClose("</aqua>");
                        continue loop;
                    }
                    case 'c' -> {
                        builder.openTag("<red>");
                        builder.reqClose("</red>");
                        continue loop;
                    }
                    case 'd' -> {
                        builder.openTag("<light_purple>");
                        builder.reqClose("</light_purple>");
                        continue loop;
                    }
                    case 'e' -> {
                        builder.openTag("<yellow>");
                        builder.reqClose("</yellow>");
                        continue loop;
                    }
                    case 'f' -> {
                        builder.openTag("<white>");
                        builder.reqClose("</white>");
                        continue loop;
                    }
                    case 'l' -> {
                        builder.softOpenTag("<b>");
                        builder.reqClose("</b>");
                        continue loop;
                    }
                    case 'n' -> {
                        builder.softOpenTag("<u>");
                        builder.reqClose("</u>");
                        continue loop;
                    }
                    case 'm' -> {
                        builder.softOpenTag("<st>");
                        builder.reqClose("</st>");
                        continue loop;
                    }
                    case 'o' -> {
                        builder.softOpenTag("<i>");
                        builder.reqClose("</i>");
                        continue loop;
                    }
                    case 'k' -> {
                        builder.softOpenTag("<obf>");
                        builder.reqClose("</obf>");
                        continue loop;
                    }
                    case 'r' -> {
                        builder.openTag("<reset>");
                        continue loop;
                    }
                    default -> {
                        if (code == '#') {
                            if (exp.ensureCapacity(6)) {
                                int idx = exp.idx - 1;
                                int color = 0;
                                int x;
                                parser:
                                {
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color |= x;
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                }
                                if (x == -1) {
                                    builder.appendText(c);
                                    exp.idx = idx;
                                } else {
                                    builder.openTag("<color:#");
                                    for (int shift = 20; shift >= 0; shift -= 4) {
                                        int nibble = (color >> shift) & 0xF;
                                        builder.buf.append((char) (nibble < 10 ? '0' + nibble : 'a' + (nibble - 10)));
                                    }
                                    builder.buf.append(">");
                                    builder.reqClose("</color>");
                                }
                                continue loop;
                            } else {
                                builder.appendText(c);
                                builder.appendText('#');
                            }
                        } else if (code == 'x') {
                            //&x&0&d&f&b&0&0
                            if (exp.ensureCapacity(12)) {
                                int idx = exp.idx - 1;
                                int color = 0;
                                int x;
                                parser:
                                {
                                    if (exp.next() != c) {
                                        x = -1;
                                        break parser;
                                    }
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color |= x;
                                    if (exp.next() != c) {
                                        x = -1;
                                        break parser;
                                    }
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                    if (exp.next() != c) {
                                        x = -1;
                                        break parser;
                                    }
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                    if (exp.next() != c) {
                                        x = -1;
                                        break parser;
                                    }
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                    if (exp.next() != c) {
                                        x = -1;
                                        break parser;
                                    }
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                    if (exp.next() != c) {
                                        x = -1;
                                        break parser;
                                    }
                                    if ((x = hexToInt(exp.next())) == -1) break parser;
                                    color = (color << 4) | x;
                                }
                                if (x == -1) {
                                    builder.appendText(c);
                                    exp.idx = idx;
                                } else {
                                    builder.openTag("<color:#");
                                    for (int shift = 20; shift >= 0; shift -= 4) {
                                        int nibble = (color >> shift) & 0xF;
                                        builder.buf.append((char) (nibble < 10 ? '0' + nibble : 'a' + (nibble - 10)));
                                    }
                                    builder.buf.append(">");
                                    builder.reqClose("</color>");
                                }
                                continue loop;
                            } else {
                                builder.appendText(c);
                                builder.appendText('x');
                            }
                        }
                    }
                }
            } else {
                builder.appendText(c);
            }
        }
        builder.finish();
    }

    private static class Builder {
        private int pos;
        private boolean hasText;
        private final StringBuilder buf;
        private final CloseBuffer closeable = new CloseBuffer();

        public Builder(StringBuilder buf) {
            this.buf = buf;
        }

        public void softOpenTag(String color) {
            buf.append(color);
        }

        public void openTag(String color) {
            if (!hasText) {
                buf.setLength(pos);
                closeable.clear();
            }
            closeable.flushTo(buf);
            pos = buf.length();
            buf.append(color);
            closeable.clear();
            hasText = false;
        }

        public void reqClose(String s) {
            closeable.insert(s);
        }

        public void appendText(char c) {
            buf.append(c);
            hasText = true;
            pos = buf.length();
        }

        public void finish() {
            if (!hasText) {
                buf.setLength(pos);
                closeable.clear();
            }
            closeable.flushTo(buf);
            hasText = false;
        }
    }

    private static int hexToInt(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return -1;
    }

    private static class Exp {
        private final String source;
        private int idx;
        private final int length;

        public Exp(String source) {
            this.source = source;
            length = source.length();
        }

        public boolean hasNext() {
            return idx < length;
        }

        public char next() {
            return source.charAt(idx++);
        }

        public boolean ensureCapacity(int minimum) {
            return idx - 1 + minimum < length;
        }
    }

    private static class CloseBuffer {
        private char[] buf = new char[128];
        private int end = 128;

        public void insert(String s) {
            int len = s.length();
            if (len > end) {
                char[] newBuf = new char[Math.max(buf.length * 2, buf.length + len)];
                int offset = newBuf.length - buf.length;
                System.arraycopy(buf, 0, newBuf, offset, buf.length);
                end += offset;
                buf = newBuf;
            }
            for (int i = len - 1; i >= 0; i--) {
                buf[--end] = s.charAt(i);
            }
        }

        public void flushTo(StringBuilder sb) {
            sb.append(buf, end, buf.length - end);
            end = buf.length;
        }

        public void clear() {
            end = buf.length;
        }
    }
}
