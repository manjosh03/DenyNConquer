package networking.server;

import com.example.denyconquer.DrawInfo;
import com.example.denyconquer.LobbyPlayer;
import javafx.scene.paint.Color;
import networking.utils.Constants;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import java.io.*;
import java.net.*;
import java.util.Objects;

/**
 Whenever a new client connects to the server to play the game, a client thread is created.
 * All messages that the client sends to the server are taken care of here.
 */
public class ConnectThread extends Thread {

    /**These lines declare private member variables for the ClientThread class.
     clientSocket: This variable holds the socket connection with the client.
     input: This variable is used for reading data from the client.
     clientID: This variable holds the unique identifier for the client thread.
     serverData: This variable holds an instance of the ServerData class.
     player: This variable holds information about the lobby player associated with the client thread.
     */
    private final Socket clientSocket;
    private BufferedReader input;
    private final int clientID;
    private final ServerData serverData;
    private LobbyPlayer player = null;

    public ConnectThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        serverData = ServerData.getInstance();
        clientID = clientSocket.hashCode();
    }


    /** This run method is executed when the thread starts.
     This is the modified version of the run method. It calls the setupStreams method to establish input and output streams
     and then calls the handleClientCommunication method to handle incoming messages. If a SocketException or IOException occurs
     during the process, it is caught and handled by calling the respective methods (handleClientDisconnect and handleIOException).
     Finally, it prints a message indicating that the client thread is stopping. */

    public void run() {
        System.out.println("Client Processor Starting");
        PrintWriter output = null;
        try {
            setupStreams();
            handleClientCommunication();
        } catch (SocketException ex) {
            handleClientDisconnect();
        } catch (IOException ex) {
            handleIOException(ex);
        }
        System.out.println("Client Thread Stopping");
    }

    private void setupStreams() throws IOException {
        input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
        serverData.addClient(clientID, output);
    }
    /**
     This private method is responsible for setting up the input and output streams for the client socket. It initializes the input
     variable with a BufferedReader to read data from the client socket's input stream. It also initializes a new PrintWriter called
     output to send data through the client socket's output stream. Additionally, it adds the client to the server's list of clients
     using the serverData instance.
     */
    private void handleClientCommunication() throws IOException {
        while (true) {
            processMessage(input.readLine());
        }
    }

    /**
     This private method is responsible for handling the communication with the client. It runs in an infinite loop and reads
     lines from the input stream using the input.readLine() method. It then passes each received line to the processMessage
     method for further handling.
     */
    private void handleClientDisconnect() {
        sendLobbyPlayerLeft();
        serverData.removeClient(clientID);
        closeResources();
    }
    /**
     This private method is responsible for handling the disconnection of the client. It calls the sendLobbyPlayerLeft method
     to notify other players about the player leaving the lobby. Then, it removes the client from the server's list of clients
     using the serverData instance and closes the resources related to the client using the closeResources method.
     */
    private void closeResources() {
        try {
            if (input != null) {
                input.close();
            }
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleIOException(IOException ex) {
        System.out.println("Exception on Server: " + ex.getMessage());
        ex.printStackTrace();
    }
    private void sendLobbyPlayerLeft() {
        String message = Constants.leftMsgWaitingRoom(player);
        serverData.addMessageList(message);
        if(Objects.nonNull(player)) {
            serverData.sendMessage(message);
        }
    }

    
    private void processMessage(String msg) {
        String[] parts = msg.split("-", 2);
        if (parts.length != 2) {
            handleInvalidMessage();
            return;
        }

        String header = parts[0];
        String data = parts[1];

        Map<String, Runnable> headerToProcessMethod = new HashMap<>();
        headerToProcessMethod.put(Constants.BOARD_HEADER, () -> handleBoardDrawMsg(data));
        headerToProcessMethod.put(Constants.BOARD_REQ_HEADER, () -> handleBoardReqMsg(data));
        headerToProcessMethod.put(Constants.BOARD_REL_HDR, this::handleBoardReleaseMsg);
        headerToProcessMethod.put(Constants.COLOUR_REQ_HEADER, () -> handleColorReqMsg(data));
        headerToProcessMethod.put(Constants.BOARD_FREEZE, () -> handleLockMsg(data));
        headerToProcessMethod.put(Constants.BOARD_CLEAR, () -> handleBoardClearMsg(data));
        headerToProcessMethod.put(Constants.BOARD_ACQUIRED, () -> handleBoardAcquiredMsg(data));
        headerToProcessMethod.put(Constants.COMPUTE_RESULTS, () -> handleComputeResultsMsg(data));
        headerToProcessMethod.put(Constants.WAITING_ROOM_JOIN_HDR, () -> handlePlayerJoinedMsg(data));
        headerToProcessMethod.put(Constants.WAITING_ROOM_READY_HDR, () -> handlePlayerReadyMsg(data));

        Runnable processMethod = headerToProcessMethod.get(header);
        if (processMethod != null) {
            processMethod.run();
        } else {
            handleInvalidMessage();
        }

    }
    

    private void handleInvalidMessage() {
        throw new IllegalArgumentException("Message transmitted over the network is not valid.");
    }

    private void handleBoardDrawMsg(String data) {
        DrawInfo info = DrawInfo.fromJson(data);

        int colorHash = info.getColor().hashCode();
        int canvasID = info.getCanvasID();

        // Check the colour and canvas are valid
        if(!serverData.ValidCanvas(clientID, canvasID)) {
            throw new IllegalStateException("Attempting to draw on an canvas that isn't registered to the user");
        }
        if(!serverData.ColourInUse(clientID, colorHash)) {
            throw new IllegalStateException("Attempting to draw with an unregistered colour!");
        }

        serverData.sendMessageFilter(Constants.appendBoardHeader(data), clientID);
    }

    private void handleBoardReqMsg(String data) {
        int canvasID = Integer.parseInt(data);

        boolean success = serverData.CanvasToDraw(clientID, canvasID);

        serverData.sendMessage(Constants.appendBoardReqHdr(Boolean.toString(success)), clientID);
    }

    private void handleBoardReleaseMsg() {
        serverData.releaseCanvas(clientID);
    }

    private void handleColorReqMsg(String data) {
        int colorHash = Integer.parseInt(data);

        boolean success = serverData.registerColor(clientID, colorHash);
        serverData.sendMessage(Constants.appendColorReq(Boolean.toString(success)), clientID);
    }

    private void handleLockMsg(String data) {
        int canvasID = Integer.parseInt(data);
        serverData.lockCanvas(canvasID);
    }

    private void handleBoardClearMsg(String data) {
        serverData.sendMessage(Constants.clearBoard(data));
    }

    private void handleBoardAcquiredMsg(String data) {

        String[] parts = data.split("/", 2);
        if (parts.length != 2) {
            handleInvalidMessage();
            return;
        }

        String id = parts[0];
        String stringColor = parts[1];
        try {
            Color color = Color.valueOf(stringColor);
            serverData.sendMessage(Constants.acquireBlock(id, color));
        } catch (IllegalArgumentException ex) {
            handleInvalidMessage();
        }
    }

    /**This method calls the method to store the score of the user in a hashmap and if game ended calls the method
     to check the winner
     */
    private void handleComputeResultsMsg(String data) {


        String[] parts = data.split("/");
        if (parts.length != 2) {
            handleInvalidMessage();
            return;
        }
      
        try {
            int score = Integer.parseInt(parts[0]);
            Color color = Color.valueOf(parts[1]);
            boolean allCanvasColored = serverData.setScore(color, score);

            if (allCanvasColored) {
                serverData.sendMessage(Constants.computeScoreResults(Integer.toString(serverData.getWinnerScore()), serverData.getWinningColor()));
            }
        } catch (NumberFormatException  ex) {
            handleInvalidMessage();
        }
    }

    private void handlePlayerJoinedMsg(String data) {
        String[] fields = data.split("/");
        player = new LobbyPlayer(Color.valueOf(fields[0]), fields[1]);
        String message = Constants.joinWaitingRoom(data);


        // Send message to self, then send history to self, then add to history, then send to everyone else
        // A message must always be added to history before being sent to everyone
        // this prevents someone from joining at the perfect time and missing the message since it was sent to everyone (they didn't join yet)
        // then they asked for history and got it (message wasn't in history) then finally the message got added to history but they missed it.
        // The self entry needs to be the first one the new player gets so it needs to be sent before history leading to this setup.
        serverData.sendMessage(message, clientID); //It sends the message (indicating that a player has joined the lobby)
                                                    //to a specific client indicated by the clientID. This ensures that the player
                                                    // who just joined receives the message.
        serverData.sendMessageHistory(clientID); //It sends the entire lobby message history to the client with the provided clientID.
                                                      //This is to make sure that the newly joined player gets all the previous chat messages that
                                                      // were exchanged in the lobby before they joined.
        serverData.addMessageList(message);  //It adds the current message (player joined message) to the list of lobby messages
                                                   //stored in the serverData instance. This list maintains a history of chat messages
                                                   // exchanged in the lobby.
        serverData.sendMessageFilter(message, clientID); //sends the message to all clients except the one with the provided clientID. This
                                                             // ensures that the existing players in the lobby are notified about the new player who joined.

        serverData.playerJoined(clientID); //informs the serverData instance that a new player has joined the lobby. It associates the clientID
                                                // of the newly joined player with their lobby player information.
    }

    private void handlePlayerReadyMsg(String data) {
        serverData.playerReady(clientID, Boolean.parseBoolean(data.split("/")[1]));
        String message = Constants.readyWaitingRoom(data);
        serverData.addMessageList(message);
        serverData.sendMessage(message);
    }

}


