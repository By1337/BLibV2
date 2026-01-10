package dev.by1337.core.command.bcmd;

import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.core.command.bcmd.argument.*;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public class TestCommand {

    public static Command<CommandSender> createTest(String name) {
        return new Command<CommandSender>(name)
                .sub(new Command<CommandSender>("string")
                        .argument(new ArgumentString<>("s"))
                        .argument(new ArgumentString<>("s1"))
                        .executor((v, a) -> v.sendMessage(a.toString()))
                )
                .sub(new Command<CommandSender>("strings")
                        .argument(new ArgumentStrings<>("s"))
                        .executor((v, a) -> v.sendMessage(a.toString()))
                )
                .sub(new Command<CommandSender>("double")
                        .argument(new ArgumentDouble<>("s"))
                        .argument(new ArgumentDouble<>("s1"))
                        .executor((v, a) -> v.sendMessage(a.toString()))
                )
                .sub(new Command<CommandSender>("int")
                        .argument(new ArgumentInt<>("s"))
                        .argument(new ArgumentInt<>("s1"))
                        .executor((v, a) -> v.sendMessage(a.toString()))
                )
                .sub(new Command<CommandSender>("long")
                        .argument(new ArgumentLong<>("s"))
                        .argument(new ArgumentLong<>("s1"))
                        .executor((v, a) -> v.sendMessage(a.toString()))
                )
                .sub(new Command<CommandSender>("players")
                        .argument(new ArgumentPlayers<>("s"))
                        .argument(new ArgumentPlayers<>("s1"))
                        .executor((v, a) -> v.sendMessage(a.toString()))
                )
                .sub(new Command<CommandSender>("player")
                        .argument(new ArgumentPlayers<>("s", true))
                        .argument(new ArgumentPlayers<>("s1", true))
                        .executor((v, a) -> v.sendMessage(a.toString()))
                )
                .sub(new Command<CommandSender>("bool")
                        .argument(new ArgumentBool<>("s"))
                        .argument(new ArgumentBool<>("s1"))
                        .executor((v, a) -> v.sendMessage(a.toString()))
                )
                .sub(new Command<CommandSender>("choice")
                        .argument(new ArgumentChoice<>("s", List.of("item1", "item2")))
                        .argument(new ArgumentChoice<>("s1", List.of("item3", "item4")))
                        .executor((v, a) -> v.sendMessage(a.toString()))
                )
                ;
    }
}
