package dev.by1337.core.plugin;

import dev.by1337.cmd.Command;
import dev.by1337.core.command.bcmd.CommandWrapper;
import dev.by1337.core.util.io.ResourceUtil;
import dev.by1337.edsl.MessageManager;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.yaml.YamlValue;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Standard base class for BDev Paper plugins.
 * {@link #onLoad()} initializes {@link #messages} from {@code messages.yml} and
 * {@code lang.yml}, copying bundled defaults when needed. Subclasses overriding
 * {@code onLoad} must call {@code super.onLoad()} before using messages.
 * <p>
 * Use {@link #registerCommand(Command, String)} for owned commands and
 * {@link #addCloseHook(Closeable)} for owned resources. {@link #onDisable()}
 * unregisters commands and closes resources in reverse registration order;
 * subclasses overriding shutdown must call {@code super.onDisable()}.
 */
public class BasePlugin extends JavaPlugin {
    protected MessageManager messages;
    protected final Logger log;
    private final List<CommandWrapper> commands = new ArrayList<>();
    private final List<Closeable> resources = new ArrayList<>();

    public BasePlugin() {
        log = getSLF4JLogger();
    }

    @Override
    public void onLoad() {
        messages = safeDecode(MessageManager.decoder(), YamlValue.EMPTY, this);
    }

    protected <T> @NotNull T safeDecode(YamlDecoder<T> decoder, String file, Object... ctx) {
        return safeDecode(decoder, ResourceUtil.load(file, this).get(), ctx);
    }

    protected <T> @NotNull T safeDecode(YamlDecoder<T> decoder, YamlValue yaml, Object... ctx) {
        var v = decoder.decode(yaml, ctx);
        var res = v.result();
        if (res == null) throw new IllegalStateException("Failed to decode\n" + v.error());
        if (v.hasError()) log.error("Failed to decode\n{}", v.error());
        return res;
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        commands.forEach(CommandWrapper::close);
        commands.clear();
        List<Closeable> list = new ArrayList<>(resources);
        resources.clear();
        for (int i = list.size() - 1; i >= 0; i--) {
            var v = list.get(i);
            try {
                v.close();
            } catch (Exception e) {
                log.error("Failed to close resource", e);
            }
        }
    }

    protected void addCloseHook(Closeable closeable) {
        resources.add(closeable);
    }

    protected void registerCommand(Command<CommandSender> cmd, @Nullable String permission) {
        CommandWrapper wrapper = new CommandWrapper(cmd, this);
        if (permission != null) wrapper.setPermission(permission);
        commands.add(wrapper);
        wrapper.register();
    }

    public void sendMessage(String key, UUID player, PlaceholderResolver<EventContext> c) {
        var pl = Bukkit.getPlayer(player);
        if (pl != null) sendMessage(key, pl, c);
    }

    public void sendMessage(String key, Player player, PlaceholderResolver<EventContext> c) {
        messages.call(key, messages.newContext().source(player).placeholders(c).build());
    }

    public void sendMessage(String key, UUID player) {
        var pl = Bukkit.getPlayer(player);
        if (pl != null) sendMessage(key, pl);
    }

    public void sendMessage(String key, Player player) {
        messages.call(key, player);
    }
}
