package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import dev.by1337.core.command.bcmd.ArgumentParseError;
import dev.by1337.item.registry.GlobalItemRegistry;
import dev.by1337.item.registry.ItemModelHolder;
import dev.by1337.item.registry.ItemRegistry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class GlobalRegistryItem<C> extends Argument<C, ItemStack> {
    public GlobalRegistryItem(String name) {
        super(name);
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        var c = reader.readString();
        if (c.isEmpty()) return;
        var res = GlobalItemRegistry.resolveItemStack(c);
        if (res != null) {
            out.put(name, res);
        } else {
            throw new ArgumentParseError("unknown-item", name, c, "registered item ID");
        }
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String s = reader.readString();
        if (s.isBlank() || !s.contains(":")) {
            for (ItemRegistry<?> value : GlobalItemRegistry.values()) {
                for (ItemModelHolder holder : value) {
                    var id = ((ItemRegistry) value).getId(holder);
                    if (id.startsWith(s) || value.space().startsWith(s)) {
                        suggestions.suggest(value.space() + ":" + id);
                    }
                    if (!suggestions.hasFree()) return;
                }
            }
        } else {
            for (ItemRegistry<?> value : GlobalItemRegistry.values()) {
                for (ItemModelHolder holder : value) {
                    var id = ((ItemRegistry) value).getId(holder);
                    if ((value.space() + ":" + id).startsWith(s)) {
                        suggestions.suggest(value.space() + ":" + id);
                    }
                    if (!suggestions.hasFree()) return;
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
        return false;
    }
}
