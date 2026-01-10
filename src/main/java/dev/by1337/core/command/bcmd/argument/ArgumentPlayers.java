package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ArgumentPlayers<C> extends Argument<C, List<Player>> {

    private final boolean single;

    public ArgumentPlayers(String name) {
        super(name);
        single = false;
    }
    public ArgumentPlayers(String name, boolean single) {
        super(name);
        this.single = single;
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        if ((str.equals("-all") || str.equals("*")) && !single) {
            out.put(name, Bukkit.getOnlinePlayers());
            return;
        }

        if (str.equals("-me") && (ctx instanceof Player pl)) {
            out.put(name, List.of(pl));
            return;
        }
        var pl = Bukkit.getPlayerExact(str);
        if (pl == null) {
            if (str.length() == 36) {
                try {
                    pl = Bukkit.getPlayer(UUID.fromString(str));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (pl == null)
                throw new CommandMsgError("Unknown player: " + str);
        }
        out.put(name, List.of(pl));
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String str = reader.readString().toLowerCase();
        if (!single){
            suggestions.suggest("-all");
            suggestions.suggest("*");
        }
        if (ctx instanceof Player) {
            suggestions.suggest("-me");
        }
        if ((str.equals("-all") || str.equals("*")) && !single) {
            args.put(name, Bukkit.getOnlinePlayers());
            return;
        }
        var pl = Bukkit.getPlayerExact(str);
        if (pl != null) {
            suggestions.suggest(pl.getName());
            args.put(name, List.of(pl));
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(str)) {
                    suggestions.suggest(player.getName());
                }
                if (!suggestions.hasFree()) {
                    break;
                }
            }
        }
    }

    @Override
    public boolean compilable() {
        return false;
    }

    @Override
    public boolean allowAsync() {
        return true;
    }
}
