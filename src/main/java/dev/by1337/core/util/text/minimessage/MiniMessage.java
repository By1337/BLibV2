package dev.by1337.core.util.text.minimessage;

import dev.by1337.core.BDev;
import dev.by1337.core.util.RepositoryUtil;
import dev.by1337.core.util.asm.AsmUtils;
import dev.by1337.core.util.text.FontWidth;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.function.Function;

public class MiniMessage {
    private static final Function<String, Component> MINI_MESSAGE;

    private static final Component br = Component.text('\n');
    private static final int CHAT_WIDTH = 320;
    private static final int TITLE_WIDTH = 162;
    private static final int OFF_TITLE_WIDTH = 168;

    public static Component deserialize(String text) {
        if (text.contains("<off_title>")) {
            var arr = text.split("<off_title>");
            Component first = deserialize(arr[0]);
            String second = arr[1];
            int appendPixels = OFF_TITLE_WIDTH - FontWidth.getPixels(first);
            return Component.empty()
                    .append(first)
                    .append(createWidth(appendPixels))
                    .append(deserialize0(second));
        } else if (text.contains("<title_center>")) {
            var arr = text.split("<title_center>");
            Component first = deserialize(arr[0]);
            Component second = deserialize0(arr[1]);
            int firstWidth = FontWidth.getPixels(first);
            int secondWidth = FontWidth.getPixels(second);

            int targetX = (TITLE_WIDTH - secondWidth) / 2;
            int padding = targetX - firstWidth;

            return Component.empty()
                    .append(first)
                    .append(createWidth(Math.max(0, padding)))
                    .append(second);
        } else if (text.contains("<center>")) {
            if (text.contains("\n") || text.contains("<br>")) {
                var arr = text.replace("<br>", "\n").split("\n");
                var res = Component.empty();
                for (int i = 0; i < arr.length; i++) {
                    var s = arr[i];
                    res = res.append(deserialize(s));
                    if (i != arr.length - 1) {
                        res = res.append(br);
                    }
                }
                return res;
            }
            var arr = text.split("<center>");
            Component first = deserialize(arr[0]);
            Component second = deserialize0(arr[1]);
            int firstWidth = FontWidth.getPixels(first);
            int secondWidth = FontWidth.getPixels(second);

            int targetX = (CHAT_WIDTH - secondWidth) / 2;
            int padding = targetX - firstWidth;

            return Component.empty()
                    .append(first)
                    .append(createWidth(Math.max(0, padding)))
                    .append(second);

        } else {
            return deserialize0(text);
        }
    }

    private static Component deserialize0(String text) {
        return MINI_MESSAGE.apply(Legacy2MiniMessage.convert(text));
    }

    private static Component createWidth(int pixels) {
        if (pixels <= 0) {
            return Component.empty();
        }

        int b = 0;
        int n = 0;
        switch (pixels) {
            case 1, 2 -> {
                return Component.empty();
            }
            case 3 -> n = 1;
            case 7 -> n = 2;
            default -> {
                while (pixels % 4 != 0 && pixels >= 5) {
                    b++;
                    pixels -= 5;
                }
                if (pixels != 0) {
                    n += pixels / 4;
                }
            }
        }
        Component component = Component.empty();

        if (n > 0) {
            component = component.append(
                    Component.text(" ".repeat(n))
            );
        }

        if (b > 0) {
            component = component.append(
                    Component.text(" ".repeat(b))
                            .decoration(TextDecoration.BOLD, true)
            );
        }
        return component;
    }

