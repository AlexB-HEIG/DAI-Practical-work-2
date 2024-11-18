package ch.heigvd.dai.game;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class GameClient {

    private final String HOST;
    private final int PORT;
    private static final int CLIENT_ID = (int) (Math.random() * 1000000);

    private enum ClientCommand {
        LIST,
        JOIN,
        CREATE,
        QUIT,
        HELP
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
                System.out.print("> ");

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
                            request = ClientCommand.CREATE.name();
                        }
                        case QUIT -> {
                            socket.close();
                            continue;
                        }
                        case HELP -> {
                            help();
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
                    socket.close();
                    continue;
                }

                
                System.out.println(serverResponse);



            }


            System.out.println("[Client " + CLIENT_ID + "] Closing connection and quitting...");
        } catch (Exception e) {
            System.out.println("[Client " + CLIENT_ID + "] exception: " + e);
        }


    }


    private static void help() {
        System.out.println("Usage:");
        System.out.println(" " + ClientCommand.LIST + " - Not currently implemented.");  //TODO: if time
        System.out.println(" " + ClientCommand.JOIN + " - Not currently implemented.");  //TODO: if time
        System.out.println(" " + ClientCommand.CREATE + " - Not currently implemented.");//TODO: if time
        System.out.println(" " + ClientCommand.QUIT + " - Close the connection to the server.");
        System.out.println(" " + ClientCommand.HELP + " - Display this help message.");
    }
}
