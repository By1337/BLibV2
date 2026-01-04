package dev.by1337.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.bukkit.Bukkit;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ServerVersion {
    public static final String CURRENT_ID;
    public static final int CURRENT;
    public static final int CURRENT_PROTOCOL;

    public static final int V1_16_5 = toInt("1.16.5");
    public static final int V1_17 = toInt("1.17");
    public static final int V1_17_1 = toInt("1.17.1");
    public static final int V1_18 = toInt("1.18");
    public static final int V1_18_1 = toInt("1.18.1");
    public static final int V1_18_2 = toInt("1.18.2");
    public static final int V1_19 = toInt("1.19");
    public static final int V1_19_1 = toInt("1.19.1");
    public static final int V1_19_2 = toInt("1.19.2");
    public static final int V1_19_3 = toInt("1.19.3");
    public static final int V1_19_4 = toInt("1.19.4");
    public static final int V1_20 = toInt("1.20");
    public static final int V1_20_1 = toInt("1.20.1");
    public static final int V1_20_2 = toInt("1.20.2");
    public static final int V1_20_3 = toInt("1.20.3");
    public static final int V1_20_4 = toInt("1.20.4");
    public static final int V1_20_5 = toInt("1.20.5");
    public static final int V1_20_6 = toInt("1.20.6");
    public static final int V1_21 = toInt("1.21");
    public static final int V1_21_1 = toInt("1.21.1");
    public static final int V1_21_2 = toInt("1.21.2");
    public static final int V1_21_3 = toInt("1.21.3");
    public static final int V1_21_4 = toInt("1.21.4");
    public static final int V1_21_5 = toInt("1.21.5");
    public static final int V1_21_6 = toInt("1.21.6");
    public static final int V1_21_7 = toInt("1.21.7");
    public static final int V1_21_8 = toInt("1.21.8");
    public static final int V1_21_9 = toInt("1.21.9");
    public static final int V1_21_10 = toInt("1.21.10");
    public static final int V1_21_11 = toInt("1.21.11");

    public static class Protocol {
        public static final int CURRENT = ServerVersion.CURRENT_PROTOCOL;
        public static final int V754 = 754;
        public static final int V1_16_5 = V754;
        public static final int V755 = 755;
        public static final int V1_17 = V755;
        public static final int V756 = 756;
        public static final int V1_17_1 = V756;
        public static final int V757 = 757;
        public static final int V1_18 = V757;
        public static final int V1_18_1 = V757;
        public static final int V758 = 758;
        public static final int V1_18_2 = V758;
        public static final int V759 = 759;
        public static final int V1_19 = V759;
        public static final int V760 = 760;
        public static final int V1_19_1 = V760;
        public static final int V1_19_2 = V760;
        public static final int V761 = 761;
        public static final int V1_19_3 = V761;
        public static final int V762 = 762;
        public static final int V1_19_4 = V762;
        public static final int V763 = 763;
        public static final int V1_20 = V763;
        public static final int V1_20_1 = V763;
        public static final int V764 = 764;
        public static final int V1_20_2 = V764;
        public static final int V765 = 765;
        public static final int V1_20_3 = V765;
        public static final int V1_20_4 = V765;
        public static final int V766 = 766;
        public static final int V1_20_5 = V766;
        public static final int V1_20_6 = V766;
        public static final int V767 = 767;
        public static final int V1_21 = V767;
        public static final int V1_21_1 = V767;
        public static final int V768 = 768;
        public static final int V1_21_2 = V768;
        public static final int V1_21_3 = V768;
        public static final int V769 = 769;
        public static final int V1_21_4 = V769;
        public static final int V770 = 770;
        public static final int V1_21_5 = V770;
        public static final int V771 = 771;
        public static final int V1_21_6 = V771;
        public static final int V772 = 772;
        public static final int V1_21_7 = V772;
        public static final int V1_21_8 = V772;
        public static final int V773 = 773;
        public static final int V1_21_9 = V773;
        public static final int V1_21_10 = V773;
        public static final int V774 = 774;
        public static final int V1_21_11 = V774;
    }

    private static int toInt(String id) {
        String[] parts = id.split("\\.");
        int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return (major << 16) | (minor << 8) | patch;
    }

    static {
        try (InputStream stream = Bukkit.getServer().getClass().getResourceAsStream("/version.json")) {
            if (stream == null) {
                throw new FileNotFoundException("not found version.json file!");
            } else {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    Gson gson = new Gson();
                    JsonReader jsonReader = new JsonReader(reader);
                    JsonObject object = gson.getAdapter(JsonObject.class).read(jsonReader);
                    CURRENT_ID = object.get("id").getAsString();
                    CURRENT = toInt(CURRENT_ID);
                    CURRENT_PROTOCOL = object.get("protocol_version").getAsInt();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean is1_16_5() {
        return CURRENT == V1_16_5;
    }

    public static boolean is1_16_5orNewer() {
        return CURRENT >= V1_16_5;
    }

    public static boolean is1_16_5orOlder() {
        return CURRENT <= V1_16_5;
    }

    public static boolean is1_17() {
        return CURRENT == V1_17;
    }

    public static boolean is1_17orNewer() {
        return CURRENT >= V1_17;
    }

    public static boolean is1_17orOlder() {
        return CURRENT <= V1_17;
    }

    public static boolean is1_17_1() {
        return CURRENT == V1_17_1;
    }

    public static boolean is1_17_1orNewer() {
        return CURRENT >= V1_17_1;
    }

    public static boolean is1_17_1orOlder() {
        return CURRENT <= V1_17_1;
    }

    public static boolean is1_18() {
        return CURRENT == V1_18;
    }

    public static boolean is1_18orNewer() {
        return CURRENT >= V1_18;
    }

    public static boolean is1_18orOlder() {
        return CURRENT <= V1_18;
    }

    public static boolean is1_18_1() {
        return CURRENT == V1_18_1;
    }

    public static boolean is1_18_1orNewer() {
        return CURRENT >= V1_18_1;
    }

    public static boolean is1_18_1orOlder() {
        return CURRENT <= V1_18_1;
    }

    public static boolean is1_18_2() {
        return CURRENT == V1_18_2;
    }

    public static boolean is1_18_2orNewer() {
        return CURRENT >= V1_18_2;
    }

    public static boolean is1_18_2orOlder() {
        return CURRENT <= V1_18_2;
    }

    public static boolean is1_19() {
        return CURRENT == V1_19;
    }

    public static boolean is1_19orNewer() {
        return CURRENT >= V1_19;
    }

    public static boolean is1_19orOlder() {
        return CURRENT <= V1_19;
    }

    public static boolean is1_19_1() {
        return CURRENT == V1_19_1;
    }

    public static boolean is1_19_1orNewer() {
        return CURRENT >= V1_19_1;
    }

    public static boolean is1_19_1orOlder() {
        return CURRENT <= V1_19_1;
    }

    public static boolean is1_19_2() {
        return CURRENT == V1_19_2;
    }

    public static boolean is1_19_2orNewer() {
        return CURRENT >= V1_19_2;
    }

    public static boolean is1_19_2orOlder() {
        return CURRENT <= V1_19_2;
    }

    public static boolean is1_19_3() {
        return CURRENT == V1_19_3;
    }

    public static boolean is1_19_3orNewer() {
        return CURRENT >= V1_19_3;
    }

    public static boolean is1_19_3orOlder() {
        return CURRENT <= V1_19_3;
    }

    public static boolean is1_19_4() {
        return CURRENT == V1_19_4;
    }

    public static boolean is1_19_4orNewer() {
        return CURRENT >= V1_19_4;
    }

    public static boolean is1_19_4orOlder() {
        return CURRENT <= V1_19_4;
    }

    public static boolean is1_20() {
        return CURRENT == V1_20;
    }

    public static boolean is1_20orNewer() {
        return CURRENT >= V1_20;
    }

    public static boolean is1_20orOlder() {
        return CURRENT <= V1_20;
    }

    public static boolean is1_20_1() {
        return CURRENT == V1_20_1;
    }

    public static boolean is1_20_1orNewer() {
        return CURRENT >= V1_20_1;
    }

    public static boolean is1_20_1orOlder() {
        return CURRENT <= V1_20_1;
    }

    public static boolean is1_20_2() {
        return CURRENT == V1_20_2;
    }

    public static boolean is1_20_2orNewer() {
        return CURRENT >= V1_20_2;
    }

    public static boolean is1_20_2orOlder() {
        return CURRENT <= V1_20_2;
    }

    public static boolean is1_20_3() {
        return CURRENT == V1_20_3;
    }

    public static boolean is1_20_3orNewer() {
        return CURRENT >= V1_20_3;
    }

    public static boolean is1_20_3orOlder() {
        return CURRENT <= V1_20_3;
    }

    public static boolean is1_20_4() {
        return CURRENT == V1_20_4;
    }

    public static boolean is1_20_4orNewer() {
        return CURRENT >= V1_20_4;
    }

    public static boolean is1_20_4orOlder() {
        return CURRENT <= V1_20_4;
    }

    public static boolean is1_20_5() {
        return CURRENT == V1_20_5;
    }

    public static boolean is1_20_5orNewer() {
        return CURRENT >= V1_20_5;
    }

    public static boolean is1_20_5orOlder() {
        return CURRENT <= V1_20_5;
    }

    public static boolean is1_20_6() {
        return CURRENT == V1_20_6;
    }

    public static boolean is1_20_6orNewer() {
        return CURRENT >= V1_20_6;
    }

    public static boolean is1_20_6orOlder() {
        return CURRENT <= V1_20_6;
    }

    public static boolean is1_21() {
        return CURRENT == V1_21;
    }

    public static boolean is1_21orNewer() {
        return CURRENT >= V1_21;
    }

    public static boolean is1_21orOlder() {
        return CURRENT <= V1_21;
    }

    public static boolean is1_21_1() {
        return CURRENT == V1_21_1;
    }

    public static boolean is1_21_1orNewer() {
        return CURRENT >= V1_21_1;
    }

    public static boolean is1_21_1orOlder() {
        return CURRENT <= V1_21_1;
    }

    public static boolean is1_21_2() {
        return CURRENT == V1_21_2;
    }

    public static boolean is1_21_2orNewer() {
        return CURRENT >= V1_21_2;
    }

    public static boolean is1_21_2orOlder() {
        return CURRENT <= V1_21_2;
    }

    public static boolean is1_21_3() {
        return CURRENT == V1_21_3;
    }

    public static boolean is1_21_3orNewer() {
        return CURRENT >= V1_21_3;
    }

    public static boolean is1_21_3orOlder() {
        return CURRENT <= V1_21_3;
    }

    public static boolean is1_21_4() {
        return CURRENT == V1_21_4;
    }

    public static boolean is1_21_4orNewer() {
        return CURRENT >= V1_21_4;
    }

    public static boolean is1_21_4orOlder() {
        return CURRENT <= V1_21_4;
    }

    public static boolean is1_21_5() {
        return CURRENT == V1_21_5;
    }

    public static boolean is1_21_5orNewer() {
        return CURRENT >= V1_21_5;
    }

    public static boolean is1_21_5orOlder() {
        return CURRENT <= V1_21_5;
    }

    public static boolean is1_21_6() {
        return CURRENT == V1_21_6;
    }

    public static boolean is1_21_6orNewer() {
        return CURRENT >= V1_21_6;
    }

    public static boolean is1_21_6orOlder() {
        return CURRENT <= V1_21_6;
    }

    public static boolean is1_21_7() {
        return CURRENT == V1_21_7;
    }

    public static boolean is1_21_7orNewer() {
        return CURRENT >= V1_21_7;
    }

    public static boolean is1_21_7orOlder() {
        return CURRENT <= V1_21_7;
    }

    public static boolean is1_21_8() {
        return CURRENT == V1_21_8;
    }

    public static boolean is1_21_8orNewer() {
        return CURRENT >= V1_21_8;
    }

    public static boolean is1_21_8orOlder() {
        return CURRENT <= V1_21_8;
    }

    public static boolean is1_21_9() {
        return CURRENT == V1_21_9;
    }

    public static boolean is1_21_9orNewer() {
        return CURRENT >= V1_21_9;
    }

    public static boolean is1_21_9orOlder() {
        return CURRENT <= V1_21_9;
    }

    public static boolean is1_21_10() {
        return CURRENT == V1_21_10;
    }

    public static boolean is1_21_10orNewer() {
        return CURRENT >= V1_21_10;
    }

    public static boolean is1_21_10orOlder() {
        return CURRENT <= V1_21_10;
    }

    public static boolean is1_21_11() {
        return CURRENT == V1_21_11;
    }

    public static boolean is1_21_11orNewer() {
        return CURRENT >= V1_21_11;
    }

    public static boolean is1_21_11orOlder() {
        return CURRENT <= V1_21_11;
    }

}