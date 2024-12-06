package ch.heigvd.dai.game;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;


public class GameClient {

    private final String HOST;
    private final int PORT;
    private static final int CLIENT_ID = (int) (Math.random() * 1000000);

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";

    private static Socket socket;
    private static BufferedReader socketIn;
    private static BufferedWriter socketOut;

    private static boolean inGame = false;

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
        CONFIRMQUITGAME,
        INVALID
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
            System.out.println("[Client " + CLIENT_ID + "] exception: " + e);
        }
    }


    private static class TerminalHandler implements Runnable {
        TerminalHandler() {
        }

        //TODO: fix text not in right place
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

                        if (!inGame) {
                            String[] userInputParts = userInput.split(" ", 2);
                            ClientCommand command = ClientCommand.valueOf(userInputParts[0].toUpperCase());


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
                            }

                        } else {
                            String[] userInputParts = userInput.split(" ", 2);
                            ClientCommand command = ClientCommand.valueOf(userInputParts[0].toUpperCase());


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
                            }
                        }

                        if (request != null) {
                            socketOut.write(request + "\n");
                            socketOut.flush();

                            synchronized (waitResponse) {
                                waitResponse.wait();
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid command. Please try again.");
                        continue;
                    }
                }
            } catch (Exception e) {
                System.out.println("[Client " + CLIENT_ID + "] exception: " + e);
            }
        }
    }


    private static class ServerHandler implements Runnable {
        ServerHandler() {
        }

        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {

                    String serverResponse = socketIn.readLine();//TODO: fix crach from command quit

                    if (serverResponse == null) {
                        System.out.println(ANSI_RED
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
                                System.out.println("Opponent joined.");
                                //TODO: maybe call fonction with while
                            }

                            case GAME_LIST -> {
                                String[] gamelist = serverResponseParts[1].split("¦");
                                for (String s : gamelist) {
                                    System.out.println(s);//TODO: finish
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
                                System.out.println("Waiting for opponent...");
                            }

                            case CONFIRMQUITGAME -> {
                                inGame = false;//TODO: maybe more
                            }

                            case INVALID -> {
                                System.out.println(serverResponseParts[1]);
                            }
                        }

                        synchronized (waitResponse) {
                            waitResponse.notify();
                        }

                    } catch (IllegalArgumentException e) {
                        //TODO: think
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