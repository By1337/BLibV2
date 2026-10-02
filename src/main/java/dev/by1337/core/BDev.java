package dev.by1337.core;

import dev.by1337.cmd.*;
import dev.by1337.core.bridge.inventory.ItemStackSerializer;
import dev.by1337.core.bridge.nbt.NbtBridge;
import dev.by1337.core.bridge.world.BlockEntityUtil;
import dev.by1337.core.command.bcmd.CommandWrapper;
import dev.by1337.core.command.bcmd.TestCommand;
import dev.by1337.core.command.bcmd.argument.ArgumentInt;
import dev.by1337.core.command.bcmd.argument.ArgumentPlayers;
import dev.by1337.core.command.bcmd.argument.GlobalRegistryItem;
import dev.by1337.core.command.bcmd.requires.RequiresPermission;
import dev.by1337.core.legacy.BLibBridge;
import dev.by1337.core.particle.EmitterGraphLoader;
import dev.by1337.core.util.network.ChannelGetter;
import dev.by1337.particle.*;
import dev.by1337.particle.particle.ParticleData;
import dev.by1337.particle.particle.options.VibrationParticleOption;
import dev.by1337.particle.util.Version;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApiStatus.Internal
public class BDev extends JavaPlugin {
    private static final Logger log = LoggerFactory.getLogger(BDev.class);
    public static Path HOME_DIR;
    private CommandWrapper commands;
    private ParticleRenderBootstrapper particles;
    private EmitterGraphLoader emitterGraphLoader;
    private ThemesBotter themes;

    public BDev() {
        getDataFolder().mkdirs();
        HOME_DIR = getDataFolder().toPath();
        Bootstrap.bootstrap(this, getFile());
        BLibBridge.bootstrap(this);
        themes = new ThemesBotter(this);
    }

    @Override
    public void onLoad() {
        try {
            BLibBridge.load(this);
        } catch (Exception e) {
            getSLF4JLogger().warn("Failed fo load legacy BLib!", e);
        }
        themes.onLoad();
    }

    @Override
    public void onEnable() {
        particles = new ParticleRenderBootstrapper("bdev-particles", this);
        particles.enable();
        emitterGraphLoader = new EmitterGraphLoader(this);
        commands = new CommandWrapper(create(), this);
        commands.setPermission("bdev.use");
        commands.register();
        BLibBridge.onEnable();

        int ignored = ItemType.BARRIER.getProtocolId(Version.VERSION.protocolVersion()); //preload
        int ignored2 = BlockType.BARRIER.getProtocolId(Version.VERSION.protocolVersion()); //preload
        themes.onEnable();
    }

    @Override
    public void onDisable() {
        emitterGraphLoader.close();
        particles.disable();
        commands.unregister();
        BLibBridge.onDisable();
        themes.onDisable();
    }

