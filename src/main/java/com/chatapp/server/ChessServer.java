package com.chatapp.server;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

public class ChessServer extends WebSocketServer {

    private final ExecutorService executorService;
    private final Set<WebSocket> connections;
    private final Gson gson;
    private final Map<WebSocket, Player> connectionPlayerMap;
    private final Map<String, GameRoom> gameRooms;
    private final Queue<Player> waitingQueue;

    public ChessServer(int port) {
        super(new InetSocketAddress(port));
        this.executorService = Executors.newCachedThreadPool();
        this.connectionPlayerMap = new ConcurrentHashMap<>();
        this.gameRooms = new ConcurrentHashMap<>();
        this.waitingQueue = new ConcurrentLinkedQueue<>();
        this.gson = new Gson();
        this.connections = Collections.synchronizedSet(new HashSet<>());
        setConnectionLostTimeout(60);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        connections.add(webSocket);
        System.out.println("Kết nối mới từ: " + webSocket.getRemoteSocketAddress());
        System.out.println("Tổng số kết nối: " + connections.size());
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        connections.remove(webSocket);
        Player player = connectionPlayerMap.get(webSocket);
        if (player != null) {
            handlePlayerDisconnect(player);
            connectionPlayerMap.remove(webSocket);
        }
        System.out.println("Kết nối đóng: " + reason);
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        executorService.submit(() -> handleMessage(webSocket, message));
    }

    private void handleMessage(WebSocket webSocket, String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(message, Map.class);
            String type = (String) data.get("type");

            switch (type) {
                case "connect":
                    handlePlayerConnect(webSocket, data);
                    break;
                case "join":
                    handlePlayerJoin(webSocket, data);
                    break;
                case "create_room":
                    handleCreateRoom(webSocket, data);
                    break;
                case "join_room":
                    handleJoinRoom(webSocket, data);
                    break;
                case "leave_room":
                    handleLeaveRoom(webSocket, data);
                    break;
                case "move_request":
                    handleGameMove(webSocket, data);
                    break;
                case "chat":
                    handleChatMessage(webSocket, data);
                    break;
                case "get_rooms":
                    handleGetRooms(webSocket);
                    break;
                default:
                    System.out.println("Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.out.println("Error handleMessage!");
            e.printStackTrace();
        }
    }

    private void handlePlayerConnect(WebSocket  webSocket, Map<String, Object> data) {
        String playerId = UUID.randomUUID().toString();
        String playerName = (String) data.get("playerName");

        Player player = new Player(playerId, playerName, webSocket);
        connectionPlayerMap.put(webSocket, player);

        System.out.println("checked");
        // Gửi thông tin người chơi
        Map<String, Object> response = new HashMap<>();
        response.put("type", "player_info");
        response.put("playerId", playerId);
        response.put("playerName", playerName);
        webSocket.send(gson.toJson(response));
    }

    private void handlePlayerJoin(WebSocket webSocket, Map<String, Object> data) {
//        String playerId = UUID.randomUUID().toString();
//        String playerName = (String) data.get("playerName");
//
//        Player player = new Player(playerId, playerName, webSocket);
//        connectionPlayerMap.put(webSocket, player);
//
//        // Gửi thông tin người chơi
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "player_info");
//        response.put("playerId", playerId);
//        response.put("playerName", playerName);
//        webSocket.send(gson.toJson(response));
        Player player = connectionPlayerMap.get(webSocket);
        String playerId = player.getPlayerId();

        // Thêm vào queue tìm trận
        waitingQueue.offer(player);
        System.out.println("Player " + playerId + " is matching");
        tryMatchmaking();
    }

