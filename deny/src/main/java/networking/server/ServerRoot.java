
package networking.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ServerRoot extends Thread {
    private static final int DEFAULT_PORT = 8080;

    @Override
    public void run() {
        System.out.println("Initializing Server");

        try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT)) {
            initializeServerSocket(serverSocket);

            while (true) {
                acceptAndHandleClient(serverSocket);
            }
        } catch (SocketException ex) {
            System.out.println("Closing Server to new clients");
        } catch (IOException ex) {
            handleIOException(ex);
        }
    }

    private void initializeServerSocket(ServerSocket serverSocket) {
        ServerData.getInstance().setServerSocket(serverSocket);
        System.out.println("Server started on port: " + DEFAULT_PORT);
    }

    private void acceptAndHandleClient(ServerSocket serverSocket) throws IOException {
        Socket socket = serverSocket.accept();
        String clientAddress = getClientAddress(socket);
        System.out.println("New connection from: " + clientAddress);

        ConnectThread clientThread = new ConnectThread(socket);
        clientThread.setDaemon(true);
        clientThread.setName("Client Thread: " + clientAddress);
        clientThread.start();
    }

    private String getClientAddress(Socket socket) {
        return socket.getInetAddress().toString() + ":" + socket.getPort();
    }

    private void handleIOException(IOException ex) {
        System.out.println("Exception occurred: " + ex.getMessage());
        ex.printStackTrace();
    }

    public static void main(String[] args) {
        ServerRoot server = new ServerRoot();
        server.start();
    }
}