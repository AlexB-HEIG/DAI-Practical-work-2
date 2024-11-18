package ch.heigvd.dai;

import ch.heigvd.dai.commands.Root;

import java.io.File;

import ch.heigvd.dai.game.GameClient;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {


        GameClient client = new GameClient("localhost", 6433);

        client.launchClient();

        System.out.println("Hello world!");

        // Array<Array<int>> gameTable;

        int[][] i = new int[3][3];

        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                System.out.println(switch (i[col][row]) {
                    case 0 -> "_";
                    case 1 -> "X";
                    case 2 -> "O";
                    default -> "wrong value";
                });

            }
        }


        /*

        // Define command name - source: https://stackoverflow.com/a/11159435
        String jarFilename =
                new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
                        .getName();

        // Create root command
        Root root = new Root();

        // Execute command and get exit code
        int exitCode =
                new CommandLine(root)
                        .setCommandName(jarFilename)
                        .setCaseInsensitiveEnumValuesAllowed(true)
                        .execute(args);

        System.exit(exitCode);

         */
    }
}