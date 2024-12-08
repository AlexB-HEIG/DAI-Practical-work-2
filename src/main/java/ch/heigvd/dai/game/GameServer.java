package ch.heigvd.dai.game;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * The GameServer class represents a multi-player game server that listens for incoming connections
 * and controls communication with clients using sockets. Clients can create, join games, place tiles
 * and quit games. The server controls game logic and communication with multiple clients simultanously.
 *
 * @author Alex Berberat
 * @author Lisa Gorgerat
 */
public class GameServer {
    private final int PORT;
    private static final int SERVER_ID = (int) (Math.random() * 1000000);
    private static final ConcurrentHashMap<Integer, Socket> clientMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, BufferedWriter> clientWriter = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, GameHandler> gamesMap = new ConcurrentHashMap<>();

    /**
     * ANSI codes for formatting console text output.
     */
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_BRIGHT_RED = "\u001B[31;1m";
    private static final String ANSI_BRIGHT_GREEN = "\u001B[32;1m";
    private static final String ANSI_BRIGHT_YELLOW = "\u001B[33;1m";
    private static final String ANSI_BRIGHT_BLUE = "\u001B[34;1m";
    private static final String ANSI_BRIGHT_PURPLE = "\u001B[35;1m";
    private static final String ANSI_BRIGHT_CYAN = "\u001B[36;1m";
    private static final String ANSI_BLINK = "\u001B[5m";


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
        GAME_TABLE,
        WAIT_OPPONENT,
        STANDARD_MESSAGE,
        CONFIRMQUITGAME,
        INVALID,
        FIRSTOFCHAIN,
        LASTOFCHAIN,
        ENDGAME_MESSAGE
    }

    /**
     * Instantiates a new Game server with the specified port.
     *
     * @param port the port on which the server will listen for incoming connections
     */
    public GameServer(int port) {
        this.PORT = port;
    }

    /**
     * Starts the game server, listens for client connections, and controls them with different threads.
     * The server accepts new clients and assigns them to a ClientHandler.
     */
    public void launchServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println("[Server " + SERVER_ID + "] starting with id " + SERVER_ID);
            System.out.println("[Server " + SERVER_ID + "] listening on port " + PORT);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();

                int clientId;
                // Ensure that the client ID is unique
                do {
                    clientId = (int) (Math.random() * 100000);
                } while (clientMap.containsKey(clientId));
                clientMap.put(clientId, clientSocket);

                executor.submit(new ClientHandler(clientSocket, clientId));
            }
        } catch (Exception e) {
            System.out.println(ANSI_RED + "[Server " + SERVER_ID + "] exception: " + e + ANSI_RESET);
        }
    }

    /**
     * The ClientHandler class manages individual client connections,
     * handling client commands and interacting with the game logic.
     */
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int CLIENT_ID;
        private boolean inGame = false;
        private int GAME_ID;

        /**
         * Instantiates a new Client handler for a specific client.
         *
         * @param socket    the socket associated with the client
         * @param CLIENT_ID the unique ID of the client
         */
        public ClientHandler(Socket socket, int CLIENT_ID) {
            this.socket = socket;
            this.CLIENT_ID = CLIENT_ID;
        }

        @Override
        public void run() {
            try (socket; // This allows to use try-with-resources with the socket
                 BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter socketOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                clientWriter.put(CLIENT_ID, socketOut);

                System.out.println(ANSI_BLUE + "[Server " + SERVER_ID + "] \n"
                        + "       [Client " + CLIENT_ID + "] new connection from "
                        + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ANSI_RESET);

                // Main loop to handle incoming requests from the client
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
                                    System.out.println(ANSI_PURPLE + "[Server " + SERVER_ID + "] \n"
                                            + "       [Client " + CLIENT_ID + "] request game list" + ANSI_RESET);

                                    StringBuilder gameList = new StringBuilder();
                                    gameList.append(ServerCommand.GAME_LIST).append(" Game id    Grid Size ¦");

                                    for (Integer key : gamesMap.keySet()) {
                                        if (gamesMap.get(key).gameIsJoinable()) {
                                            gameList.append(String.format("%7d    %d ¦", key, gamesMap.get(key).getGridSize()));
                                        }
                                    }
                                    response = gameList.toString();
                                }
                                case JOIN -> {
                                    if (clientRequestParts.length < 2 || !isNumeric(clientRequestParts[1])) {
                                        response = ServerCommand.INVALID + " Missing <game id> parameter. Please try again.";
                                        break;
                                    }

                                    int gameId = Integer.parseInt(clientRequestParts[1]);
                                    if (gamesMap.containsKey(gameId) && gamesMap.get(gameId).gameIsJoinable()) {
                                        GAME_ID = gameId;
                                        inGame = true;
                                        gamesMap.get(GAME_ID).joinGame(CLIENT_ID);

                                        System.out.println(ANSI_CYAN + "[Server " + SERVER_ID + "] \n"
                                                + "       [Client " + CLIENT_ID + "] join [Game " + gameId + "]" + ANSI_RESET);

                                        sendToSocket(CLIENT_ID, ServerCommand.FIRSTOFCHAIN.name());
                                        sendToSocket(CLIENT_ID, ServerCommand.INIT_GAME + " Opponent joined.");
                                        sendToSocket(CLIENT_ID, ServerCommand.STANDARD_MESSAGE + " Opponent start, you play as [O].");
                                        sendToSocket(CLIENT_ID, ServerCommand.GAME_TABLE + " " + gamesMap.get(GAME_ID).getTable());
                                        sendToSocket(CLIENT_ID, ServerCommand.LASTOFCHAIN.name());

                                        sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.FIRSTOFCHAIN.name());
                                        sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.INIT_GAME + " Opponent joined.");
                                        sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.STANDARD_MESSAGE + " You start, playing as [X].");
                                        sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.GAME_TABLE + " " + gamesMap.get(GAME_ID).getTable());
                                        sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.LASTOFCHAIN.name());

                                    } else {
                                        response = ServerCommand.INVALID + " Game " + gameId + " doesn't exist. Please try again.";
                                    }
                                }
                                case CREATE -> {
                                    if (clientRequestParts.length < 2 || !isNumeric(clientRequestParts[1])) {
                                        response = ServerCommand.INVALID + " Missing <grid size> parameter. Please try again.";
                                        break;
                                    }

                                    int gridSize = Integer.parseInt(clientRequestParts[1]);

                                    // Ensure grid size is valid (3, 5, 7, 9)
                                    if (gridSize != 3 && gridSize != 5 && gridSize != 7 && gridSize != 9) {
                                        response = ServerCommand.INVALID + " Invalid <grid size> parameter. Please try again. " +
                                                "Available grid size : 3, 5, 7, 9";
                                        break;
                                    }

                                    int gameId;
                                    // Ensure that the gameID is unique
                                    do {
                                        gameId = (int) (Math.random() * 100);
                                    } while (gamesMap.containsKey(gameId));

                                    gamesMap.put(gameId, new GameHandler(gridSize, CLIENT_ID));

                                    System.out.println(ANSI_GREEN + "[Server " + SERVER_ID + "] \n"
                                            + "       [Client " + CLIENT_ID + "] created [Game " + gameId + "]" + ANSI_RESET);

                                    GAME_ID = gameId;
                                    inGame = true;

                                    response = ServerCommand.WAIT_OPPONENT + " Waiting for opponent...";
                                }
                            }
                        } else {
                            String[] clientRequestParts = clientRequest.split(" ", 4);
                            ClientCommand clientCommand = ClientCommand.valueOf(clientRequestParts[0]);

                            switch (clientCommand) {
                                case QUITGAME -> {
                                    System.out.println(ANSI_YELLOW + "[Server " + SERVER_ID + "] \n"
                                            + "       [Client " + CLIENT_ID + "] left [Game " + GAME_ID + "]" + ANSI_RESET);

                                    int tmp = gamesMap.get(GAME_ID).quitGame(CLIENT_ID);

                                    if (tmp != 0) {
                                        sendToSocket(tmp, ServerCommand.FIRSTOFCHAIN.name());
                                        sendToSocket(tmp, ServerCommand.STANDARD_MESSAGE + " Your opponent has left the game.");
                                        if (!gamesMap.get(GAME_ID).gameStatus()) {
                                            sendToSocket(tmp, ServerCommand.STANDARD_MESSAGE + " " + ANSI_BRIGHT_GREEN + "You win by forfeit." + ANSI_RESET);
                                        }
                                        sendToSocket(tmp, ServerCommand.LASTOFCHAIN.name());

                                    } else {
                                        gamesMap.remove(GAME_ID);
                                    }

                                    GAME_ID = 0;
                                    inGame = false;

                                    response = ServerCommand.CONFIRMQUITGAME + " Game quited.";
                                }
                                case PLACE -> {
                                    if (clientRequestParts.length != 3 || clientRequestParts[1].length() > 1 || isNumeric(clientRequestParts[1]) || !isNumeric(clientRequestParts[2])) {
                                        response = ServerCommand.INVALID + " Wrong format. Please try again. Example : PLACE A 1";

                                    } else {
                                        char rows = clientRequestParts[1].toUpperCase().charAt(0);
                                        int cols = Integer.parseInt(clientRequestParts[2]);

                                        if ((int) rows < 65 || (int) rows > 90 || cols < 1) {
                                            response = ServerCommand.INVALID + " Wrong placement. Please try again.";
                                        } else {
                                            // Handle the different outcomes from placing a piece.
                                            switch (gamesMap.get(GAME_ID).placePiece(rows, cols, CLIENT_ID)) {
                                                case -1 -> {
                                                    response = ServerCommand.INVALID + " Please wait your turn to play.";
                                                }
                                                case -2 -> {
                                                    response = ServerCommand.INVALID + " Wrong placement, outside the grid.";
                                                }
                                                case -3 -> {
                                                    response = ServerCommand.INVALID + " Wrong placement, case already played.";
                                                }
                                                case -4 -> {
                                                    response = ServerCommand.INVALID + " Please wait for opponent.";
                                                }
                                                case -5 -> {
                                                    response = ServerCommand.INVALID + " Game is finished, please quit the game.";
                                                }
                                                case 1 -> {
                                                    // Handle a win condition
                                                    sendToSocket(CLIENT_ID, ServerCommand.FIRSTOFCHAIN.name());
                                                    sendToSocket(CLIENT_ID, ServerCommand.GAME_TABLE + " " + gamesMap.get(GAME_ID).getTable());
                                                    sendToSocket(CLIENT_ID, ServerCommand.ENDGAME_MESSAGE + EndGameMessage.GAME_WON);
                                                    sendToSocket(CLIENT_ID, ServerCommand.LASTOFCHAIN.name());

                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.FIRSTOFCHAIN.name());
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.STANDARD_MESSAGE + " Opponent placed at " + rows + " " + clientRequestParts[2]);
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.GAME_TABLE + " " + gamesMap.get(GAME_ID).getTable());
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.ENDGAME_MESSAGE + EndGameMessage.GAME_LOST);
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.LASTOFCHAIN.name());
                                                }
                                                case 2 -> {
                                                    // Handle a draw condition
                                                    sendToSocket(CLIENT_ID, ServerCommand.FIRSTOFCHAIN.name());
                                                    sendToSocket(CLIENT_ID, ServerCommand.GAME_TABLE + " " + gamesMap.get(GAME_ID).getTable());
                                                    sendToSocket(CLIENT_ID, ServerCommand.ENDGAME_MESSAGE + EndGameMessage.GAME_DRAW);
                                                    sendToSocket(CLIENT_ID, ServerCommand.LASTOFCHAIN.name());

                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.FIRSTOFCHAIN.name());
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.STANDARD_MESSAGE + " Opponent placed at " + rows + " " + clientRequestParts[2]);
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.GAME_TABLE + " " + gamesMap.get(GAME_ID).getTable());
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.ENDGAME_MESSAGE + EndGameMessage.GAME_DRAW);
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.LASTOFCHAIN.name());
                                                }
                                                default -> {
                                                    // Continue if no win nor draw
                                                    response = ServerCommand.GAME_TABLE + " " + gamesMap.get(GAME_ID).getTable();

                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.FIRSTOFCHAIN.name());
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.STANDARD_MESSAGE + " Opponent placed at " + rows + " " + clientRequestParts[2]);
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.GAME_TABLE + " " + gamesMap.get(GAME_ID).getTable());
                                                    sendToSocket(gamesMap.get(GAME_ID).getOpponentID(CLIENT_ID), ServerCommand.LASTOFCHAIN.name());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(ANSI_RED + "[Server " + SERVER_ID + "] exception: " + e + ANSI_RESET);
                        response = ServerCommand.INVALID + "Unknown command. Please try again.";
                    }

                    sendToSocket(CLIENT_ID, response);
                }

                System.out.println("[Server " + SERVER_ID + "] \n "
                        + "       [Client " + CLIENT_ID + "] closing connection");

                // Erase client data after the connection is closed.
                clientMap.remove(CLIENT_ID);
                clientWriter.remove(CLIENT_ID);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "[Server " + SERVER_ID + "] exception: " + e + ANSI_RESET);
            }
        }


        /**
         * Sends the message to the given client.
         *
         * This method writes to the client's output stream.
         * To make sure that the message is sent immediately the flush() command is used.
         *
         * @param clientId The ID of the client to send to.
         * @param message  The message to send to the client.
         */
        private void sendToSocket(int clientId, String message) {
            try {
                if (message != null) {
                    clientWriter.get(clientId).write(message + "\n");
                    clientWriter.get(clientId).flush();
                }
            } catch (Exception e) {
                System.out.println(ANSI_RED + "[Server " + SERVER_ID + "] exception: " + e + ANSI_RESET);
            }
        }
    }

    /**
     * Controls the logic and state of one game.
     */
    private static class GameHandler {
        private int gridSize;

        private final int[][] table;

        private int player1ID;
        private int player2ID;

        private int tilePlayed;

        private boolean turnOf;
        private boolean isFinished;
        private boolean isJoinable;


        /**
         * Constructor to initialize the game with the given grid size and player1ID.
         *
         * @param gridSize The size of the grid (example : 3 for a 3x3 grid).
         * @param playerID The ID of the player1.
         */
        GameHandler(int gridSize, int playerID) {
            tilePlayed = 0;
            this.gridSize = gridSize;
            this.table = new int[gridSize][gridSize];
            this.player1ID = playerID;
            isJoinable = true;
        }

        /**
         * Joins player2 to the game and sets the game as non-joinable.
         *
         * @param playerID The ID of the player2.
         */
        void joinGame(int playerID) {
            player2ID = playerID;
            isJoinable = false;
        }

        /**
         * Indicates the status of the game, whether it is finished or not.
         *
         * @return True if the game is finished, false otherwise.
         */
        boolean gameStatus() {
            return isFinished;
        }

        /**
         * Indicates whether the game can still be joined by a second player or not.
         *
         * @return True if the game is still joinable, false otherwise.
         */
        boolean gameIsJoinable() {
            return isJoinable;
        }

        /**
         * Allows a player to quit the game.
         *
         * @param playerID The ID of the player quitting the game.
         * @return The ID of the opponent if one is present, 0 if not.
         */
        int quitGame(int playerID) {
            if (player1ID == 0 || player2ID == 0) {
                if (playerID == player1ID) {
                    player1ID = 0;
                    return 0;
                } else {
                    player2ID = 0;
                    return 0;
                }
            } else {
                if (playerID == player1ID) {
                    player1ID = 0;
                    return player2ID;
                } else {
                    player2ID = 0;
                    return player1ID;
                }
            }
        }

        /**
         * Returns the opponent's ID based on the given playerID.
         *
         * @param playerID The ID of the quiting player.
         * @return The ID of the opponent if both players are present, 0 if one player is missing.
         */
        int getOpponentID(int playerID) {
            if (player1ID != 0 && player2ID != 0) {
                if (playerID == player1ID) {
                    return player2ID;
                } else {
                    return player1ID;
                }
            } else {
                return 0;
            }
        }

        /**
         * Places a piece for the given player on the grid at the given position.
         * Checks for winning conditions and if the move is valid or not.
         *
         * @param row      The row where to place the piece.
         * @param col      The column where to place the piece.
         * @param playerID The ID of the player placing the piece.
         * @return An integer indicating the result of the action:
         *         -1 if it is not the player's turn.
         *         -2 if the move is out of bounds.
         *         -3 if the position is already played.
         *         -4 if the game is waiting for an opponent.
         *         -5 if the game is finished.
         *          1 if the player wins.
         *          2 if the game ends on a draw.
         *          0 if the game continues.
         */
        int placePiece(int row, int col, int playerID) {
            if (isFinished) {
                return -5;
            }

            if (player1ID == 0 || player2ID == 0) {
                return -4;
            }

            if ((playerID == player1ID && turnOf) || (playerID == player2ID && !turnOf)) {
                return -1;
            }

            int realRow = row - 65;
            int realCol = col - 1;

            if (realRow < 0 || realRow > gridSize - 1 || realCol < 0 || realCol > gridSize - 1) {
                return -2;
            }

            if (table[realRow][realCol] != 0) {
                return -3;
            }

            if (playerID == player1ID) {
                table[realRow][realCol] = 1;
                turnOf = true;
            } else if (playerID == player2ID) {
                table[realRow][realCol] = 2;
                turnOf = false;
            }

            tilePlayed++;

            boolean winningRow = true;
            boolean winningCol = true;
            boolean winningRightDiagonal = true;
            boolean winningLeftDiagonal = true;

            // Check for winning conditions on the row, column, and diagonals.
            for (int c = 1; c < gridSize; c++) {
                if (table[realRow][c] != table[realRow][c - 1]) {
                    winningRow = false;
                    break;
                }
            }

            for (int r = 1; r < gridSize; r++) {
                if (table[r][realCol] != table[r - 1][realCol]) {
                    winningCol = false;
                    break;
                }
            }

            for (int i = 1; i < gridSize; i++) {
                if (table[i][i] != table[i - 1][i - 1] || table[i][i] == 0) {
                    winningRightDiagonal = false;
                    break;
                }
            }

            for (int r = 1, c = gridSize - 2; r < gridSize; r++, c--) {
                if (table[r][c] != table[r - 1][c + 1] || table[r][c] == 0) {
                    winningLeftDiagonal = false;
                    break;
                }
            }

            if (winningRow || winningCol || winningRightDiagonal || winningLeftDiagonal) {
                isFinished = true;
                return 1;
            }

            if (tilePlayed >= gridSize * gridSize) {
                isFinished = true;
                return 2;
            }

            return 0;
        }

        /**
         * Returns a string representing the game table.
         * The table is formatted with row labels (A, B, C, ...) and column numbers (1, 2, 3, ...).
         * [X] represents player1, [O] represents player2 and [ ] represents unplayed positions.
         *
         * @return A formatted string representing the current state of the game.
         */
        String getTable() {
            /* format example
               1   2   3
            A  0 ¦ 0 ¦ 0
              ---¦---¦---
            B  0 ¦ 0 ¦ 0
              ---¦---¦---
            C  0 ¦ 0 ¦ 0
             */
            StringBuilder tableString = new StringBuilder();
            for (int i = 1; i <= gridSize; i++) {
                tableString.append("   ").append(i);
            }
            tableString.append("/");

            char rowLabel = 'A';

            for (int rows = 0; rows < gridSize; rows++) {
                tableString.append(rowLabel).append("  ");

                for (int cols = 0; cols < gridSize; cols++) {
                    tableString.append(switch (table[rows][cols]) {
                        case 1 -> "X";
                        case 2 -> "O";
                        default -> " ";
                    });

                    if (cols < gridSize - 1) {
                        tableString.append(" ¦ ");
                    }
                }

                tableString.append("/  ");

                if (rows < gridSize - 1) {
                    for (int cols = 0; cols < gridSize; cols++) {
                        tableString.append("---");

                        if (cols < gridSize - 1) {
                            tableString.append("¦");
                        }
                    }
                    tableString.append("/");
                }
                rowLabel++;
            }
            return tableString.toString();
        }

        /**
         * Indicates the size of the grid.
         *
         * @return The size of the game grid.
         */
        int getGridSize() {
            return gridSize;
        }
    }

    /**
     * Contains the ASCII art messages displayed when a game ends. 
     * These messages are shown depending on the result of the game:
     * - Win: Displays a "YOU WON" message in bright green and blinking style.
     * - Lose: Displays a "YOU LOST" message in bright red and blinking style.
     * - Draw: Displays a "DRAW" message in yellow and blinking style.
     */
    private static class EndGameMessage {
        /* Win ASCII art message
        __  __ ____   __  __   _       __ ____   _   __
        \ \/ // __ \ / / / /  | |     / // __ \ / | / /
         \  // / / // / / /   | | /| / // / / //  |/ /
         / // /_/ // /_/ /    | |/ |/ // /_/ // /|  /
        /_/ \____/ \____/     |__/|__/ \____//_/ |_/
        */
        private static final String GAME_WON = " n" +
                ANSI_BRIGHT_GREEN + ANSI_BLINK +
                "__  __ ____   __  __   _       __ ____   _   __ n" +
                "\\ \\/ // __ \\ / / / /  | |     / // __ \\ / | / / n" +
                " \\  // / / // / / /   | | /| / // / / //  |/ / n" +
                " / // /_/ // /_/ /    | |/ |/ // /_/ // /|  / n" +
                "/_/ \\____/ \\____/     |__/|__/ \\____//_/ |_/ n" +
                "n" + ANSI_RESET;


        /* Lose ASCII art message
        __  __ ____   __  __   __    ____  _____ ______
        \ \/ // __ \ / / / /  / /   / __ \/ ___//_  __/
         \  // / / // / / /  / /   / / / /\__ \  / /
         / // /_/ // /_/ /  / /___/ /_/ /___/ / / /
        /_/ \____/ \____/  /_____/\____//____/ /_/
        */
        private static final String GAME_LOST = " n" +
                ANSI_BRIGHT_RED + ANSI_BLINK +
                "__  __ ____   __  __   __    ____  _____ ______ n" +
                "\\ \\/ // __ \\ / / / /  / /   / __ \\/ ___//_  __/ n" +
                " \\  // / / // / / /  / /   / / / /\\__ \\  / / n" +
                " / // /_/ // /_/ /  / /___/ /_/ /___/ / / / n" +
                "/_/ \\____/ \\____/  /_____/\\____//____/ /_/ n" +
                "n" + ANSI_RESET;


        /* Draw ASCII art message
            ____   ____   ___  _       __
           / __ \ / __ \ /   || |     / /
          / / / // /_/ // /| || | /| / /
         / /_/ // _, _// ___ || |/ |/ /
        /_____//_/ |_|/_/  |_||__/|__/
        */
        private static final String GAME_DRAW = " n" +
                ANSI_YELLOW + ANSI_BLINK +
                "    ____   ____   ___  _       __ n" +
                "   / __ \\ / __ \\ /   || |     / / n" +
                "  / / / // /_/ // /| || | /| / / n" +
                " / /_/ // _, _// ___ || |/ |/ / n" +
                "/_____//_/ |_|/_/  |_||__/|__/ n" +
                "n" + ANSI_RESET;
    }

    /**
     * Checks if a given string is a valid number.
     *
     * This method tries to parse the string as an integer. If the string can be parsed, it returns true
     * indicating the string is a number. If the string cannot be parsed and throws an Exception, it returns false.
     *
     * @param str The string to check.
     * @return true if the string represents a valid integer, false otherwise.
     */
    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