    private Command<CommandSender> create() {
        return new Command<CommandSender>("bdev")
                .requires(new RequiresPermission<>("bdev.use"))
                .sub(emitterGraphLoader.commands())
                .sub(TestCommand.createTest("commands"))
                .sub(new Command<CommandSender>("vibrationtest")
                                .executor(sender -> sendTestVibration(sender, false))
                                .sub(new Command<CommandSender>("block").executor(sender -> sendTestVibration(sender, false)))
                                .sub(new Command<CommandSender>("entity").executor(sender -> sendTestVibration(sender, true)))
                )
                .sub(new Command<CommandSender>("test")
                        .requires(sender -> sender instanceof Player)
                        .executor((sender) -> {
                            Player player = (Player) sender;
                            new ItemStackSerializer.TestImpl().run(player, BCore.getItemStackSerializer());
                            new BlockEntityUtil.TestImpl().run(player, BCore.getBlockEntityUtil());
                            new NbtBridge.TestImpl().run(player, BCore.getNbtBridge());
                            player.sendMessage("done");
                            player.sendMessage(Objects.toString(ChannelGetter.get(player)));
                        })
                )
                .sub(new Command<CommandSender>("handlers")
                        .requires(sender -> sender instanceof Player)
                        .executor((sender) -> {
                            Player player = (Player) sender;
                            var channel = ChannelGetter.get(player);
                            StringBuilder out = new StringBuilder();
                            out.append("out\n");
                            for (Map.Entry<String, ChannelHandler> entry : channel.pipeline()) {
                                if (entry.getValue() instanceof ChannelOutboundHandler) {
                                    out.append(entry.getKey()).append(" ").append(entry.getValue().getClass().getSimpleName()).append("\n");
                                }
                            }
                            sender.sendMessage(out.toString());
                            System.out.println(out);
                        })
                )
                .sub(new Command<CommandSender>("item").executor(
                        new GlobalRegistryItem<>("item"),
                        new ArgumentPlayers<>("players"),
                        new ArgumentInt<>("count"),
                        (s, item, players, count) -> {
                            if (item == null) {
                                s.sendMessage("unknown item!");
                                return;
                            }
                            if (players == null || players.isEmpty()) {
                                if (s instanceof Player pl) {
                                    players = List.of(pl);
                                } else {
                                    s.sendMessage("players not found!");
                                    return;
                                }
                            }
                            int x = count == null ? 1 : Math.max(1, Math.min(count, 64));
                            for (Player player : players) {
                                player.getInventory().addItem(item.asQuantity(x)).forEach((slot, i) -> player.getWorld().dropItemNaturally(player.getLocation(), i));
                            }
                            s.sendMessage("done");
                        }))
                .sub(new Command<CommandSender>("listening").executor(
                        new Argument<CommandSender, Class<?>>("count") {
                            @Override
                            public void parse(CommandSender ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
                                var input = reader.readString();
                                try {
                                    out.put(name, Class.forName(input));
                                } catch (Exception ex) {
                                    throw new CommandMsgError(ex.getMessage());
                                }
                            }

                            @Override
                            public void suggest(CommandSender ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
                                try {
                                    for (HandlerList handlerList : HandlerList.getHandlerLists()) {

                                    }

/*                                    var f = EventExecutor.class.getDeclaredField("eventExecutorMap");
                                    f.setAccessible(true);
                                    ConcurrentMap<Method, Class<? extends EventExecutor>> eventExecutorMap = (ConcurrentMap<Method, Class<? extends EventExecutor>>) f.get(null);
                                    var input = reader.readString();
                                    for (Method method : eventExecutorMap.keySet()) {
                                        var arr = method.getParameterTypes();
                                        if (arr.length != 1) continue;
                                        var name = arr[0].getCanonicalName();
                                        if (name.startsWith(input)){
                                            suggestions.suggest(name);
                                        }
                                    }*/
                                } catch (Exception ex) {
                                    throw new CommandMsgError(ex.getMessage());
                                }
                            }
                        },
                        (s, cl) -> {
                            if (cl == null) {
                                s.sendMessage("use /bdev listening <class>");
                                return;
                            }
                            StringBuilder sb = new StringBuilder("[\n");
                            // Class<?> cl = BlockExplodeEvent.class;
                            try {
                                var mn = cl.getMethod("getHandlerList");
                                HandlerList list = (HandlerList) mn.invoke(null);
                                for (RegisteredListener listener : list.getRegisteredListeners()) {
                                    sb.append(listener.getPlugin().getName()).append(": ").append(listener.getListener().getClass().getCanonicalName()).append("\n");
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            sb.append("]");
                            log.info(sb.toString());
                            s.sendMessage(sb.toString());
                        }))
                ;
    }
    private void sendTestVibration(CommandSender sender, boolean entityDestination) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command requires a player.");
            return;
        }
        var start = player.getEyeLocation();
        var target = start.clone().add(start.getDirection().multiply(4));
        var origin = new VibrationParticleOption.BlockPos(start.getBlockX(), start.getBlockY(), start.getBlockZ());
        VibrationParticleOption option = entityDestination
                ? VibrationParticleOption.toEntity(origin, player.getEntityId(), 0.0f, 30)
                : VibrationParticleOption.toBlock(origin,
                new VibrationParticleOption.BlockPos(target.getBlockX(), target.getBlockY(), target.getBlockZ()), 30);
        ParticleRender.render(player, ParticleData.of(ParticleType.VIBRATION, option),
                start.getX(), start.getY(), start.getZ());
        sender.sendMessage("Vibration sent to " + (entityDestination ? "you" : "the block ahead") + ".");
    }
}
