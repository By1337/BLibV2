package dev.by1337.edsl;

import dev.by1337.cmd.Command;
import dev.by1337.edsl.command.DefaultEventCommands;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.messages.Messages;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MessageManager {
    private final @Nullable Messages messages;
    private boolean debug;
    private Command<EventContext> commands = DefaultEventCommands.createCommands();
    private final LangTranslator translator;

    public MessageManager(@Nullable Messages messages, LangTranslator translator) {
        this.messages = messages;
        this.translator = translator;
        if (translator != null) {
            if (!GlobalTranslator.translator().addSource(translator)) {
                for (Translator source : GlobalTranslator.translator().sources()) {
                    if (source.name().equals(translator.name())) {
                        GlobalTranslator.translator().removeSource(source);
                    }
                }
                GlobalTranslator.translator().addSource(translator);
            }
        }
    }

    public static YamlDecoder<MessageManager> decoder(@Nullable String messagesFile) {
        return decoder(messagesFile, null);
    }

    public static YamlDecoder<MessageManager> decoder(@Nullable String messagesFile, @Nullable String langFile) {
        return RecordYamlDecoder.mapOf(
                MessageManager::new,
                Messages.decoder(messagesFile).fieldOf(),
                LangTranslator.decoder(langFile).fieldOf()
        );
    }

    public void call(String key, Player player, PlaceholderResolver<EventContext> plc) {
        call(key, newContext().placeholders(plc).source(player).build());
    }

    public void call(String key, Player player) {
        call(key, newContext().source(player).build());
    }

    public void call(String key, EventContext ctx) {
        try (var v = messages.handle(key, ctx)) {
        }
    }

    public Builder newContext(Object... args) {
        return new Builder() {
            Player source;
            PlaceholderResolver<EventContext> placeholders;

            @Override
            public Builder source(Player player) {
                source = player;
                return this;
            }

            @Override
            public Builder placeholders(PlaceholderResolver<EventContext> v) {
                placeholders = v;
                return this;
            }

            @Override
            public EventContext build() {
                var ctx = new EventContext(debug, source, placeholders);
                for (Object arg : args) {
                    ctx.with(arg);
                }
                if (messages != null) {
                    ctx.setMessages(messages);
                }
                ctx.setCommand(commands);
                return ctx;
            }
        };
    }

    public Command<EventContext> commands() {
        return commands;
    }

    public void setCommands(Command<EventContext> commands) {
        this.commands = commands;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public LangTranslator translator() {
        return translator;
    }

    public interface Builder {
        Builder source(Player player);

        Builder placeholders(PlaceholderResolver<EventContext> v);

        EventContext build();
    }
}
