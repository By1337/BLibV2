package dev.by1337.core.util.text.minimessage;

import dev.by1337.core.BDev;
import dev.by1337.core.util.RepositoryUtil;
import dev.by1337.core.util.asm.AsmUtils;
import net.kyori.adventure.text.Component;
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

    public static Component deserialize(String text) {
        return MINI_MESSAGE.apply(Legacy2MiniMessage.convert(text));
    }

    static {
        Function<String, Component> fun = null;
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