package ch.heigvd.dai.commands;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.*;

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
    public Integer call() {
        throw new UnsupportedOperationException(
                "Please remove this exception and implement this method.");
    }
}
