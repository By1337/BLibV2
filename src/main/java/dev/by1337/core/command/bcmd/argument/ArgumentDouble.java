package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.util.math.FastExpressionParser;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class ArgumentDouble<C> extends Argument<C, Double> {
    private static final DecimalFormat df = new DecimalFormat("#.##");

    public ArgumentDouble(String name) {
        super(name);
    }

    public static Double getOrThrow(String key, ArgumentMap map, String error){
        Object o = map.get(key);
        if (o instanceof Double) {
            return (Double) o;
        }
        throw new CommandMsgError(error);
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        try {
            out.put(name, FastExpressionParser.parse(str));
        } catch (FastExpressionParser.MathFormatException e) {
            throw new CommandMsgError("must be a number");
        }
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String str = reader.readString();
        if (str.endsWith("*")){
            int x = 32;
            if (ctx instanceof Player pl) {
                var item = pl.getInventory().getItemInMainHand();
                x = item == null && item.getAmount() != 0 ? 32 : item.getAmount();
            }
            suggestions.suggest(str = (str + x));
        }
        try {
            double d = FastExpressionParser.parse(str);
            args.put(name, d);
            suggestions.suggest(df.format(d));
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
