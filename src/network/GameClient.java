package network;

import java.io.*;
import java.net.Socket;

public class GameClient {
    private static final Object CONSOLE_LOCK = new Object();

    public static void main(String[] args) throws Exception {

        Socket socket = new Socket("localhost", 8080);

        BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

        BufferedReader userInput =
                new BufferedReader(
                        new InputStreamReader(System.in));

        PrintWriter out =
                new PrintWriter(socket.getOutputStream(), true);

        System.out.println(in.readLine());

        String name = userInput.readLine();

        out.println(name);
        printShortcutHelp();

        Thread serverListener = new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    synchronized (CONSOLE_LOCK) {
                        System.out.println("\n" + response);
                        System.out.print("> ");
                    }
                }
            } catch (IOException ignored) {
            }
        });
        serverListener.setDaemon(true);
        serverListener.start();

        System.out.print("> ");

        String userCommand;
        while ((userCommand = userInput.readLine()) != null) {
            String commandToSend = mapShortcutToCommand(userCommand);
            out.println(commandToSend);

            String normalized = commandToSend.trim().toLowerCase();
            if (normalized.equals("quit") || normalized.equals("exit")) {
                break;
            }
            System.out.print("> ");
        }

        socket.close();
    }

    private static String mapShortcutToCommand(String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim();
        String lower = input.toLowerCase();

        switch (lower) {
            case "w":
                return "move north";
            case "s":
                return "move south";
            case "d":
                return "move east";
            case "a":
                return "move west";
            case "e":
                return "status";
            case "q":
                return "scan";
            case "h":
                return "help";
            case "x":
                return "quit";
            default:
                break;
        }

        if (lower.startsWith("f ")) {
            String target = input.substring(2).trim();
            if (!target.isEmpty()) {
                return "attack " + target;
            }
        }

        return input;
    }

    private static void printShortcutHelp() {
        System.out.println("Shortcuts: W/A/S/D=move, E=status, Q=scan, H=help, F <player>=attack, X=quit");
    }
}