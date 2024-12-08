package ch.heigvd.dai.commands;

import java.util.concurrent.Callable;
import java.io. *;
import java.net.*;

import ch.heigvd.dai.game.GameClient;
import picocli.CommandLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is the Client sub command
 * It implements the standard command and definition of its options, parameters and subcommands.
 *
 * @author Alex Berberat
 * @author Lisa Gorgerat
 */
@CommandLine.Command(name = "client", description = "Start the client part of the network game.")
public class Client implements Callable<Integer> {

    // Definition of the option for the host
    @CommandLine.Option(
            names = {"-H", "--host"},
            description = "Host to connect to.",
            required = true)
    protected String host;

    // Definition of the option for the port
    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "6433")
    protected int port;

    // Function to launch the client with the values obtained from the options
    @Override
    public Integer call() {
        GameClient client = new GameClient(host, port);
        client.launchClient();
        return 0;
    }
}
