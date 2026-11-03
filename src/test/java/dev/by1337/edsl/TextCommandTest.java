package dev.by1337.edsl;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.CompiledCommand;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.edsl.command.DefaultEventCommands;
import dev.by1337.edsl.command.args.ArgumentRawText;
import dev.by1337.edsl.command.args.ArgumentRawTitle;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.edsl.handle.CommandHandler;
import dev.by1337.edsl.text.RawTextComponentLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TextCommandTest {
    @AfterEach
    void reset() {
        Themes.setSelector(null);
        Themes.setThemeReplacer(null);
    }

    @Test
    void compiledMessageKeepsRawArgumentAndResolvesThemeBeforeCallerEveryTime() {
        AtomicReference<String> theme = new AtomicReference<>("first");
        AtomicReference<String> value = new AtomicReference<>("Alex");
        Themes.setSelector(player -> theme.get());
        Themes.setThemeReplacer(id -> s -> s.replace("{greeting}", id + " {name}"));
        AtomicReference<Component> output = new AtomicReference<>();
        AtomicReference<RawTextComponentLike> argument = new AtomicReference<>();
        Command<EventContext> root = new Command<EventContext>("root")
                .sub(new Command<EventContext>("[message]").executor(new ArgumentRawText<>("text"), (ctx, raw) -> {
                    argument.set(raw);
                    output.set(raw.asComponent(ctx));
                }));
        EventContext context = new EventContext(false) {
            @Override
            public String setPlaceholders(String source) {
                return source.replace("{name}", value.get());
            }
        }.setCommand(root);
        var handler = new CommandHandler("[message] {greeting}");
        handler.handle(context);
        RawTextComponentLike original = argument.get();
        assertEquals(Component.text("first Alex"), output.get());
        value.set("Bob");
        theme.set("second");
        handler.handle(context);
        assertSame(original, argument.get());
        assertEquals(Component.text("second Bob"), output.get());
    }

    @Test
    void ordinaryCommandsStillSubstituteBeforeParsing() {
        AtomicReference<String> output = new AtomicReference<>();
        Command<EventContext> root = new Command<EventContext>("root")
                .sub(new Command<EventContext>("[console]").executor(new ArgumentStrings<>("text"),
                        (ctx, text) -> output.set(text)));
        var context = new EventContext(false) {
            @Override
            public String setPlaceholders(String source) {
                return source.replace("{value}", "resolved");
            }
        }.setCommand(root);
        new CommandHandler("[console] say {value}").handle(context);
        assertEquals("say resolved", output.get());
    }

    @Test
    void allTextAliasesDeferButSimilarCommandNamesDoNot() {
        for (String name : new String[]{ "message", "broadcast", "bc", "message_all", "action_bar", "action_bar_all", "title", "title_all" }) {
            assertTrue(DefaultEventCommands.defersPlaceholders("[" + name + "] {value}"));
            assertTrue(DefaultEventCommands.defersPlaceholders("[" + name.toUpperCase(java.util.Locale.ROOT) + "] {value}"));
        }
        assertFalse(DefaultEventCommands.defersPlaceholders("[message_extra] {value}"));
        assertFalse(DefaultEventCommands.defersPlaceholders("[console] [message] {value}"));
    }

    @Test
    void titleSplitsNewlineIntroducedByPlaceholderWithoutFreezingCallerData() {
        AtomicReference<Title> output = new AtomicReference<>();
        Command<EventContext> root = new Command<EventContext>("root")
                .sub(new Command<EventContext>("[title]").executor(new ArgumentRawTitle<>("text"),
                        (ctx, text) -> output.set(text.render(null, ctx, Title.DEFAULT_TIMES))));
        AtomicReference<String> content = new AtomicReference<>("first\\nsecond");
        var context = new EventContext(false) {
            @Override
            public String setPlaceholders(String source) {
                return source.replace("{content}", content.get());
            }
        }.setCommand(root);
        var handler = new CommandHandler("[title] \"{content}\"");
        handler.handle(context);
        assertEquals(Component.text("first"), output.get().title());
        assertEquals(Component.text("second"), output.get().subtitle());
        content.set("updated\\nsubtitle");
        handler.handle(context);
        assertEquals(Component.text("updated"), output.get().title());
        assertEquals(Component.text("subtitle"), output.get().subtitle());
    }
}
