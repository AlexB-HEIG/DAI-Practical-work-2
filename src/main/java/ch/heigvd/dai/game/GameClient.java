package ch.heigvd.dai.game;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;


public class GameClient {
    private final String HOST;
    private final int PORT;
    private static final int CLIENT_ID = (int) (Math.random() * 1000000);

    // ANSI text format
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BRIGHT_YELLOW = "\u001B[33;1m";
    private static final String ANSI_BRIGHT_BLUE = "\u001B[34;1m";
    private static final String ANSI_BRIGHT_PURPLE = "\u001B[35;1m";
    private static final String ANSI_BRIGHT_CYAN = "\u001B[36;1m";

    private static Socket socket;
    private static BufferedReader socketIn;
    private static BufferedWriter socketOut;

    private static boolean inGame = false;

    private static final AtomicBoolean expectingResponse = new AtomicBoolean(false);

    private static final Object quitLock = new Object();
    private static final Object waitResponse = new Object();

    private enum ClientCommand {
        LIST,
        JOIN,
        CREATE,
        QUIT,
        HELP,
        QUITGAME,
        PLACE
    }

    private enum ServerCommand {
        INIT_GAME,
        GAME_LIST,
        GAME_TABLE,
        WAIT_OPPONENT,
        STANDARD_MESSAGE,
        CONFIRMQUITGAME,
        INVALID,
        FIRSTOFCHAIN,
        LASTOFCHAIN,
        ENDGAME_MESSAGE
    }

    public GameClient(String host, int port) {
        this.HOST = host;
        this.PORT = port;
    }

