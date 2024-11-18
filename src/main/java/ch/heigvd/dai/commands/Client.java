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
        try (Socket socket = new Socket(host, port);) {
            System.out.println("Client connected to " + host + ":" + port);
        } catch (IOException e) {
            System.out.println("Client exception: " + e);
        }
        return 0;
    }
}