    static {
        Function<String, Component> fun;
        try {
            Class<?> miniMsg = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            String getter;
            try {
                miniMsg.getMethod("miniMessage");
                getter = "miniMessage";
            } catch (NoSuchMethodException e) {
                miniMsg.getMethod("get");
                getter = "get";
            }
            fun = generateNativeBridge(getter);
        } catch (Exception ignored) {
            try {
                Function<String, String> toJson = generateMMToJson();
                fun = s -> GsonComponentSerializer.gson().deserialize(toJson.apply(s));
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        MINI_MESSAGE = fun;
    }


    private static Function<String, Component> generateNativeBridge(String mmGetter) throws Exception {
        ClassNode n = new ClassNode();
        n.name = MiniMessage.class.getPackage().getName().replace(".", "/") + "/MiniMessageBridge";
        n.version = Opcodes.V1_8;
        n.superName = "java/lang/Object";
        n.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;
        n.interfaces.add(Type.getInternalName(Function.class));
        { //init
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            methodNode.visitCode();
            methodNode.visitVarInsn(Opcodes.ALOAD, 0);
            methodNode.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            methodNode.visitInsn(Opcodes.RETURN);
            methodNode.visitEnd();
            n.methods.add(methodNode);
        }
        {
            //Component apply(String t);
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "apply", "(Ljava/lang/String;)Lnet/kyori/adventure/text/Component;", null, null);
            methodNode.visitCode();
            methodNode.visitMethodInsn(Opcodes.INVOKESTATIC, "net/kyori/adventure/text/minimessage/MiniMessage", mmGetter, "()Lnet/kyori/adventure/text/minimessage/MiniMessage;", true);
            methodNode.visitVarInsn(Opcodes.ALOAD, 1);
            methodNode.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/kyori/adventure/text/minimessage/MiniMessage", "deserialize", "(Ljava/lang/Object;)Lnet/kyori/adventure/text/Component;", true);
            methodNode.visitInsn(Opcodes.ARETURN);
            n.methods.add(methodNode);
        }
        {
            //bridge Object apply(Object t);
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            methodNode.visitCode();
            methodNode.visitVarInsn(Opcodes.ALOAD, 0);
            methodNode.visitVarInsn(Opcodes.ALOAD, 1);
            methodNode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
            methodNode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, n.name, "apply", "(Ljava/lang/String;)Lnet/kyori/adventure/text/Component;", false);
            methodNode.visitInsn(Opcodes.ARETURN);
            n.methods.add(methodNode);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        n.accept(cw);
        byte[] arr = cw.toByteArray();

        AsmUtils.dumpGeneratedClass(arr, "MiniMessageBridge");

        Class<?> cl = MethodHandles.lookup().defineHiddenClass(arr, true).lookupClass();
        return (Function<String, Component>) cl.getConstructor().newInstance();
    }

    private static Function<String, String> generateMMToJson() throws Exception {
        ClassNode n = new ClassNode();
        n.name = MiniMessage.class.getPackage().getName().replace(".", "/") + "/MiniMessageBridgeToJson";
        n.version = Opcodes.V1_8;
        n.superName = "java/lang/Object";
        n.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;
        n.interfaces.add(Type.getInternalName(Function.class));
        { //init
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            methodNode.visitCode();
            methodNode.visitVarInsn(Opcodes.ALOAD, 0);
            methodNode.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            methodNode.visitInsn(Opcodes.RETURN);
            methodNode.visitEnd();
            n.methods.add(methodNode);
        }
        {
            //String apply(String t);
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "apply", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
            methodNode.visitCode();

            methodNode.visitMethodInsn(Opcodes.INVOKESTATIC, "net/kyori/adventure/text/serializer/gson/GsonComponentSerializer", "gson", "()Lnet/kyori/adventure/text/serializer/gson/GsonComponentSerializer;", true);
            methodNode.visitMethodInsn(Opcodes.INVOKESTATIC, "net/kyori/adventure/text/minimessage/MiniMessage", "miniMessage", "()Lnet/kyori/adventure/text/minimessage/MiniMessage;", true);
            methodNode.visitVarInsn(Opcodes.ALOAD, 1);
            methodNode.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/kyori/adventure/text/minimessage/MiniMessage", "deserialize", "(Ljava/lang/Object;)Lnet/kyori/adventure/text/Component;", true);
            methodNode.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/kyori/adventure/text/serializer/gson/GsonComponentSerializer", "serialize", "(Lnet/kyori/adventure/text/Component;)Ljava/lang/Object;", true);
            methodNode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
            methodNode.visitInsn(Opcodes.ARETURN);
            n.methods.add(methodNode);
        }
        {
            //bridge Object apply(Object t);
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            methodNode.visitCode();
            methodNode.visitVarInsn(Opcodes.ALOAD, 0);
            methodNode.visitVarInsn(Opcodes.ALOAD, 1);
            methodNode.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
            methodNode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, n.name, "apply", "(Ljava/lang/String;)Ljava/lang/String;", false);
            methodNode.visitInsn(Opcodes.ARETURN);
            n.methods.add(methodNode);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        n.accept(cw);
        byte[] arr = cw.toByteArray();

        AsmUtils.dumpGeneratedClass(arr, "MiniMessageBridgeToJson");

        String paperRepo = "https://repo.papermc.io/repository/maven-public/";
        Path libraries = BDev.HOME_DIR.resolve(".libraries");
        libraries.toFile().mkdirs();
        URL[] urls = {
                RepositoryUtil.download(paperRepo, "net.kyori:adventure-text-serializer-gson:4.18.0", libraries).toUri().toURL(),
                RepositoryUtil.download(paperRepo, "net.kyori:adventure-api:4.18.0", libraries).toUri().toURL(),
                RepositoryUtil.download(paperRepo, "net.kyori:adventure-text-minimessage:4.18.0", libraries).toUri().toURL(),
                RepositoryUtil.download(paperRepo, "net.kyori:adventure-text-serializer-json:4.18.0", libraries).toUri().toURL(),
                RepositoryUtil.download(paperRepo, "net.kyori:option:1.0.0", libraries).toUri().toURL(),
        };


        Class<?> cl = new URLClassLoader(
                "BDevCore-mm-bridge",
                urls,
                MiniMessage.class.getClassLoader()
        ) {

            public Class<?> define(String name, byte[] bytes) {
                return defineClass(name, bytes, 0, bytes.length);
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                return super.findClass(name);
            }

            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith("net.kyori.")) return super.loadClass(name, resolve);
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        return super.loadClass(name, resolve);
                    }
                }
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }

        }.define(n.name.replace("/", "."), arr);

        return (Function<String, String>) cl.getConstructor().newInstance();
    }
}