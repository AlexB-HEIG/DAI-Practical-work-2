package ch.heigvd.dai.game;

public class GameServer {

    private final int PORT;
    private static final int SERVER_ID = (int) (Math.random() * 1000000);

    public GameServer(int port) {
        this.PORT = port;
    }

    public void launchServer() {
        
    }

}
