package dev.by1337.edsl.check.impl;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.check.CheckHandler;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexRequirement<C> implements CheckHandler<C> {
    private final boolean not;
    private final String regex;
    private final String input;
    private final Pattern pattern;

    public RegexRequirement(boolean not, String regex, String input) {
        this.not = not;
        this.regex = regex;
        this.input = input;
        pattern = Pattern.compile(regex);
    }

    @Override
    public boolean handle(C ctx, PlaceholderApplier placeholders, Command<C> executor) {
        Matcher m = pattern.matcher(placeholders.setPlaceholders(input));
        return not != m.find();
    }

    public @Nullable CheckHandler<C> compile() {
        if (input.contains("{") || input.contains("%")) return null;
        return handle(null, s -> s, null) ? CheckHandler.nopTrue() : CheckHandler.nopFalse();
    }

    @Override
    public YamlValue encode() {
        return YamlValue.wrap(toString());
    }

    @Override
    public @NotNull String toString() {
        String s = regex + " " + input;
        return not ? "!regex " + s : "regex " + s;
    }
}
