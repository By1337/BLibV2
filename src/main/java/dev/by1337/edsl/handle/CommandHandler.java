package dev.by1337.edsl.handle;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.CompiledCommand;
import dev.by1337.edsl.EventHandler;
import dev.by1337.plc.PlaceholderApplier;
import dev.by1337.yaml.YamlValue;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler<C> implements EventHandler<C> {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);
    private final String source;
    private @Nullable CompiledCommand<C> compiled;
    private @Nullable Command<C> lastExecutor;
    private boolean canBeCompiled;
    private final boolean hasPlaceholders;

    public CommandHandler(String source) {
        this.source = source;
        hasPlaceholders = source.contains("%") || source.contains("{");
        canBeCompiled = !hasPlaceholders;
    }

    @Override
    public boolean handle(C ctx, PlaceholderApplier placeholders, Command<C> executor) {
        if (compiled != null && executor == lastExecutor) {
            compiled.execute(ctx);
        } else {
            if (canBeCompiled) {
                canBeCompiled = false;
                try {
                    compiled = executor.compile(source);
                    lastExecutor = executor;
                } catch (Exception ignored) {
                }
                return handle(ctx, placeholders, executor);
            } else {
                try {
                    if (hasPlaceholders) {
                        executor.execute(ctx, placeholders.setPlaceholders(source));
                    } else {
                        executor.execute(ctx, source);
                    }
                } catch (Exception e) {
                    log.error("Failed to run command {}. post-placeholders: {}", source, placeholders.setPlaceholders(source), e);
                }
            }
        }
        return true;
    }

    @Override
    public YamlValue encode() {
        return YamlValue.wrap(source);
    }
}
