package networking.client;
import com.example.denyconquer.*;
import java.util.List;
import java.util.concurrent.*;
import java.io.BufferedReader;
import java.util.Objects;
import javafx.scene.paint.Color;
import networking.utils.Constants;
import java.io.IOException;
import java.io.InputStreamReader;
import javafx.application.Platform;
import java.net.Socket;
import java.util.ArrayList;
import java.io.PrintWriter;



// The ClientNetwork makes it easier for the local game and the network server to communicate.

public class ClientNetwork {

    private static final int SERVER_RESPONSE_TIMEOUT = 100;

    private PrintWriter output;
    private BufferedReader input;
    private Socket socket;
    private final List<NetworkObserver> messageObservers;

    public Color playerColor = null; //player's colour is null at first
    public final InputHandler inputHandler;
    public int currentCanvasID;

    // When choosing a canvas or registering a colour,
    // a blocking queue is employed for thread-safe communication
    // between the main application thread and the network thread.
    private final BlockingQueue<Boolean> serverBoolResponseQueue;
    private boolean isClientRunning = false;
    private boolean isFirstDraw = false;

    private LobbyControllerCallback lobbyCallback;


    public ClientNetwork(String host, String port) throws IOException, IllegalArgumentException {
        serverBoolResponseQueue = new LinkedBlockingQueue<>();
        messageObservers = new ArrayList<>();
        inputHandler = new InputHandler();
        addObserver(inputHandler);
        currentCanvasID = -1;
        socket = new Socket(host, Integer.parseInt(port));

        try {
            setupIOStreams();
            startNetworkThread();
        }
        catch (IOException exp)
        {
            exp.printStackTrace();
        }

    }

