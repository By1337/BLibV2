package dev.by1337.core.particle;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.core.command.bcmd.argument.ArgumentChoice;
import dev.by1337.core.command.bcmd.argument.ArgumentDouble;
import dev.by1337.core.command.bcmd.argument.ArgumentInt;
import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.particle.ParticleRender;
import dev.by1337.particle.flow.emit.EmitterGraph;
import dev.by1337.yaml.YamlMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public class EmitterGraphLoader implements AutoCloseable {
    private static final int DEFAULT_TICKS = 500;
    private static final double VIEW_DISTANCE_SQUARED = 30 * 30;

    private final Plugin plugin;
    private final File root;
    private final Map<String, BukkitRunnable> active = new HashMap<>();
    private final Map<String, EmitterGraph> graphs = new HashMap<>();
    private int nextId;

    public EmitterGraphLoader(Plugin plugin) {
        this.plugin = plugin;
        root = new File(plugin.getDataFolder(), "particles");
        if (!root.exists()) {
            ResourceUtil.saveIfNotExist("particles/example.yml", plugin);
        }
        reload();
    }

    public Command<CommandSender> commands() {
        return new Command<CommandSender>("particles")
                .sub(new Command<CommandSender>("reload").executor(sender ->
                        sender.sendMessage("Loaded " + reload() + " particle graph(s).")))
                .sub(new Command<CommandSender>("start").executor(
                        new ArgumentChoice<>("file", graphs::keySet),
                        new ArgumentInt<>("ticks"),
                        new ArgumentChoice<>("name", () -> graphs.keySet().stream().map(s -> s + "#" + nextId).toList()),
                        (sender, file, ticks, name) -> {
                            if (sender instanceof Player player) {
                                start(sender, player.getLocation(), file, ticks, name);
                            } else {
                                sender.sendMessage("This command requires a player. Use start_at from console.");
                            }
                        }))
                .sub(new Command<CommandSender>("start_at").executor(
                        new ArgumentDouble<>("x"),
                        new ArgumentDouble<>("y"),
                        new ArgumentDouble<>("z"),
                        new ArgumentString<>("world"),
                        new ArgumentChoice<>("file", graphs::keySet),
                        new ArgumentInt<>("ticks"),
                        new ArgumentChoice<>("name", graphs::keySet),
                        (sender, x, y, z, worldName, file, ticks, name) -> {
                            if (x == null || y == null || z == null || worldName == null) {
                                sender.sendMessage("Usage: /bdev particles start_at <x> <y> <z> <world> <file> [ticks] [name]");
                                return;
                            }
                            World world = Bukkit.getWorld(worldName);
                            if (world == null) {
                                sender.sendMessage("Unknown world: " + worldName);
                                return;
                            }
                            start(sender, new Location(world, x, y, z), file, ticks, name);
                        }))
                .sub(new Command<CommandSender>("stop").executor(
                        new ArgumentChoice<>("name", active::keySet),
                        (sender, name) -> {
                            if (name == null) {
                                sender.sendMessage("Usage: /bdev particles stop <name>");
                                return;
                            }
                            BukkitRunnable task = active.remove(name);
                            if (task == null) {
                                sender.sendMessage("Effect not running: " + name);
                            } else {
                                task.cancel();
                                sender.sendMessage("Stopped effect: " + name);
                            }
                        }))
                .sub(new Command<CommandSender>("web").executor(sender ->
                        sender.sendMessage(
                                Component.text("https://particle.bdev.space/")
                                        .hoverEvent(Component.text("open"))
                                        .clickEvent(ClickEvent.openUrl("https://particle.bdev.space/"))
                        )));
    }

    public int reload() {
        File[] files = root.listFiles(file -> file.isFile()
                && (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")));
        if (files == null) {
            plugin.getSLF4JLogger().warn("Cannot read particle directory: {}", root);
            return graphs.size();
        }
        Map<String, EmitterGraph> loaded = new HashMap<>();
        for (File file : files) {
            String filename = file.getName();
            String key = filename.substring(0, filename.lastIndexOf('.'));
            try {
                EmitterGraph graph = EmitterGraph.DECODER.decode(YamlMap.load(file).get()).getOrThrow();
                if (loaded.putIfAbsent(key, graph) != null) {
                    plugin.getSLF4JLogger().warn("Duplicate particle graph name: {}", key);
                }
            } catch (Exception e) {
                plugin.getSLF4JLogger().warn("Failed to load particle graph {}", file, e);
            }
        }
        graphs.clear();
        graphs.putAll(loaded);
        return graphs.size();
    }

    private void start(CommandSender sender, Location location, String file, Integer ticks, String name) {
        if (file == null) {
            sender.sendMessage("Specify a particle file name.");
            return;
        }
        String key = file.endsWith(".yaml") ? file.substring(0, file.length() - 5)
                : file.endsWith(".yml") ? file.substring(0, file.length() - 4) : file;
        EmitterGraph graph = graphs.get(key);
        if (graph == null) {
            sender.sendMessage("Unknown particle graph: " + file);
            return;
        }
        int duration = ticks == null ? DEFAULT_TICKS : ticks;
        if (duration <= 0) {
            sender.sendMessage("Ticks must be greater than zero.");
            return;
        }
        String effectName = name;
        if (effectName == null || active.containsKey(effectName)) {
            do {
                effectName = key + ++nextId;
            } while (active.containsKey(effectName));
        }
        final String finalName = effectName;
        BukkitRunnable task = new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (tick >= duration) {
                    active.remove(finalName);
                    cancel();
                    return;
                }
                try {
                    var particles = graph.make(tick++);
                    World world = location.getWorld();
                    if (world != null) {
                        for (Player player : world.getPlayers()) {
                            if (player.getLocation().distanceSquared(location) <= VIEW_DISTANCE_SQUARED) {
                                ParticleRender.render(player, particles, location.getX(), location.getY(), location.getZ());
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getSLF4JLogger().error("Particle effect {} failed", finalName, e);
                    active.remove(finalName);
                    cancel();
                }
            }
        };
        task.runTaskTimer(plugin, 0, 1);
        active.put(finalName, task);
        sender.sendMessage("Started effect: " + finalName);
    }

    @Override
    public void close() {
        for (BukkitRunnable task : active.values()) {
            task.cancel();
        }
        active.clear();
    }
}
