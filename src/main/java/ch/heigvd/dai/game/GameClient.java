package ch.heigvd.dai.game;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class GameClient {

    private final String HOST;
    private final int PORT;
    private static final int CLIENT_ID = (int) (Math.random() * 1000000);

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";

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
        INVALID
    }

    public GameClient(String host, int port) {
        this.HOST = host;
        this.PORT = port;
    }

    public void launchClient() {
        System.out.println("[Client " + CLIENT_ID + "] Starting with id " + CLIENT_ID);
        System.out.println("[Client " + CLIENT_ID + "] Connecting to " + HOST + ":" + PORT);


        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter socketOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));) {

            System.out.println("[Client " + CLIENT_ID + "] Connected to " + HOST + ":" + PORT);
            System.out.println();

            help();

            while (!socket.isClosed()) {
                System.out.print("\n> ");

                BufferedReader cbir = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                String userInput = cbir.readLine();

                try {

                    String[] userInputParts = userInput.split(" ", 2);
                    ClientCommand command = ClientCommand.valueOf(userInputParts[0].toUpperCase());

                    String request = null;

                    switch (command) {
                        case LIST -> {
                            request = ClientCommand.LIST.name();
                        }
                        case JOIN -> {
                            request = ClientCommand.JOIN.name();
                        }
                        case CREATE -> {
                            //TODO: fix if one argument launch exept
                            request = ClientCommand.CREATE + " " + userInputParts[1];
                        }
                        case QUIT -> {
                            socket.close();
                            continue;
                        }
                        case HELP -> {
                            help();
                            continue;
                        }
                    }

                    if (request != null) {
                        socketOut.write(request + "\n");
                        socketOut.flush();
                    }


                } catch (Exception e) {
                    System.out.println("Invalid command. Please try again.");
                    continue;
                }


                String serverResponse = socketIn.readLine();

                if (serverResponse == null) {
                    System.out.println(ANSI_RED
                            + "[Client " + CLIENT_ID + "] Server unexpectedly closed."
                            + ANSI_RESET);
                    socket.close();
                    continue;
                }


                try {
                    String[] serverResponseParts = serverResponse.split(" ", 2);
                    ServerCommand serverCommand = ServerCommand.valueOf(serverResponseParts[0]);

                    switch (serverCommand) {
                        case INIT_GAME -> {
                            //TODO: maybe call fonction with while
                        }
                        case GAME_LIST -> {
                            String[] gamelist = serverResponseParts[1].split("/");
                            for (String s : gamelist) {
                                System.out.println(s);//TODO: finish
                            }
                        }

                        case INVALID -> {
                            System.out.println(serverResponseParts[1]);
                        }
                    }


                } catch (IllegalArgumentException e) {
                    //TODO: think
                }


            }


            System.out.println("[Client " + CLIENT_ID + "] Closing connection and quitting...");
        } catch (Exception e) {
            System.out.println("[Client " + CLIENT_ID + "] exception: " + e);
        }
    }


    private static void inGame(){


    }

    private static void help() {
        System.out.println("Usage:");
        System.out.println(" " + ClientCommand.LIST + " - Not currently implemented.");  //TODO: if time
        System.out.println(" " + ClientCommand.JOIN + " - Not currently implemented.");  //TODO: if time
        System.out.println(" " + ClientCommand.CREATE + " <grid size> - Create a new game with the given grid size.");
        System.out.println(" " + ClientCommand.QUIT + " - Close the connection to the server.");
        System.out.println(" " + ClientCommand.HELP + " - Display this help message.");
    }
}
