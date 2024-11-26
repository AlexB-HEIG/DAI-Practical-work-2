package ch.heigvd.dai.game;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;


public class GameServer {

    private final int PORT;
    private static final int SERVER_ID = (int) (Math.random() * 1000000);
    private static ConcurrentHashMap<Integer, Socket> clientMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, GameHandler> gamesMap = new ConcurrentHashMap<>();

    private enum ClientCommand {
        LIST,
        JOIN,
        CREATE,
        QUITGAME,
        PLACE
    }

    private enum ServerCommand {
        INIT_GAME,
        GAME_LIST,
        INVALID
    }

    public GameServer(int port) {
        this.PORT = port;
    }

    public void launchServer() {

        try (ServerSocket serverSocket = new ServerSocket(PORT);
             ExecutorService executor = Executors.newCachedThreadPool();) {
            System.out.println("[Server " + SERVER_ID + "] starting with id " + SERVER_ID);
            System.out.println("[Server " + SERVER_ID + "] listening on port " + PORT);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();

                int clientId;
                do {
                    clientId = (int) (Math.random() * 100000);

                } while (clientMap.containsKey(clientId));
                clientMap.put(clientId, clientSocket);

                executor.submit(new ClientHandler(clientSocket, clientId));
            }

        } catch (Exception e) {
            System.out.println("[Server " + SERVER_ID + "] exception: " + e);
        }
    }

    static class ClientHandler implements Runnable {

        private final Socket socket;
        private final int CLIENT_ID;
        private boolean inGame = false;
        private int GAME_ID;

        public ClientHandler(Socket socket, int CLIENT_ID) {
            this.socket = socket;
            this.CLIENT_ID = CLIENT_ID;
        }

        @Override
        public void run() {
            try (socket; // This allow to use try-with-resources with the socket
                 BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter socketOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {


                System.out.println("[Server " + SERVER_ID + "] new client connected from "
                        + socket.getInetAddress().getHostAddress() + ":" + socket.getPort()
                        + "\n    using Client ID " + CLIENT_ID);

                while (!socket.isClosed()) {

                    String clientRequest = socketIn.readLine();

                    if (clientRequest == null) {
                        socket.close();
                        continue;
                    }

                    String response = null;

                    try {


                        if (!inGame) {
                            String[] clientRequestParts = clientRequest.split(" ", 2);
                            ClientCommand clientCommand = ClientCommand.valueOf(clientRequestParts[0]);


                            switch (clientCommand) {
                                case LIST -> {
                                    //TODO: game list logic
                                }


                                case JOIN -> {
                                    if (clientRequestParts.length < 2) {
                                        response = ServerCommand.INVALID + " Missing <game id> parameter. Please try again.";
                                        break;
                                    }
                                    //TODO: join logic
                                    // int gameId = Integer.parseInt(clientRequestParts[1]);
                                    // lauch playgame

                                }


                                case CREATE -> {

                                    if (clientRequestParts.length < 2) {
                                        response = ServerCommand.INVALID + " Missing <grid size> parameter. Please try again.";
                                        break;
                                    }
                                    //TODO: creation logic

                                    int gridSize = Integer.parseInt(clientRequestParts[1]);

                                    //TODO: find better way
                                    if (gridSize != 3 && gridSize != 5 && gridSize != 7 && gridSize != 9) {
                                        response = ServerCommand.INVALID + " Invalid <grid size> parameter. Please try again. " +
                                                "Available grid size : 3, 5, 7, 9";
                                        break;
                                    }


                                    int gameId;
                                    do {
                                        gameId = (int) (Math.random() * 100);

                                    } while (gamesMap.containsKey(gameId));


                                    gamesMap.put(gameId, new GameHandler(gameId, gridSize));

                                    System.out.println("[Server " + SERVER_ID + "] \n"
                                            + "     [Client " + CLIENT_ID + "] new game created with id " + gameId);

                                    GAME_ID = gameId;
                                    inGame = true;

                                }
                            }

                        } else {
                            String[] clientRequestParts = clientRequest.split(" ", 3);
                            ClientCommand clientCommand = ClientCommand.valueOf(clientRequestParts[0]);


                            switch (clientCommand) {
                                case QUITGAME -> {
                                // commande to quit game
                                }


                                case PLACE -> {
                                // PLACE A 1

                                }
                            }
                        }


                    } catch (Exception e) {
                        System.out.println("[Server " + SERVER_ID + "] exception: " + e);
                        response = ServerCommand.INVALID + "Unknown command. Please try again.";
                    }

                    socketOut.write(response + "\n");
                    socketOut.flush();
                }

                System.out.println("[Server " + SERVER_ID + "] closing connection");
            } catch (Exception e) {
                System.out.println("[Server " + SERVER_ID + "] exception: " + e);
            }
        }
    }


    private static class GameHandler {


        private int gameId;
        private int gridSize;

        private int[][] table;

        private boolean turnOf;

        private int player1ID;
        private int player2ID;


        GameHandler(int gameId, int gridSize) {
            this.gameId = gameId;
            this.gridSize = gridSize;
            this.table = new int[gridSize][gridSize];

        }


        void joinGame() {

        }

        void playGame() {

        }

        String getTable() {//TODO: do logic
            return null;
        }
    }
}
