package dev.by1337.core.util.network;

import dev.by1337.core.BDev;
import dev.by1337.core.util.asm.AsmUtils;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

/**
 * The ChannelGetterCreator class is responsible for dynamically generating instances of the
 * ChannelGetter interface to retrieve Netty Channel objects associated with Bukkit Player instances.
 * The class uses reflection, bytecode generation, and hidden class definitions to achieve this.
 * It attempts to find the suitable fields and methods required for accessing the Channel in the
 * server's or plugin's runtime environment.
 */
public class ChannelGetterCreator {

    private static final Logger log = LoggerFactory.getLogger("BDevCore");

    /**
     * Creates a ChannelGetter implementation to retrieve a Netty Channel associated with a specified player.
     * This method dynamically navigates through the fields of a player's underlying NMS object
     * to locate and retrieve the desired Channel instance. Depending on the runtime environment, it may
     * utilize a generated implementation through bytecode or reflective access.
     *
     * @param player is a real player instance required for proper generation of ChannelGetter.
     * @return A ChannelGetter implementation capable of extracting the Netty Channel from the provided player.
     */
    public static ChannelGetter create(Player player) {
        try {
            var getHandle = player.getClass().getDeclaredMethod("getHandle");
            getHandle.setAccessible(true);
            var nmsType = getHandle.invoke(player);
            var result = findType(nmsType.getClass(), Channel.class, new Stack<>(), true);
            if (result != null) {
                try {
                    return BytecodeGenerator.generateGetter(getHandle, result, player.getClass());
                } catch (Exception e) {
                    log.warn("Failed to generate ChannelGetter");
                }
            } else {
                result = findType(nmsType.getClass(), Channel.class, new Stack<>(), false);
            }
            if (result == null) {
                log.error("Failed to create ChannelGetter");
                return pl -> null;
            }

            return ReflectGenerator.generateGetter(getHandle, result, player);
        } catch (Throwable e) {
            log.error("Failed to create ChannelGetter", e);
            return pl -> null;
        }
    }


    /**
     * Finds a sequence of fields in a class hierarchy that leads to a specific field type.
     *
     * @param in      The class in which the search starts.
     * @param type    The target field type to search for.
     * @param stack   A stack to avoid recursive loops in the class hierarchy.
     * @param pubOnly A flag indicating whether to only include public fields.
     * @return A list of fields representing the path to the target type, or null if no path is found.
     */
    private static List<Field> findType(Class<?> in, Class<?> type, Stack<Class<?>> stack, boolean pubOnly) {
        if (in == null || stack.contains(in)) return null;
        stack.push(in);

        List<Field> bestPath = null;
        for (Field field : in.getDeclaredFields()) {
            if (pubOnly && !Modifier.isPublic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                if (type.isAssignableFrom(field.getType())) {
                    return List.of(field);
                } else if (!field.getType().isPrimitive()) {
                    var res = findType(field.getType(), type, stack, pubOnly);
                    if (res != null) {
                        List<Field> candidate = new ArrayList<>();
                        candidate.add(field);
                        candidate.addAll(res);
                        if (bestPath == null || candidate.size() < bestPath.size()) {
                            bestPath = candidate;
                        }
                    }
                }
            } catch (InaccessibleObjectException ignored) {
            }
        }
        stack.pop();
        if (bestPath != null) {
            var superPath = findType(in.getSuperclass(), type, stack, pubOnly);
            return superPath == null ? bestPath : bestPath.size() < superPath.size() ? bestPath : superPath;
        }

        return findType(in.getSuperclass(), type, stack, pubOnly);
    }


    /**
     * A utility class that provides reflection-based functionality for dynamically generating
     * implementations of the {@link ChannelGetter} interface. It operates using MethodHandles
     * and dynamically constructs getters for accessing nested fields or invoking methods
     * on objects.
     */
    private static class ReflectGenerator {
        private static ChannelGetter generateGetter(Method getHandle, List<Field> path, Player player) throws Throwable {
            var lookup = MethodHandles.lookup();
            MethodHandle get = lookup.findVirtual(player.getClass(), getHandle.getName(), MethodType.methodType(getHandle.getReturnType()));

            Function<Object, Object> last = invokerOf(get, lookup);

            for (Field field : path) {
                field.setAccessible(true);
                last = last.andThen(getterOf(field, lookup));
            }

            Function<Object, Object> resultMapper = last;
            return pl -> (Channel) resultMapper.apply(pl);
        }

