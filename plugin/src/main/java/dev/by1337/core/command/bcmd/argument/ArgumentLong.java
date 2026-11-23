package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.command.bcmd.ArgumentParseError;
import dev.by1337.core.util.math.FastExpressionParser;
import org.bukkit.entity.Player;

public class ArgumentLong<C> extends Argument<C, Long> {

    public ArgumentLong(String name) {
        super(name);
    }


    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        if (str.isEmpty()) return;
        try {
            out.put(name, (long) FastExpressionParser.parse(str, FastExpressionParser.Mode.NUMBERS_ONLY));
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseError("invalid-number", name, str, "number or math expression");
        }
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String str = reader.readString();
        if (str.isEmpty()) {
            suggestions.suggest("10");
            return;
        }
        if (str.endsWith("*")) {
            int x = 32;
            if (ctx instanceof Player pl) {
                var item = pl.getInventory().getItemInMainHand();
                x = item == null || item.getAmount() == 0 ? 32 : item.getAmount();
            }
            suggestions.suggest(str = (str + x));
        }
        try {
            long d = (long) FastExpressionParser.parse(str, FastExpressionParser.Mode.NUMBERS_ONLY);
            args.put(name, d);
            suggestions.suggest(Long.toString(d));
        } catch (IllegalArgumentException e) {
            throw new ArgumentParseError("invalid-number", name, str, "number or math expression");
        }
    }

    @Override
    public boolean compilable() {
        return true;
    }

    @Override
    public boolean allowAsync() {
        return true;
    }

}