    private void setupIOStreams() throws IOException {
        output = new PrintWriter(socket.getOutputStream(),true);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void startNetworkThread(){
        ClientNetworkThread networkThread = new ClientNetworkThread(input, messageObservers);
        networkThread.setName("Client Network Thread");
        networkThread.setDaemon(true);
        networkThread.start();
    }
    private void addObserver(NetworkObserver obs) {
        this.messageObservers.add(obs);
    }

    private void removeObserver(NetworkObserver obs) {
        this.messageObservers.remove(obs);
    }

    public void startClient() {

        if(playerColor == null) {
            throw new IllegalStateException("Attempting to start the client without registering a colour!");
        }

        isClientRunning = true;
    }


// Attempts to register a colour with the server. The method will block until a response from the server occurs
// or the response times out.
// "color" is the colour being registered
// Returns true if the colour is successfully registered and false if the colour is already in use

    public boolean registerColor(Color color) {
        output.println(Constants.appendColorReq(Integer.toString(color.hashCode())));
        System.out.println("Client sent - " + Constants.appendColorReq(Integer.toString(color.hashCode())));
        Boolean response;

        try {
            response = serverBoolResponseQueue.poll(SERVER_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
            // Null response means timeout occurred
            if(!Objects.nonNull(response)) {
                throw new RuntimeException("Server timed out in registerColor");
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(response) {
            playerColor = color;
        }

        return response;
    }

 
    public boolean startBoardDrawing(int canvasID) {
        validateClientRunning();
        boolean response = sendRequestAndWait(Constants.appendBoardReqHdr(Integer.toString(canvasID)),canvasID);

        return response;
    }

//     validates whether the client is currently running or not
//      It throws an exception if the client is not running.

    private void validateClientRunning(){
        if(!isClientRunning){
            throw new IllegalStateException("Client is not running.");
        }
    }

//    Sends a request to the server and waits for a response.
//    It handles sending the request and waiting for a response using serverBoolResponseQueue.
//    "request" here is The  request to be sent to the server

    private boolean sendRequestAndWait(String request, int canvasID)
    {
        output.println(request);
        boolean r = waitForServerResponse(canvasID);
        return r;
    }


//    Receives the server response from serverBoolResponseQueue.
//    Handles receiving the server response and handling timeouts.

    private boolean waitForServerResponse(int canvasID){
        Boolean response;
        try{
            response =serverBoolResponseQueue.poll(SERVER_RESPONSE_TIMEOUT,TimeUnit.MILLISECONDS);
            if(!Objects.nonNull(response)){
                throw new RuntimeException("Server response timed out.");
            }
        }
        catch (RuntimeException | InterruptedException E){
            throw new RuntimeException(E);
        }

        if(response){
            currentCanvasID = canvasID;
            isFirstDraw =  true;
        }

        return response;

    }

    //Releases the canvas owned by the client.
    //If no canvases are owned then does nothing

    public void releaseCanvas() {
        if(!isClientRunning) {
            throw new IllegalStateException("Attempting to release canvas without a running client");
        }

        output.println(Constants.releaseBoardMsg());
        System.out.println("Client sent - " + Constants.releaseBoardMsg());

        currentCanvasID = -1;
    }

//     validates whether the player color is registered or not
//     It throws an exception if the player color is not registered.

    private void validatePlayerColor(){
        if(playerColor == null){
            throw new IllegalStateException("Player color is not registered.");
        }
    }

//      validates whether the canvas is registered or not
//      It throws an exception if no canvas is  registered.

    private void validCanvasRegistered()
    {
        if(currentCanvasID == -1){
            throw new IllegalStateException("No Canvas is registered.");
        }
    }

//    This notifies the server that a specific pixel on the
//     registered canvas has been drawn. The recorded client
//     colour is the hue of the pixel.
    public void sendDrawing(double x, double y) {
        validateClientRunning();
        validatePlayerColor();
        validCanvasRegistered();

        DrawInfo drawInfo = createDrawInfo(x,y);
        output.println(Constants.drawBoardMsg(drawInfo));
        System.out.println("Client sent - " + Constants.drawBoardMsg(drawInfo));

        isFirstDraw = false;
    }

    private DrawInfo createDrawInfo(double x, double y){
        return new DrawInfo(x,y,currentCanvasID,playerColor,isFirstDraw,false,false);
    }

    //  It notifies the server that the current canvas
    //  has to be locked out of the way for other clients.
    public void sendLockCanvasRequest() {

        validateClientRunning();
        validatePlayerColor();
        validCanvasRegistered();

        String stringCanvasID = Integer.toString(currentCanvasID);
        output.println(Constants.freezeBoard(stringCanvasID));
        System.out.println("Client sent - " + Constants.freezeBoard(stringCanvasID));

    }
   //It notifies the server that the current canvas has to
   // be cleared to make room for additional clients.
    public void sendClearCanvas() {
        validateClientRunning();
        validatePlayerColor();
        validCanvasRegistered();

        String stringCanvasID = Integer.toString(currentCanvasID);
        output.println(Constants.clearBoard(stringCanvasID));
        System.out.println("Client sent - " + Constants.clearBoard(stringCanvasID));

    }

    public void sendOwnCanvas() {
        validateClientRunning();
        validatePlayerColor();
        validCanvasRegistered();
        String stringCanvasID = Integer.toString(currentCanvasID);
        output.println(Constants.acquireBlock(stringCanvasID, playerColor));
        System.out.println("Client sent - " + Constants.acquireBlock(stringCanvasID, playerColor));

    }

    public void sendScore(int score) {
        validateClientRunning();
        validatePlayerColor();

        String stringScore = Integer.toString(score);
        output.println(Constants.computeScoreResults(stringScore, playerColor));
        System.out.println("Client sent - " + Constants.computeScoreResults(stringScore, playerColor));

    }

    //uses the specified lobby callback to open the lobby.
    public void startLobby(LobbyControllerCallback lobbyCallback, LobbyPlayer player) {
        this.lobbyCallback = lobbyCallback;
        output.println(Constants.joinMsgWaitingRoom(player));
        System.out.println("Client sent - " + Constants.joinMsgWaitingRoom(player));

    }

    public void setPlayerReady(LobbyPlayer player, boolean isReady) {
        output.println(Constants.readyMsgWaitingRoom(player, isReady));
        System.out.println("Client sent - " + Constants.readyMsgWaitingRoom(player, isReady));

    }

    public class InputHandler implements NetworkObserver {
        private final ConcurrentLinkedQueue<DrawInfo> drawInfoQueue;
        private final List<LobbyPlayer> lobbyPlayersList = new ArrayList<>();

        public InputHandler() {
            drawInfoQueue = new ConcurrentLinkedQueue<>();
        }

        public boolean areInputsAvailable() {
            return !drawInfoQueue.isEmpty();
        }

        public DrawInfo getNextInput() {
            return drawInfoQueue.poll();
        }

        @Override
        public void messageReceived(String message) {
            String header = message.split("-", 2)[0];
            String data = message.split("-", 2)[1];

            if (header.equals(Constants.BOARD_HEADER)) {
                handleDrawMessage(data);
            } else if (header.equals(Constants.BOARD_REQ_HEADER) ||
                    header.equals(Constants.COLOUR_REQ_HEADER)) {
                handleServerResponse(Boolean.parseBoolean(data));
            } else if (header.equals(Constants.COMPUTE_RESULTS)) {
                handleGameResults(data);
            } else if (header.equals(Constants.BOARD_CLEAR)) {
                handleClearCanvas(data);
            } else if (header.equals(Constants.BOARD_ACQUIRED)) {
                handleOwnCanvas(data);
            } else if (header.equals(Constants.WAITING_ROOM_JOIN_HDR)) {
                handleLobbyPlayerJoin(data);
            } else if (header.equals(Constants.WAITING_ROOM_LEFT_HDR)) {
                handleLobbyPlayerLeft(data);
            } else if (header.equals(Constants.WAITING_ROOM_READY_HDR)) {
                handleLobbyPlayerReady(data);
            } else if (header.equals(Constants.WAITING_ROOM_COUNTDOWN_START_HDR)) {
                handleLobbyStartCountdown();
            } else {
                throw new IllegalArgumentException("Received invalid network message");
            }

        }


        private void handleDrawMessage(String data) {
            drawInfoQueue.add(DrawInfo.fromJson(data));
        }

        private void handleServerResponse(boolean response) {
            serverBoolResponseQueue.add(response);
        }

        private void handleGameResults(String data) {
            String winnerScore = data.split("/")[0];
            String stringColor = data.split("/")[1];

            Color winnerColor = Color.valueOf(stringColor);
            int score = Integer.parseInt(winnerScore);
            GameResults results = new GameResults(score, winnerColor);
            Game.GameEndResults endResults = new Game.GameEndResults(results);
            Platform.runLater(() -> {
                endResults.run();
            });
        }

        private void handleClearCanvas(String data) {
            int canvasID = Integer.parseInt(data);
            DrawInfo clear = new DrawInfo(0, 0, canvasID, Color.TRANSPARENT, false, true, false);
            drawInfoQueue.add(clear);
        }

        private void handleOwnCanvas(String data) {
            String[] msg = data.split("/", 2);
            Color color = Color.valueOf(msg[1]);
            DrawInfo own = new DrawInfo(0, 0, Integer.parseInt(msg[0]), color, false, false, true);
            drawInfoQueue.add(own);
        }

        private void handleLobbyPlayerJoin(String data) {
            if (Objects.isNull(lobbyCallback)) {
                return;
            }

            String[] fields = data.split("/", 2);
            LobbyPlayer newPlayer = createLobbyPlayer(fields[0], fields[1]);
            lobbyPlayersList.add(newPlayer);
            lobbyCallback.addPlayer(newPlayer);
        }

        private void handleLobbyPlayerLeft(String data) {
            if (Objects.isNull(lobbyCallback)) {
                return;
            }

            Color leftPlayerColor = Color.valueOf(data);
            int leftPlayerIndex = getLobbyPlayerIndexByColor(leftPlayerColor);
            lobbyCallback.removePlayer(leftPlayerIndex);
            lobbyPlayersList.remove(leftPlayerIndex);
        }

        private void handleLobbyPlayerReady(String data) {
            if (Objects.isNull(lobbyCallback)) {
                return;
            }

            String[] splitData = data.split("/");
            Color playerColor = Color.valueOf(splitData[0]);
            boolean isReady = Boolean.parseBoolean(splitData[1]);

            int readyPlayerIndex = getLobbyPlayerIndexByColor(playerColor);
            lobbyPlayersList.get(readyPlayerIndex).setPlayerReady(isReady);
            lobbyCallback.setReady(readyPlayerIndex, isReady);
        }

        private void handleLobbyStartCountdown() {
            lobbyCallback.startGameCountdown();
        }

        private int getLobbyPlayerIndexByColor(Color color) {
            for (int i = 0; i < lobbyPlayersList.size(); i++) {
                if (lobbyPlayersList.get(i).getPlayerColor().equals(color)) {
                    return i;
                }
            }
            return -1;
        }

        private LobbyPlayer createLobbyPlayer(String color, String playerName) {
            LobbyPlayer newPlayer = new LobbyPlayer(Color.valueOf(color), playerName);
            if (newPlayer.getPlayerColor().equals(playerColor)) {
                newPlayer = new LobbyPlayer(playerColor, "(You) " + playerName);
                newPlayer.setPlayerIsUser(true);
            }
            return newPlayer;
        }
    }

}