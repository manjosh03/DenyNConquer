package networking.server;

import javafx.scene.paint.Color;
import networking.utils.Constants;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 ** Created a class centric to share data between threads
 * A singleton class that contains all the data that needs to be shared across the different server threads
 */
class ServerData {

    // ServerData Default constructor returning out NULL
    private static ServerData instance = null;

    //Maps clients chat responses using PrintWriter.
    private final Map<Integer, PrintWriter> clientResponses;

    // Map used to match client with already given value of colour.
    private final Map<Integer, Integer> clientColors;

    // Map showing all the canvases which are operating.
    private final Map<Integer, Integer> canvasesInUse;

    // Map which connects client to their score
    private final Map<Color, Integer> clientScores;

    //Boolean helper function to  keep the integrity of CanvasID
    private final Boolean[] isLocked;

    //List containing all clientIDs of everyone currently in lobby
    private final List<Integer> playersInLobby;
    //List containing all clientIDs of everyone currently ready in lobby
    private final List<Integer> readyPlayersInLobby;
    //List containing chat history
    private final List<String> lobbyMessagesList;


    private ServerSocket serverSocket;

    //Default constructor serverData
    public static ServerData getInstance() {
        if(instance == null) {
            instance = new ServerData();
        }

        return instance;
    }

    //Constructor
    private ServerData() {
        clientResponses = new HashMap<>();
        clientColors = new HashMap<>();
        canvasesInUse = new HashMap<>();
        clientScores = new HashMap<>();
        playersInLobby = new ArrayList<>();
        readyPlayersInLobby = new ArrayList<>();
        lobbyMessagesList =  new LinkedList<>();
        isLocked = new Boolean[8*8];
        Arrays.fill(isLocked, false);
    }

    /**
     * Adding a new client.
     * @param clientID
     * @param clientOutput
     */
    public synchronized void addClient(int clientID, PrintWriter clientOutput) {
        clientResponses.put(clientID, clientOutput);
    }


    /**
     * Removes a client.
     * @param clientID
     */
    public synchronized void removeClient(int clientID) {
        clientColors.remove(clientID);
        canvasesInUse.remove(clientID);
        clientResponses.remove(clientID);
        playersInLobby.remove((Integer) clientID);
        readyPlayersInLobby.remove((Integer) clientID);
        //using the helper function to make sure whether the removed player was the only one that wasn't ready.
        checkAllReady();


        // If the last player left, then restart the server.
        if(clientResponses.size() == 0) {
            System.out.println("All client's disconnected. Resetting Server");
            restartServer();
            ServerRoot server = new ServerRoot();
            server.setName("Server");
            server.setDaemon(true);
            server.start();
        }
    }
    /**
     * Restarting the server after use
     */
    private synchronized void restartServer() {
        clientResponses.clear();
        clientColors.clear();
        canvasesInUse.clear();
        clientScores.clear();
        playersInLobby.clear();
        readyPlayersInLobby.clear();
        lobbyMessagesList.clear();
        Arrays.fill(isLocked, false);
    }


    /**
     * Locks the given canvas.
     * @param canvasID
     */
    public synchronized void lockCanvas(int canvasID) {
        if(canvasID >= isLocked.length || canvasID < 0) {
            throw new IllegalArgumentException("Invalid canvasID");
        }

        isLocked[canvasID] = true;
    }

    /**
     * Boolean function that let us know whether a player can use a canvas to draw.
     * @param clientID
     * @param canvasID
     */
    public synchronized boolean CanvasToDraw(int clientID, int canvasID) {
        if(!clientResponses.containsKey(clientID)) {
            throw new IllegalArgumentException("Attempting to acquire a canvas with an invalid clientID");
        }

        if(canvasID >= isLocked.length || canvasID < 0) {
            throw new IllegalArgumentException("Invalid canvasID");
        }

        if(canvasesInUse.containsValue(canvasID) || isLocked[canvasID]) {
            return false;
        }

        canvasesInUse.put(clientID, canvasID);
        return true;
    }



    /**
     * Boolean function to know whether the canvas player want to take, is in use or not.
     * @param clientID
     * @param colorHash
     */
    public synchronized boolean ColourInUse(int clientID, int colorHash) {
        return clientColors.containsKey(clientID) && clientColors.get(clientID).equals(colorHash);
    }

    /**
     * Boolean function to know whether the canvas can be used to draw or not.
     * @param clientID
     * @param canvasID
     */
    public synchronized boolean ValidCanvas(int clientID, int canvasID) {
        return canvasesInUse.containsKey(clientID) && canvasesInUse.get(clientID).equals(canvasID);
    }

    /**
     * Layout to sent messages.
     * @param message
     * @param clientID
     */
    private synchronized void LayoutForClient(String message, int clientID) {
        System.out.println("Server Sent - "+ message);
        clientResponses.get(clientID).println(message);
    }

    /**
     * Send message to all players online/connected
     * @param message
     */
    public synchronized void sendMessage(String message) {
        for(int clientID : clientResponses.keySet()) {
            LayoutForClient(message, clientID);
        }
    }