    private void tryMatchmaking() {
        // Match two waiting players
        if (waitingQueue.size() >= 2) {
            Player player1 = waitingQueue.poll();
            Player player2 = waitingQueue.poll();

            if (player1 != null && player2 != null) {
                String roomId = generateUniqueRoomId();

                GameRoom room = new GameRoom(roomId);
                player1.setColor("white");
                player2.setColor("black");
                room.addPlayer(player1);
                room.addPlayer(player2);
                gameRooms.put(roomId, room);

                for (Player player : room.getPlayers()) {
                    Map<String, Object> roomInfo = new HashMap<>();
                    roomInfo.put("type", "room_info");
                    roomInfo.put("roomId", roomId);
                    player.getConnection().send(gson.toJson(roomInfo));

                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "color");
                    response.put("color", player.getColor());
                    player.getConnection().send(gson.toJson(response));
                }

                startGame(room);
                broadcastRoomsList();
            }
        }
    }

    private void startGame(GameRoom room) {
        room.setStatus("playing");
        room.setCurrentTurn("white");

        Map<String, Object> gameStartData = new HashMap<>();
        gameStartData.put("gameState", room.getGameState());
        gameStartData.put("currentTurn", "white");

        notifyRoomPlayers(room, "game_start", gameStartData);
    }

    //Send message to two players in Gameroom
    private void notifyRoomPlayers(GameRoom room, String messagesType, Map<String, Object> data) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", messagesType);
        message.putAll(data);

        String jsonMessage = gson.toJson(message);

        for (Player player : room.getPlayers()) {
            try {
                if (player.getConnection().isOpen()) {
                    player.getConnection().send(jsonMessage);
                }
            } catch (Exception e) {
                System.err.println("Error sending room message: " + e.getMessage());
            }
        }
    }

    //Get list of all room in status waitin ( Just have one player )
    private void broadcastRoomsList() {
        List<Map<String, Object>> roomsList = new ArrayList<>();

        for (GameRoom room : gameRooms.values()) {
            if (room.getStatus().equals("waiting")) {
                Map<String, Object> roomInfo = new HashMap<>();
                roomInfo.put("roomId", room.getRoomId());
                roomInfo.put("playerCount", room.getPlayers().size());
                roomsList.add(roomInfo);
            }
        }

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "room_update");
        broadcast.put("rooms", roomsList);
        String jsonMessage = gson.toJson(broadcast);

        // Broadcast to all connections
        for (WebSocket conn : connectionPlayerMap.keySet()) {
            try {
                if (conn.isOpen()) {
                    conn.send(jsonMessage);
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting rooms list: " + e.getMessage());
            }
        }
    }

    //Auto generateRoomID
    private String generateUniqueRoomId() {
        Random random = new Random();
        String roomId;

        do {
            // Tạo số ngẫu nhiên 6 chữ số (000000 - 999999)
            int number = random.nextInt(1000000);
            roomId = String.format("%06d", number); // Đảm bảo đủ 6 số
        } while (gameRooms.containsKey(roomId)); // Kiểm tra trùng

        return roomId;
    }

    private void handleCreateRoom(WebSocket webSocket, Map<String, Object> data) {
        Player player = connectionPlayerMap.get(webSocket);
        if (player == null) return;

        // Remove player from waiting queue if they're in it
        waitingQueue.remove(player);

        String roomId = generateUniqueRoomId();

        GameRoom room = new GameRoom(roomId);
        player.setColor("white"); // Room creator is white
        room.addPlayer(player);
        gameRooms.put(roomId, room);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "room_created");
        response.put("roomId", roomId);
        response.put("color", player.getColor());
        webSocket.send(gson.toJson(response));

        broadcastRoomsList();
    }

    private void handleJoinRoom(WebSocket webSocket, Map<String, Object> data) {
        Player player = connectionPlayerMap.get(webSocket);
        if (player == null) return;

        // Remove player from waiting queue if they're in it
        waitingQueue.remove(player);

        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);

        if (room == null) {
            sendError(webSocket, "Room does not exist!");
            return;
        }

        if (room.isFull()) {
            sendError(webSocket, "Room is already full!");
            return;
        }

        player.setColor("black"); // Joiner is black
        room.addPlayer(player);

        // Notify new player
        Map<String, Object> joinResponse = new HashMap<>();
        joinResponse.put("type", "room_joined");
        joinResponse.put("roomId", roomId);
        joinResponse.put("color", player.getColor());
        joinResponse.put("gameState", room.getGameState());
        webSocket.send(gson.toJson(joinResponse));

        // Notify other players about new player
        Map<String, Object> playerJoinedData = new HashMap<>();
        playerJoinedData.put("playerName", player.getPlayerName());
        playerJoinedData.put("color", player.getColor());
        notifyRoomPlayers(room, "player_joined", playerJoinedData);

        // If room is full, start game
        if (room.isFull()) {
            startGame(room);
        }

        broadcastRoomsList();
    }

    private void handleLeaveRoom(WebSocket webSocket, Map<String, Object> data) {
        Player player = connectionPlayerMap.get(webSocket);
        if (player == null) return;

        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);

        if (room != null) {
            room.removePlayer(player);

            // Notify remaining players
            Map<String, Object> leftData = new HashMap<>();
            leftData.put("leftPlayer", player.getPlayerName());
            notifyRoomPlayers(room, "player_left", leftData);

            // Remove room if empty
            if (room.isEmpty()) {
                gameRooms.remove(roomId);
            }

            broadcastRoomsList();
        }
    }

    private void handleGameMove(WebSocket webSocket, Map<String, Object> data) {
        Player player = connectionPlayerMap.get(webSocket);
        if (player == null) return;

        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);

        if (room == null || !room.getStatus().equals("playing")) {
            return;
        }

        // Check turn based on color, not player name
        if (!room.getCurrentTurn().equals(player.getColor())) {
            sendError(webSocket, "It's not your turn!");
            return;
        }

        Map<String, Object> moveData = new HashMap<>();
        moveData.put("from", data.get("from"));
        moveData.put("to", data.get("to"));
        moveData.put("color", player.getColor());
        moveData.put("piece", data.get("piece"));

        ChessValidator.MoveResult moveResult = room.getValidator().validateMove(moveData.get("from").toString(), moveData.get("to").toString(),
                moveData.get("color").toString());
        if(!moveResult.valid)
        {
            moveData.put("result", false);
            webSocket.send(gson.toJson(moveData));
        }
        else {
            moveData.put("result", true);
            notifyRoomPlayers(room, "move_result", moveData);
            if(moveResult.winner != null)
            {
                Map<String, Object> response = new HashMap<>();
                response.put("winner", moveResult.winner);
                notifyRoomPlayers(room, "end_game", response);
            }
            System.out.println(moveData.get("from").toString() + " " + moveData.get("to").toString());
            // Change turn
            String nextTurn = player.getColor().equals("white") ? "black" : "white";
            room.setCurrentTurn(nextTurn);

            Map<String, Object> turnData = new HashMap<>();
            turnData.put("currentTurn", nextTurn);
            notifyRoomPlayers(room, "turn_change", turnData);
        }
    }

    private void handleChatMessage(WebSocket webSocket, Map<String, Object> data) {
        Player player = connectionPlayerMap.get(webSocket);
        if (player == null) return;

        String roomId = (String) data.get("roomId");
        String message = (String) data.get("message");
        GameRoom room = gameRooms.get(roomId);
        System.out.println(message + player.getPlayerName());

        if (room != null) {
            Map<String, Object> chatData = new HashMap<>();
            chatData.put("playerName", player.getPlayerName());
            chatData.put("message", message);
            chatData.put("timestamp", System.currentTimeMillis());

            notifyRoomPlayers(room, "chat", chatData);
        }
    }

    private void handleGetRooms(WebSocket webSocket) {
        List<Map<String, Object>> roomList = new ArrayList<>();

        for (GameRoom room : gameRooms.values()) {
            if (room.getStatus().equals("waiting")) {
                Map<String, Object> roomInfo = new HashMap<>();
                roomInfo.put("roomId", room.getRoomId());
                roomInfo.put("playerCount", room.getPlayers().size());
                roomInfo.put("status", room.getStatus());
                roomList.add(roomInfo);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "room_list");
        response.put("rooms", roomList);
        webSocket.send(gson.toJson(response));
    }

    private void sendError(WebSocket webSocket, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");
        error.put("message", errorMessage);
        try {
            webSocket.send(gson.toJson(error));
        } catch (Exception e) {
            System.err.println("Error sending error message: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Chess Server đã khởi động thành công!");
        System.setProperty("java.awt.headless", "true");
    }

    private void handlePlayerDisconnect(Player player) {
        // Remove from waiting queue
        waitingQueue.remove(player);

        // Find and handle room disconnection
        String roomToRemove = null;
        for (Map.Entry<String, GameRoom> entry : gameRooms.entrySet()) {
            GameRoom room = entry.getValue();
            if (room.getPlayers().contains(player)) {
                room.removePlayer(player);

                if (room.isEmpty()) {
                    roomToRemove = entry.getKey();
                } else {
                    // Notify other players
                    Map<String, Object> disconnectData = new HashMap<>();
                    disconnectData.put("disconnectedPlayer", player.getPlayerName());
                    notifyRoomPlayers(room, "player_disconnected", disconnectData);
                }
                break;
            }
        }

        if (roomToRemove != null) {
            gameRooms.remove(roomToRemove);
        }

        broadcastRoomsList();
    }

    public void shutdown() {
        try {
            this.stop();
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public static void main(String[] args) {
        int port = 8080;
        ChessServer server = new ChessServer(port);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        server.start();
        System.out.println("WebSocket Chess Server chạy tại ws://localhost:" + port);
    }
}