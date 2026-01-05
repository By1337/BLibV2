package dev.by1337.core.command.bcmd.requires;

import dev.by1337.cmd.Requires;
import org.bukkit.command.CommandSender;

public class RequiresPermission<T extends CommandSender> implements Requires<T> {
    private final String permission;
    public RequiresPermission(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean test(T ctx) {
        return ctx.hasPermission(permission);
    }
}
