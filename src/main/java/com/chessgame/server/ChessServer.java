package com.chessgame.server;

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
                case "get_history":
                    handleGetHistory(webSocket, data);
                    break;
                case "cancel_matchmaking":
                    hanleCancelMatchMaking(webSocket, data);
                    break;
                case "draw_request":
                        handleDrawRequest(webSocket, data);
                        break;
                case "draw_response":
                    handleDrawResponse(webSocket,data);
                    break;
                case "get_valid_moves":
                    handleGetValidMove(webSocket, data);
                    break;
                default:
                    System.out.println("Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.out.println("Error handleMessage!" + message);
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

    //Cancel find Game
    private void hanleCancelMatchMaking(WebSocket webSocket, Map<String, Object> data) {
        waitingQueue.poll();
    }

    private void handleDrawRequest(WebSocket webSocket, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        if(room.getIsDrawOffered() != null)
        {
            sendError(webSocket, "Đã có lời cầu hòa trong phòng này!");
        }
        Player player = connectionPlayerMap.get(webSocket);
        Player oponentPlayer = room.getOpponent(player);

        room.setIsDrawOffered(player.getColor());
        Map<String, Object> response = new HashMap<>();
        response.put("type", "draw_offer");
        oponentPlayer.getConnection().send(gson.toJson(response));
    }

    private void handleGetValidMove(WebSocket webSocket, Map<String, Object> data) {
        Player player = connectionPlayerMap.get(webSocket);
        if (player == null) return;
        String roomId = (String) data.get("roomId");
        String square = (String) data.get("square"); // Ví dụ: "e2"
        GameRoom room = gameRooms.get(roomId);

        // Kiểm tra hợp lệ
        if (room == null || !room.getStatus().equals("playing") || !room.getPlayers().contains(player) || square == null) {
            // Không cần gửi lỗi, chỉ đơn giản là không gửi gì nếu yêu cầu không hợp lệ
            System.out.println("Invalid get_valid_moves request from " + player.getPlayerName());
            return;
        }

        // Chỉ tính nước đi nếu đúng lượt của người yêu cầu
        if (!room.getCurrentTurn().equals(player.getColor())) {
            // System.out.println("Not player's turn to get moves: " + player.getPlayerName());
            return; // Không gửi gì nếu không phải lượt
        }


        // Gọi hàm trong ChessValidator
        List<String> validMoves = room.getValidator().getValidMovesForSquare(square);

        // Tạo và gửi phản hồi
        Map<String, Object> response = new HashMap<>();
        response.put("type", "valid_moves");
        response.put("square", square); // Ô gốc
        response.put("moves", validMoves); // Danh sách ô đích ["e3", "e4"]
        player.getConnection().send(gson.toJson(response));
    }

    private void handleDrawResponse(WebSocket webSocket, Map<String, Object> data) {
        Player respondingPlayer = connectionPlayerMap.get(webSocket);
        if (respondingPlayer == null) return; // Không tìm thấy người chơi

        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        Object acceptedObj = data.get("accepted"); // Lấy giá trị accepted

        // --- Thêm kiểm tra đầu vào ---
        if (room == null || !room.getStatus().equals("playing") || !room.getPlayers().contains(respondingPlayer) || acceptedObj == null || !(acceptedObj instanceof Boolean)) {
            sendError(webSocket, "Phản hồi cầu hòa không hợp lệ.");
            return;
        }

        boolean accepted = (Boolean) acceptedObj; // Ép kiểu sang boolean

        // --- Kiểm tra xem có lời đề nghị hòa đang chờ phản hồi từ người này không ---
        String opponentColor = respondingPlayer.getColor().equals("white") ? "black" : "white";
        if (!opponentColor.equals(room.getIsDrawOffered())) {
            sendError(webSocket, "Không có lời cầu hòa nào đang chờ bạn phản hồi.");
            return;
        }

        Player offeringPlayer = room.getPlayerByColor(opponentColor); // Lấy người đã đề nghị hòa

        if (accepted) {
            // --- Xử lý CHẤP NHẬN hòa ---
            System.out.println("Draw accepted in room " + roomId + " by " + respondingPlayer.getPlayerName());

            // Tạo tin nhắn kết thúc game
            Map<String, Object> endData = new HashMap<>();
            endData.put("winner", "draw"); // Kết quả là hòa
            endData.put("reason", "agreement"); // Lý do: Thỏa thuận

            // Gửi tin nhắn kết thúc cho CẢ HAI người chơi
            notifyRoomPlayers(room, "end_game", endData);

            // Dọn dẹp phòng (dừng timer, xóa khỏi map)
//            cleanupRoom(roomId);

        } else {
            // --- Xử lý TỪ CHỐI hòa ---
            System.out.println("Draw rejected in room " + roomId + " by " + respondingPlayer.getPlayerName());

            // Reset trạng thái cầu hòa trong phòng
            room.setIsDrawOffered(null);

            // Gửi tin nhắn từ chối cho người đã đề nghị hòa (offeringPlayer)
            if (offeringPlayer != null && offeringPlayer.getConnection().isOpen()) {
                Map<String, Object> rejectData = new HashMap<>();
                rejectData.put("type", "draw_rejected"); // Client sẽ hiển thị thông báo
                offeringPlayer.getConnection().send(gson.toJson(rejectData));
            } else {
                System.err.println("Không thể gửi draw_rejected, người đề nghị không online?");
            }
            // Người từ chối không cần nhận thông báo gì thêm
        }
    }


    //start game in room
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
        try {
            GameRoom room = gameRooms.get(roomId);
        if (room != null) {
            room.removePlayer(player);
            if(room.isEmpty())
            {
                gameRooms.remove(roomId);
            } else {
                // Notify remaining players
                Map<String, Object> leftData = new HashMap<>();
                leftData.put("leftPlayer", player.getPlayerName());
                leftData.put("reason", "Người chơi rời khỏi phòng!");
                leftData.put("winner", room.getOpponent(player).getColor());
                notifyRoomPlayers(room, "player_left", leftData);

                // Remove room if empty
                if (room.isEmpty()) {
                    gameRooms.remove(roomId);
                }

                broadcastRoomsList();
            }}}
        catch (Exception e) {
            System.err.println("Error leaving room: " + e.getMessage());
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

        String from = data.get("from").toString();
        String to = data.get("to").toString();
        Character promo = data.get("promotion") == null? null : (Character) data.get("promotion");
        ChessValidator.MoveResult moveResult = room.getValidator().validateMove(from, to,
                player.getColor(),  promo);
        if(!moveResult.isValid)
        {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "move_result");
            response.put("result", false);
            response.put("message", moveResult.message);
            webSocket.send(gson.toJson(response));
        }
        else {
            // Change turn
            String nextTurn = player.getColor().equals("white") ? "black" : "white";
            room.setCurrentTurn(nextTurn);
            String fen = room.getValidator().toFen();
            boolean isNextPlayerInCheck = room.getValidator().isKingInCheck(nextTurn, room.getValidator().getBoard()); // Dùng board hiện tại
            Map<String, Object> fenData = new HashMap<>();
            fenData.put("result", true);
            fenData.put("fen", fen);
            Map<String, String> lastMoveData = new HashMap<>();
            lastMoveData.put("from", from); // Key là "from"
            lastMoveData.put("to", to);   // Key là "to"
            fenData.put("lastMove", lastMoveData);
            fenData.put("isCheck", isNextPlayerInCheck);
            notifyRoomPlayers(room, "move_result", fenData);
            if(moveResult.winner != null)
            {
                Map<String, Object> response = new HashMap<>();
                response.put("winner", moveResult.winner);
                notifyRoomPlayers(room, "end_game", response);
            }

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

    private void handleGetHistory(WebSocket webSocket, Map<String, Object> data) {
        // 1. TẠO HOẶC LẤY DANH SÁCH LỊCH SỬ THỰC TẾ
        // Đây là ví dụ về dữ liệu giả định (placeholder):
        List<Map<String, Object>> historyList = new ArrayList<>();
        
        // Ví dụ về một trận đấu (Giả sử bạn có một lớp MatchResult hoặc Map<String, Object> với các trường:
        // playerX, playerO, winner, date)
        Map<String, Object> match1 = new HashMap<>();
        match1.put("playerX", "Alice");
        match1.put("playerO", "Bob");
        match1.put("winner", "Alice");
        match1.put("date", new Date().getTime()); // Sử dụng timestamp
        historyList.add(match1);
    
        Map<String, Object> match2 = new HashMap<>();
        match2.put("playerX", "Charlie");
        match2.put("playerO", "David");
        match2.put("winner", "David");
        match2.put("date", new Date().getTime() - 3600000); // 1 giờ trước
        historyList.add(match2);
        
        // 2. CHUẨN BỊ PHẢN HỒI THEO ĐỊNH DẠNG CLIENT MONG MUỐN
        Map<String, Object> response = new HashMap<>();
        response.put("type", "history_list"); // <-- Đã sửa: Khớp với data.type === "history_list"
        response.put("history", historyList); // <-- Đã sửa: Khớp với renderHistoryList(data.history)
        webSocket.send(gson.toJson(response));
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