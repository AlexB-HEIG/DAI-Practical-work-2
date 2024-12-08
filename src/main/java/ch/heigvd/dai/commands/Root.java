package ch.heigvd.dai.commands;

import picocli.CommandLine;

/**
 * This class is the root of the CLI.
 * It implements the standard command and definition of its options, parameters and subcommands.
 *
 * @author Alex Berberat
 * @author Lisa Gorgerat
 */
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