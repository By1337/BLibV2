package dev.by1337.core.command.bcmd;


import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.cmd.CommandReader;
import dev.by1337.cmd.SuggestionsList;
import dev.by1337.core.BCore;
import dev.by1337.core.util.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandWrapper extends BukkitCommand implements Listener {
    private final Command<CommandSender> command;
    private final Set<String> allPossibleNames;
    private final Plugin plugin;
    private final boolean async;

    public CommandWrapper(Command<CommandSender> command, Plugin plugin) {
        super(command.name());
        setAliases(command.aliases().stream().toList());
        this.command = command;
        this.plugin = plugin;
        allPossibleNames = new HashSet<>();
        allPossibleNames.addAll(command.aliases());
        allPossibleNames.add(command.name());
        async = command.allowAsync();

    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        BCore.getBukkitCommandRegister().register(this);
    }


    public void unregister() {
        close();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        try {
            command.execute(sender, String.join(" ", args));
            return true;
        } catch (CommandMsgError e) {
            sender.sendMessage(MiniMessage.deserialize(e.getMessage()));
        }
        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return List.of("none");
    }

    private static final Suggestions EMPTY_SUGGESTIONS = new Suggestions(new StringRange(0, 0), List.of());

    @EventHandler
    @SuppressWarnings("all")
    public void on(AsyncPlayerSendSuggestionsEvent event) {
        if (event.isAsynchronous() && !async) return;
        String input = event.getBuffer();
        CommandReader reader = new CommandReader(input);
        if (reader.hasNext() && reader.next() == '/') {
            String cmd = reader.readString();
            reader.skip();
            if (allPossibleNames.contains(cmd)) {
                if (input.contains(" <=")) { // error msg in input?
                    String real = input.substring(0, input.indexOf(" <="));
                    StringRange r = new StringRange(0, real.length());
                    event.setSuggestions(new Suggestions(new StringRange(0, input.length()), List.of(
                            new Suggestion(r, real)
                    )));
                } else {
                    try {
                        SuggestionsList list = command.suggest(event.getPlayer(), reader);
                        if (list == null || list.count() == 0) {
                            event.setSuggestions(EMPTY_SUGGESTIONS);
                        } else {
                            SuggestionsBuilder builder = new SuggestionsBuilder(input, Math.min(input.length(), list.start()));
                            list.forEach(b -> builder.suggest(b));
                            event.setSuggestions(builder.build());
                        }
                    } catch (CommandMsgError e) {
                        StringRange r = new StringRange(0, input.length());
                        event.setSuggestions(new Suggestions(r, List.of(
                                new Suggestion(r, input + " <= " + e.getMessage())
                        )));
                    }

                }
            }
        }
    }

    public void close() {
        AsyncPlayerSendSuggestionsEvent.getHandlerList().unregister(this);
        BCore.getBukkitCommandRegister().unregister(this);
    }

}