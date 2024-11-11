package ch.heigvd.dai.commands;

import picocli.CommandLine;

@CommandLine.Command(
        description = "An online tic tac toe using TCP.",
        version = "1.0.0",
        subcommands = {
                Client.class,
                Server.class,
        },
        scope = CommandLine.ScopeType.INHERIT,
        mixinStandardHelpOptions = true)
public class Root {
}