    public void launchClient() {
        System.out.println("[Client " + CLIENT_ID + "] Starting with id " + CLIENT_ID);
        System.out.println("[Client " + CLIENT_ID + "] Connecting to " + HOST + ":" + PORT);

        try (Socket sock = new Socket(HOST, PORT);
             BufferedReader sockIn = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter sockOut = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));) {

            socket = sock;
            socketIn = sockIn;
            socketOut = sockOut;

            System.out.println("[Client " + CLIENT_ID + "] Connected to " + HOST + ":" + PORT);
            System.out.println();

            Thread serverThread = new Thread(new ServerHandler());
            Thread terminalThread = new Thread(new TerminalHandler());

            serverThread.start();
            terminalThread.start();

            synchronized (quitLock) {
                quitLock.wait();
            }

            System.out.println("[Client " + CLIENT_ID + "] Closing connection and quitting...");

            terminalThread.interrupt();
            serverThread.interrupt();

            socket.close();
            socketIn.close();
            socketOut.close();

        } catch (Exception e) {
            System.out.println(ANSI_RED + "[Client " + CLIENT_ID + "] exception: " + e + ANSI_RESET);
        }
    }


    private static class TerminalHandler implements Runnable {
        TerminalHandler() {
        }

        @Override
        public void run() {
            try {
                help();

                while (!socket.isClosed()) {
                    System.out.print("\n> ");

                    BufferedReader cbir = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                    String userInput = cbir.readLine();

                    try {
                        String request = null;

                        String[] userInputParts = userInput.split(" ", 2);
                        ClientCommand command = ClientCommand.valueOf(userInputParts[0].toUpperCase());

                        if (!inGame) {
                            switch (command) {
                                case LIST -> {
                                    request = ClientCommand.LIST.name();
                                }
                                case JOIN -> {
                                    request = ClientCommand.JOIN.name();
                                    if (userInputParts.length > 1) {
                                        request += " " + userInputParts[1];
                                    }
                                }
                                case CREATE -> {
                                    request = ClientCommand.CREATE.name();
                                    if (userInputParts.length > 1) {
                                        request += " " + userInputParts[1];
                                    }
                                }
                                case QUIT -> {
                                    synchronized (quitLock) {
                                        quitLock.notify();
                                        return;
                                    }
                                }
                                case HELP -> {
                                    help();
                                    continue;
                                }
                                default ->
                                        System.out.println(ANSI_BRIGHT_PURPLE + "Invalid command. Please try again." + ANSI_RESET);
                            }
                        } else {
                            switch (command) {
                                case PLACE -> {
                                    request = ClientCommand.PLACE.name();
                                    if (userInputParts.length > 1) {
                                        request += " " + userInputParts[1];
                                    }
                                }
                                case QUITGAME -> {
                                    request = ClientCommand.QUITGAME.name();
                                }
                                case HELP -> {
                                    helpInGame();
                                    continue;
                                }
                                default ->
                                        System.out.println(ANSI_BRIGHT_PURPLE + "Invalid command. Please try again." + ANSI_RESET);
                            }
                        }

                        if (request != null) {
                            expectingResponse.set(true);

                            socketOut.write(request + "\n");
                            socketOut.flush();

                            synchronized (waitResponse) {
                                waitResponse.wait();
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(ANSI_BRIGHT_PURPLE + "Invalid command. Please try again." + ANSI_RESET);
                    }
                }
            } catch (Exception e) {
                System.out.println(ANSI_RED + "[Client " + CLIENT_ID + "] exception: " + e + ANSI_RESET);
            }
        }
    }


    private static class ServerHandler implements Runnable {
        ServerHandler() {
        }

        @Override
        public void run() {
            boolean commandChain = false;

            try {
                while (!socket.isClosed()) {
                    String serverResponse = socketIn.readLine();

                    if (serverResponse == null) {
                        System.out.println("\n" + ANSI_RED
                                + "[Client " + CLIENT_ID + "] Server unexpectedly closed."
                                + ANSI_RESET);

                        synchronized (quitLock) {
                            quitLock.notify();
                            return;
                        }
                    }

                    try {
                        String[] serverResponseParts = serverResponse.split(" ", 2);
                        ServerCommand serverCommand = ServerCommand.valueOf(serverResponseParts[0]);

                        switch (serverCommand) {
                            case INIT_GAME -> {
                                inGame = true;
                                System.out.println(ANSI_BRIGHT_CYAN + serverResponseParts[1] + "\n" + ANSI_RESET);
                            }
                            case GAME_LIST -> {
                                String[] gameList = serverResponseParts[1].split("¦");
                                for (String s : gameList) {
                                    System.out.println(s);
                                }
                            }
                            case GAME_TABLE -> {
                                String[] gameTable = serverResponseParts[1].split("/");
                                for (String s : gameTable) {
                                    System.out.println(s);
                                }
                            }
                            case WAIT_OPPONENT -> {
                                inGame = true;
                                System.out.println(ANSI_BRIGHT_BLUE + serverResponseParts[1] + ANSI_RESET);
                            }
                            case STANDARD_MESSAGE -> {
                                System.out.println(serverResponseParts[1]);
                            }
                            case CONFIRMQUITGAME -> {
                                inGame = false;
                                System.out.println(ANSI_BRIGHT_YELLOW + serverResponseParts[1] + ANSI_RESET);
                            }
                            case INVALID -> {
                                System.out.println(ANSI_BRIGHT_PURPLE + serverResponseParts[1] + ANSI_RESET);
                            }
                            case FIRSTOFCHAIN -> {
                                if (!expectingResponse.get()) {
                                    System.out.println(" ");
                                }
                                commandChain = true;
                            }
                            case LASTOFCHAIN -> {
                                if (!expectingResponse.get()) {
                                    System.out.print("\n> ");
                                }
                                commandChain = false;
                            }
                            case ENDGAME_MESSAGE -> {
                                String[] endmessage = serverResponseParts[1].split("n");
                                for (String s : endmessage) {
                                    System.out.println(s);
                                }
                            }
                        }

                        if (!commandChain) {
                            expectingResponse.set(false);

                            synchronized (waitResponse) {
                                waitResponse.notify();
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        //TODO: maybe
                    }
                }
            } catch (SocketException e) {
                return;
            } catch (Exception e) {
                System.out.println("[Client " + CLIENT_ID + "] exception: " + e);
            }
        }
    }

    private static void help() {
        System.out.println("Usage:");
        System.out.println(" " + ClientCommand.LIST + " - Display the list of available games.");
        System.out.println(" " + ClientCommand.JOIN + " <game id> - Join the game with the given id.");
        System.out.println(" " + ClientCommand.CREATE + " <grid size> - Create a new game with the given grid size.");
        System.out.println(" " + ClientCommand.QUIT + " - Close the connection to the server.");
        System.out.println(" " + ClientCommand.HELP + " - Display this help message.");
    }

    private static void helpInGame() {
        System.out.println("Usage:");
        System.out.println(" " + ClientCommand.PLACE + " <row> <column> - Place a marker at the specified position.");
        System.out.println(" " + ClientCommand.QUITGAME + " - Quit the current game.");
        System.out.println(" " + ClientCommand.HELP + " - Display this help message.");
    }
}
