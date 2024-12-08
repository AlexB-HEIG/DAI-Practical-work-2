package ch.heigvd.dai.commands;

import java.io.IOException;
import java.util.concurrent.Callable;

import ch.heigvd.dai.game.GameServer;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is the Server sub command
 * It implements the standard command and definition of its options, parameters and subcommands.
 *
 * @author Alex Berberat
 * @author Lisa Gorgerat
 */
@CommandLine.Command(name = "server", description = "Start the server part of the network game.")
public class Server implements Callable<Integer> {

    // Definition of the option for the port
    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "6433")
    protected int port;

    // Function to launch the server with the value obtained from the option
    @Override
    public Integer call() throws IOException {
        GameServer server = new GameServer(port);
        server.launchServer();
        return 0; // Success
    }
}
