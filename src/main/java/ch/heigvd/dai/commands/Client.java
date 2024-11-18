package ch.heigvd.dai.commands;

import java.util.concurrent.Callable;
import java.io. *;
import java.net.*;
import picocli.CommandLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CommandLine.Command(name = "client", description = "Start the client part of the network game.")
public class Client implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-H", "--host"},
            description = "Host to connect to.",
            required = true)
    protected String host;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "6433")
    protected int port;

    @Override
    public Integer call() {
        GameClient client = new GameClient(host, port);
        client.launchClient();
        return 0;
    }
}