        public static <T, R> Function<T, R> getterOf(Field field, MethodHandles.Lookup lookup) throws Throwable {
            return invokerOf(lookup.unreflectGetter(field), lookup);
        }

        @SuppressWarnings("unchecked")
        private static <T, R> Function<T, R> invokerOf(MethodHandle handle, MethodHandles.Lookup lookup) throws Throwable {
            MethodType type = handle.type();
            return (Function<T, R>) LambdaMetafactory.metafactory(
                    lookup,
                    "apply",
                    MethodType.methodType(Function.class, MethodHandle.class),
                    type.generic(),
                    MethodHandles.exactInvoker(type),
                    type
            ).getTarget().invokeExact(handle);
        }
    }

    /**
     * The BytecodeGenerator class is a utility that programmatically generates bytecode to create an implementation
     * of the ChannelGetter interface. It dynamically constructs a class that defines a method for retrieving
     * the Netty Channel from a Bukkit Player object by navigating through specified field paths.
     * <p>
     * This class uses the ASM library for bytecode manipulation and generation.
     * <p>
     * Functionality:
     * - Creates a dynamically generated class implementing the ChannelGetter interface.
     * - Defines the getChannel(Player pl) method to extract the Netty Channel object from the provided player instance.
     * - Works with the Java 8 class version and ensures proper initialization of the generated class.
     * <p>
     * Usage Notes:
     * - This class is intended for internal use and is not expected to be used directly by external consumers.
     * - The generated class is isolated as a hidden class to avoid polluting the runtime namespace.
     * - Errors during bytecode generation or instantiation of the generated class will result in a RuntimeException.
     */
    private static class BytecodeGenerator {
        private static ChannelGetter generateGetter(Method getHandle, List<Field> path, Class<?> craftPlater) {
            ClassNode n = new ClassNode();
            n.name = ChannelGetterCreator.class.getPackage().getName().replace(".", "/") + "/ChannelGetter";
            n.version = Opcodes.V1_8;
            n.superName = "java/lang/Object";
            n.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;
            n.interfaces.add(Type.getInternalName(ChannelGetter.class));
            {
                MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                methodNode.visitCode();
                methodNode.visitVarInsn(Opcodes.ALOAD, 0);
                methodNode.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                methodNode.visitInsn(Opcodes.RETURN);
                methodNode.visitEnd();
                n.methods.add(methodNode);
            }
            //Channel getChannel(Player pl);
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "getChannel", "(Lorg/bukkit/entity/Player;)Lio/netty/channel/Channel;", null, null);
            methodNode.visitCode();
            methodNode.visitVarInsn(Opcodes.ALOAD, 1);
            String craftPlayer = craftPlater.getName().replace(".", "/");
            methodNode.visitTypeInsn(Opcodes.CHECKCAST, craftPlayer);

            String nmsPlayer = getHandle.getReturnType().getName().replace(".", "/");
            methodNode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, craftPlayer, "getHandle", "()L" + nmsPlayer + ";", false);
            String stackType = nmsPlayer;
            for (Field field : path) {
                methodNode.visitFieldInsn(Opcodes.GETFIELD, stackType, field.getName(), Type.getDescriptor(field.getType()));
                stackType = Type.getInternalName(field.getType());
            }
            methodNode.visitInsn(Opcodes.ARETURN);
            methodNode.visitEnd();
            n.methods.add(methodNode);

            try {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                n.accept(cw);
                byte[] arr = cw.toByteArray();

                AsmUtils.dumpGeneratedClass(arr, "ChannelGetter");

                Class<?> cl = MethodHandles.lookup().defineHiddenClass(arr, true).lookupClass();
                return (ChannelGetter) cl.getConstructor().newInstance();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}