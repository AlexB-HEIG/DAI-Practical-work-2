package ch.heigvd.dai.game;

public class GameClient {

    private  final String HOST;
    private  final int PORT;
    private static final int CLIENT_ID = (int) (Math.random() * 1000000);

    public GameClient(String host, int port) {
        this.HOST = host;
        this.PORT = port;
    }

    public void launchClient() {


    }

}
