package dev.by1337.core.util.nbt;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

public class Test {

    @org.junit.Test
    public void run() throws Exception {
        try (var dis = new DataInputStream(new GZIPInputStream(new FileInputStream("/home/by1337/deploy/tt/264069f3-d7cc-335d-9346-14c0398ec9d6.dat")))) {
            System.out.println(BinaryNbt.readUnnamedTag(dis, 512).toString()
                    .replace("{\"", "{\n\"")
                    .replace("\":", "\": ")
                    .replace("\",", "\",\n")
            );
        }

    }
}