    /**
     * Sending message to a give player(s)
     * @param message
     * @param clientIDs
     */
    public synchronized void sendMessage(String message, int[] clientIDs) {
        for(int clientID : clientIDs) {
            if(!clientResponses.containsKey(clientID)) {
                throw new IllegalArgumentException("Invalid clientID in array");
            }

            LayoutForClient(message, clientID);
        }
    }

    /**
     * Sending message to a give player
     * @param message
     * @param clientID
     */
    public synchronized void sendMessage(String message, int clientID) {
        if(!clientResponses.containsKey(clientID)) {
            throw new IllegalArgumentException("Invalid clientID in array");
        }

        LayoutForClient(message, clientID);
    }

    /**
     * Sending message to everyone using filter - List
     * @param message
     * @param excludedClients
     */
    public synchronized void sendMessageFilter(String message, List<Integer> excludedClients) {
        for(int clientID : clientResponses.keySet()) {
            if(!excludedClients.contains(clientID)) {
                LayoutForClient(message, clientID);
            }
        }
    }

    /**
     * Sending message to everyone using filter - Solo
     * @param message
     * @param excludedClient
     */
    public synchronized void sendMessageFilter(String message, int excludedClient) {
        for(int clientID : clientResponses.keySet()) {
            if(clientID != excludedClient) {
                LayoutForClient(message, clientID);
            }
        }
    }

    /**
     * Removes and idle the canvas (if in use).
     * @param clientID
     */
    public synchronized void releaseCanvas(int clientID) {
        canvasesInUse.remove(clientID);
    }

    /**
     * Boolean function of whether the player can take a given colour or not.
     * @param clientID
     * @param colorHash
     */
    public synchronized boolean registerColor(int clientID, int colorHash) {
        if(clientColors.containsValue(colorHash)) {
            return false;
        }

        clientColors.put(clientID, colorHash);
        return true;
    }

    /**
     * Sets score of a colour.
     * @param color
     * @param score
     */
    public synchronized boolean setScore(Color color, int score){
        clientScores.put(color, score);

        return !Arrays.asList(isLocked).contains(false);
    }

    /**
     * This function returns the winning colour. Returns after the games finished.
     * if there is more than one winner, we use color.TRANSPARENT
     */
    public synchronized Color getWinningColor() {
        if(Arrays.asList(isLocked).contains(false)) {
            throw new IllegalStateException("Attempting to get the winning color in an unfinished game");
        }

        int highestScore = getWinnerScore();
        int scoreCount = 0;
        Color color = null;

        for (Map.Entry<Color, Integer> entry : clientScores.entrySet()) {  // Iterate through hashmap
            if (entry.getValue() == highestScore) {
                scoreCount++;
                color = entry.getKey();
            }
        }

        return scoreCount == 1 ? color : Color.TRANSPARENT;
    }

    /**
     * Returns the winning score.
     */
    public synchronized int getWinnerScore() {
        if(Arrays.asList(isLocked).contains(false)) {
            throw new IllegalStateException("Attempting to get the winning score in an unfinished game");
        }

        return Collections.max(clientScores.values());// This will return highest Score
    }


    /**
     * Adding the clientID to the list of players in lobby.
     * @param clientID
     */
    public synchronized void playerJoined(int clientID) {
        playersInLobby.add(clientID);
    }

    /**
     * starts the game if everyone is ready
     * @param clientID The ID of the client
     * @param isReady The new ready status
     */
    public synchronized void playerReady(int clientID, boolean isReady) {
        if(isReady) {
            if(!readyPlayersInLobby.contains(clientID)) {
                readyPlayersInLobby.add(clientID);
            }
        }
        else {
            readyPlayersInLobby.remove((Integer) clientID); // object removal
        }

        checkAllReady();
    }

    /**
     * Adding lobby chat to list of all sent messages
     * @param message
     */
    public synchronized void addMessageList(String message) {
        lobbyMessagesList.add(message);
    }

    /**
     * Sharing chat history with  a given client
     * @param clientID
     */
    public synchronized void sendMessageHistory(int clientID) {
        for(String msg : lobbyMessagesList) {
            sendMessage(msg, clientID);
        }
    }

    /**
     * Start the game (countdown) if everyone is ready
     */
    private synchronized void checkAllReady() {
        if(playersInLobby.size() == readyPlayersInLobby.size()) {
            String message = Constants.startCountdownMsgWaitingRoom();
            addMessageList(message);
            sendMessage(message);

            try {
                serverSocket.close();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Setting ServerSocket for new connection.
     * @param socket
     */
    public synchronized void setServerSocket(ServerSocket socket) {
        this.serverSocket = socket;
    }



    //checking if a player won a game or if there is a tie
    public ConcurrentHashMap<Color, Integer> checkResult(){
        int highestScore = Collections.max(clientScores.values());// This will return highest Score

        ConcurrentHashMap <Color, Integer> winners = new ConcurrentHashMap<>();

        for (Map.Entry<Color, Integer> entry : clientScores.entrySet()) {  // Iterate through hashmap
            if (entry.getValue() == highestScore) {
                winners.put(entry.getKey(), entry.getValue());
            }
        }
        return winners;
    }

}
