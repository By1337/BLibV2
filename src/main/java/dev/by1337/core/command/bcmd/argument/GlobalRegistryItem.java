package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
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
        var res = GlobalItemRegistry.resolveItemStack(c);
        if (res != null) {
            out.put(name, res);
        }
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String s = reader.readString();
        if (s.isBlank() || !s.contains(":")) {
            for (ItemRegistry<?> value : GlobalItemRegistry.values()) {
                for (ItemModelHolder holder : value) {
                    suggestions.suggest(value.space() + ":" + ((ItemRegistry) value).getId(holder));
                    if (!suggestions.hasFree()) return;
                }
            }
        } else {
            for (ItemRegistry<?> value : GlobalItemRegistry.values()) {
                for (ItemModelHolder holder : value) {
                    var id = ((ItemRegistry) value).getId(holder);
                    if (s.startsWith(id) || s.startsWith(value.space())){
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
