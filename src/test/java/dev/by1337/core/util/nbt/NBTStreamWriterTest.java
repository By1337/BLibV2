package dev.by1337.core.util.nbt;

import dev.by1337.core.nbt.NBTWriter;
import dev.by1337.core.util.math.FastExpressionParser;
import dev.by1337.core.util.text.FontWidth;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class NBTStreamWriterTest {

    // @Test
    public void test2() throws IOException {
        String[] types = new String[]{
                //  "TAG_BYTE",
                //  "byte",
                "TAG_SHORT",
                "short",
                "TAG_INT",
                "int",
                "TAG_LONG",
                "long",
                "TAG_FLOAT",
                "float",
                "TAG_DOUBLE",
                "double",
                "TAG_BYTE_ARRAY",
                "byte[]",
                "TAG_STRING",
                "String",
                "TAG_INT_ARRAY",
                "int[]",
                "TAG_LONG_ARRAY",
                "long[]",
                //  "TAG_LIST",
                //  "Consumer<NBTWriter>",
                //  "TAG_COMPOUND",
                //  "Consumer<NBTWriter>",
        };
        String s = """
                public void named{f_name}(String name, {p_type} value) {
                        named(name);
                        push{f_name}(value);
                    }
                    public void unnamed{f_name}({p_type} value) {
                        unnamed();
                        push{f_name}(value);
                    }
                    public void push{f_name}({p_type} v) {
                        size++;
                        if (listType == -1) {
                            buf.writeByte({tag_id});
                            if (key != null) {
                                writeUTF(key);
                            }
                            buf.writeByte(v);//todo write
                        } else if (listType == TAG_COMPOUND) {
                            buf.writeByte({tag_id});
                            buf.writeShort(0);
                            buf.writeByte(v);//todo write
                            buf.writeByte(0);
                        } else {
                            buf.writeByte(v);//todo write
                        }
                    }
                """;
        for (int i = 0; i < types.length; i += 2) {
            String tag_id = types[i];
            String p_type = types[i + 1];
            String f_name = tag_id.substring("TAG_".length());
            f_name = f_name.charAt(0) + f_name.toLowerCase().substring(1);
            System.out.println(s
                    .replace("{f_name}", f_name)
                    .replace("{p_type}", p_type)
                    .replace("{tag_id}", tag_id)
            );
        }
    }


    //  @Test
    public void test() throws IOException {
        // '̧'
        System.out.println(FontWidth.getWidth('̧'));// "̧ 3.0"
        StringBuilder sb = new StringBuilder();
        char last = '1';
        for (Int2FloatMap.Entry entry : FontWidth.FRONT.int2FloatEntrySet()) {
            if (entry.getFloatValue() == 3){
                System.out.println((char) entry.getIntKey() + " " + entry.getFloatValue());
                sb.append("      - \"").append((char) entry.getIntKey()).append(" ").append(entry.getFloatValue()).append("\"\n");
            }
            last = (char) entry.getIntKey();
        }
       // System.out.println(sb);
    }
    private static int normalize(int n) {
        int mod = n % 20;

        return switch (mod) {
            case 1, 6, 11 -> n - 1;
            case 2 -> n - 2;
            case 3, 7 -> n + 1;
            default -> n;
        };
    }

    public static int createWidth(int pixels) {
        if (pixels <= 0) {
            return 0;
        }

        int target = normalize(pixels);

        int bold = target % 4;
        int normal = (target - bold * 5) / 4;

        Component component = Component.empty();

        if (normal > 0) {
            component = component.append(
                    Component.text(" ".repeat(normal))
            );
        }

        if (bold > 0) {
            component = component.append(
                    Component.text(" ".repeat(bold))
                            .decoration(TextDecoration.BOLD, true)
            );
        }
       return Math.max(0, bold)*5 + Math.max(0, normal) * 4;
      //  return component;
    }

  //  @Test
    public void te2() throws IOException {
        try (var fis = new FileInputStream("/home/by1337/Downloads/inv_sync_blob_repository-data.bin");
             var gzip = new GZIPInputStream(fis);
             var dis = new DataInputStream(gzip)
        ) {
            var v = BinaryNbt.readUnnamedTag(dis, 512);
            System.out.println(v);
        }

    }

    //save tags
    // bukkit - какой-то кринж
    // recipeBook - синхронизируем открытые рецепты
    // XpTotal
    // XpP
    // XpLevel
    // XpSeed
    // Spigot.ticksLived
    // Inventory
    // playerGameType
    // equipment
    // EnderItems

    // also advancements stats

    @Test
    public void te22() throws IOException {
        int x = 32 << 20;
        System.out.println(x);
        System.out.println(x/1024);
        System.out.println(x/1024/1024);
        System.out.println(1<<10); //KB
        System.out.println(1<<20); //MB
        System.out.println(1<<30); //GB
    }

    private static String first(String s) {
        StringBuilder out = new StringBuilder();
        int size = s.length();
        final StringBuilder number = new StringBuilder();
        for (int i = 0; i < size; i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                long l = Long.parseLong(number.toString());
                number.setLength(0);
                switch (c) {
                    case 't' -> out.append("(").append(l*50L).append(")+");
                    case 's' -> out.append("(").append(l*1000L).append(")+");
                    case 'm' -> out.append("(").append(l*60000L).append(")+");
                    case 'h' -> out.append("(").append(l*3600000L).append(")+");
                    case 'd' -> out.append("(").append(l*86400000L).append(")+");
                    case 'w' -> out.append("(").append(l*604800000L).append(")+");
                    //  case "mo" -> out.append("*2629746000");
                    case 'y' -> out.append("(").append(l*31556908800L).append(")+");
                    default -> out.append(c);
                }
            }
        }
        return out.append("0").toString();
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder();
        int size = s.length();
        boolean isOpen = false;
        boolean lastIsDigit = false;
        for (int i = 0; i < size; i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                if (!isOpen && !out.isEmpty()) {
                    out.append("+(");
                } else if (!isOpen) {
                    out.append("(");
                }
                isOpen = true;
                lastIsDigit = true;
                out.append(c);
            } else {
                switch (c) {
                    case 't' -> out.append("*50");
                    case 's' -> out.append("*1000");
                    case 'm' -> out.append("*60000");
                    case 'h' -> out.append("*3600000");
                    case 'd' -> out.append("*86400000");
                    case 'w' -> out.append("*604800000");
                    //  case "mo" -> out.append("*2629746000");
                    case 'y' -> out.append("*31556908800");
                    default -> out.append(c);
                }
                out.append(")");
                isOpen = false;
                lastIsDigit = false;
            }
        }
        if (isOpen) {
            out.append(")");
        }
        return out.toString();
    }

    private static long getResult(int x, String s) {
        return switch (s) {
            case "t" -> 50L * x;
            case "s" -> 1000L * x;
            case "m" -> 60000L * x;
            case "h" -> 3600000L * x;
            case "d" -> 86400000L * x;
            case "w" -> 604800000L * x;
            case "mo" -> 2629746000L * x;
            case "y" -> 31556908800L * x;
            default -> 0;
        };
    }

    public static long getTime(String s) {
        final StringBuilder number = new StringBuilder();
        final StringBuilder type = new StringBuilder();
        long out = 0;
        char[] arr = s.toCharArray();

        for (char c : arr) {
            if (Character.isDigit(c)) {
                if (!type.isEmpty() && !number.isEmpty()) {
                    out += getResult(Integer.parseInt(number.toString()), type.toString());
                    number.setLength(0);
                    type.setLength(0);
                }
                number.append(c);
            } else {
                type.append(c);
            }
        }
        if (!type.isEmpty() && !number.isEmpty()) {
            out += getResult(Integer.parseInt(number.toString()), type.toString());
        }
        return out;
    }

}