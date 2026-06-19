package networking.client;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.List;

public class ClientNetworkThread extends Thread {

    private final BufferedReader messageReader;
    private final List<NetworkObserver> messageObservers;
    private boolean isRunning = true;

    public ClientNetworkThread(BufferedReader messageReader, List<NetworkObserver> messageObservers) {
        this.messageReader = messageReader;
        this.messageObservers = messageObservers;
    }

    public void stopReceiving() {
        isRunning = false;
    }

    @Override
    public void run() {
        try {
            String receivedMessage;
            while (isRunning && (receivedMessage = messageReader.readLine()) != null) {
                notifyObservers(receivedMessage);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private void notifyObservers(String message) {
        for (NetworkObserver observer : messageObservers) {
            observer.messageReceived(message);
        }
    }
}
