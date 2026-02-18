package dev.by1337.core;

import dev.by1337.cmd.Command;
import dev.by1337.core.bridge.inventory.ItemStackSerializer;
import dev.by1337.core.bridge.nbt.NbtBridge;
import dev.by1337.core.bridge.world.BlockEntityUtil;
import dev.by1337.core.command.bcmd.CommandWrapper;
import dev.by1337.core.command.bcmd.TestCommand;
import dev.by1337.core.command.bcmd.requires.RequiresPermission;
import dev.by1337.core.legacy.BLibBridge;
import dev.by1337.core.util.network.ChannelGetter;
import dev.by1337.particle.*;
import dev.by1337.particle.particle.ParticleData;
import dev.by1337.particle.util.Version;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.Objects;

@ApiStatus.Internal
public class BDev extends JavaPlugin {
    public static boolean IS_SNAPSHOT = true;
    public static Path HOME_DIR;
    private CommandWrapper commands;
    private ParticleRenderBootstrapper particles;

    public BDev() {
        getDataFolder().mkdirs();
        HOME_DIR = getDataFolder().toPath();
        Bootstrap.bootstrap(this);
        BLibBridge.bootstrap(this);
    }

    @Override
    public void onLoad() {
        try {
            BLibBridge.load(this);
        } catch (Exception e) {
            getSLF4JLogger().warn("Failed fo load legacy BLib!", e);
        }
    }

    @Override
    public void onEnable() {
        commands = new CommandWrapper(create(), this);
        commands.setPermission("bdev.use");
        commands.register();
        BLibBridge.onEnable();

        particles = new ParticleRenderBootstrapper("bdev_particles", this);
        particles.enable();
        int ignored = ItemType.BARRIER.getProtocolId(Version.VERSION.protocolVersion()); //preload
    }

    @Override
    public void onDisable() {
        particles.disable();
        commands.unregister();
        BLibBridge.onDisable();
    }

    private Command<CommandSender> create() {
        return new Command<CommandSender>("bdev")
                .requires(new RequiresPermission<>("bdev.use"))
                .sub(TestCommand.createTest("commands"))
                .sub(new Command<CommandSender>("test")
                        .requires(sender -> sender instanceof Player)
                        .executor((sender, args) -> {
                            Player player = (Player) sender;
                            new ItemStackSerializer.TestImpl().run(player, BCore.getItemStackSerializer());
                            new BlockEntityUtil.TestImpl().run(player, BCore.getBlockEntityUtil());
                            new NbtBridge.TestImpl().run(player, BCore.getNbtBridge());
                            player.sendMessage("done");
                            player.sendMessage(Objects.toString(ChannelGetter.get(player)));
                        })
                )
                .sub(new Command<CommandSender>("particles")
                        .requires(sender -> sender instanceof Player)
                        .executor((sender, args) -> {
                            Player player = (Player) sender;
                            var loc = player.getLocation();
                            ParticleRender.render(
                                    player,
                                    PluginParticleRender.circle(
                                            256, 10, ParticleData.of(ParticleType.SOUL_FIRE_FLAME)
                                    ),
                                    loc.getX(),
                                    loc.getY(),
                                    loc.getZ()
                            );
                        })
                )
                ;
    }
}
