package networking.server;

import java.util.Scanner;

public class ServerMain {

    public static void main(String[] args) {
        startServerAndHandleInput();
    }

    private static void startServerAndHandleInput() {
        ServerRoot server = startNetworkServer();
        waitForUserInput();
    }

    private static ServerRoot startNetworkServer() {
        ServerRoot server = new ServerRoot();
        server.setDaemon(true);
        server.setName("Server");
        server.start();
        System.out.println("Server started on port " + "8080");
        return server;
    }

    private static void waitForUserInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press Enter to stop the server...");
        scanner.nextLine();
        scanner.close();
    }
}