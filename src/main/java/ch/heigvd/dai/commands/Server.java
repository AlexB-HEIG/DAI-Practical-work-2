package ch.heigvd.dai.commands;

import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CommandLine.Command(name = "server", description = "Start the server part of the network game.")
public class Server implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "6433")
    protected int port;

    /*
    // Definition of the parameter for board size
    @CommandLine.Option(
            names = {"-s", "--size"},
            description = "The size of the board wanted (max 9).",
            defaultValue = "3")
    protected int BoardSize;

    public int getBoardSize() {
        return BoardSize;
    }

     */

    @Override
    public Integer call() throws IOException {

        try (ServerSocket serverSocket = new ServerSocket(port);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();) {
            System.out.println("Server started on port " + port);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e);
            return -1;
        }
        return 0; // Success
    }
}
