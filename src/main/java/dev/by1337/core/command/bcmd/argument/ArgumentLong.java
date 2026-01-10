package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.util.math.FastExpressionParser;
import org.bukkit.entity.Player;

public class ArgumentLong<C> extends Argument<C, Long> {

    public ArgumentLong(String name) {
        super(name);
    }


    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        try {
            out.put(name, (long) FastExpressionParser.parse(str));
        } catch (FastExpressionParser.MathFormatException e) {
            throw new CommandMsgError("must be a number");
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
                x = item == null && item.getAmount() != 0 ? 32 : item.getAmount();
            }
            suggestions.suggest(str = (str + x));
        }
        try {
            long d = (long) FastExpressionParser.parse(str);
            args.put(name, d);
            suggestions.suggest(Long.toString(d));
        } catch (FastExpressionParser.MathFormatException e) {
            throw new CommandMsgError("must be a number");
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
