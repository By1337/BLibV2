package dev.by1337.edsl.handle;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.CompiledCommand;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.EventHandler;
import dev.by1337.yaml.YamlValue;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler<C extends EventContext<C>> implements EventHandler<C> {
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
    public boolean handle(C ctx) {
        Command<C> executor = ctx.command();
        if (compiled != null && executor == lastExecutor) {
            try (var ignored = ctx.tracer().timing("run: %s", source)){
                compiled.execute(ctx);
            }
        } else {
            if (canBeCompiled) {
                canBeCompiled = false;
                try {
                    compiled = executor.compile(source);
                    lastExecutor = executor;
                } catch (Exception ignored) {
                }
                return handle(ctx);
            } else {
                try (var ignored = ctx.tracer().timing("run: %s", source)){
                    if (hasPlaceholders) {
                        executor.execute(ctx, ctx.setPlaceholders(source));
                    } else {
                        executor.execute(ctx, source);
                    }
                } catch (Exception e) {
                    log.error("Failed to run command {}. post-placeholders: {}", source, ctx.setPlaceholders(source), e);
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
