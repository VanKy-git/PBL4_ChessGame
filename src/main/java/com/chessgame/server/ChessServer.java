//package com.chessgame.server;
//
//import com.database.server.Utils.JwtConfig;
//import com.google.gson.Gson;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jws;
//import io.jsonwebtoken.JwtException;
//import io.jsonwebtoken.Jwts;
//import org.java_websocket.WebSocket;
//import org.java_websocket.handshake.ClientHandshake;
//import org.java_websocket.server.WebSocketServer;
//
//import java.net.InetSocketAddress;
//import java.util.*;
//import java.util.concurrent.*;
//
//public class ChessServer extends WebSocketServer {
//
//    private final ExecutorService executorService;
//    private final Set<WebSocket> connections;
//    private final Gson gson;
//    private final Map<WebSocket, Player> connectionPlayerMap;
//    private final Map<String, GameRoom> gameRooms;
//    private final Queue<Player> waitingQueue;
//
//    public ChessServer(int port) {
//        super(new InetSocketAddress(port));
//        this.executorService = Executors.newCachedThreadPool();
//        this.connectionPlayerMap = new ConcurrentHashMap<>();
//        this.gameRooms = new ConcurrentHashMap<>();
//        this.waitingQueue = new ConcurrentLinkedQueue<>();
//        this.gson = new Gson();
//        this.connections = Collections.synchronizedSet(new HashSet<>());
//        setConnectionLostTimeout(60);
//    }
//
//    @Override
//    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
//        connections.add(webSocket);
//        System.out.println("Kết nối mới từ: " + webSocket.getRemoteSocketAddress());
//        System.out.println("Tổng số kết nối: " + connections.size());
//    }
//
//    @Override
//    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
//        connections.remove(webSocket);
//        Player player = connectionPlayerMap.get(webSocket);
//        if (player != null) {
//            handlePlayerDisconnect(player);
//            connectionPlayerMap.remove(webSocket);
//        }
//        System.out.println("Kết nối đóng: " + reason);
//    }
//
//    @Override
//    public void onMessage(WebSocket webSocket, String message) {
//        executorService.submit(() -> handleMessage(webSocket, message));
//    }
//
//    private void handleMessage(WebSocket webSocket, String message) {
//        try {
//            @SuppressWarnings("unchecked")
//            Map<String, Object> data = gson.fromJson(message, Map.class);
//            String type = (String) data.get("type");
//            Player player = connectionPlayerMap.get(webSocket);
//
//            // Nếu player chưa được xác thực VÀ tin nhắn không phải là "auth"
//            // (Chúng ta chỉ cho phép tin nhắn "auth" nếu chưa login)
//            if (player == null && !"auth".equals(type)) {
//                sendError(webSocket, "Bạn cần xác thực trước khi thực hiện hành động này.");
//                return;
//            }
//
//            switch (type) {
//                case "join":
//                    handlePlayerJoin(webSocket, data);
//                    break;
//                case "create_room":
//                    handleCreateRoom(webSocket, data);
//                    break;
//                case "join_room":
//                    handleJoinRoom(webSocket, data);
//                    break;
//                case "leave_room":
//                    handleLeaveRoom(webSocket, data);
//                    break;
//                case "move_request":
//                    handleGameMove(webSocket, data);
//                    break;
//                case "chat":
//                    handleChatMessage(webSocket, data);
//                    break;
//                case "get_rooms":
//                    handleGetRooms(webSocket);
//                    break;
//                case "get_history":
//                    handleGetHistory(webSocket, data);
//                    break;
//                case "cancel_matchmaking":
//                    hanleCancelMatchMaking(webSocket, data);
//                    break;
//                case "draw_request":
//                        handleDrawRequest(webSocket, data);
//                        break;
//                case "draw_response":
//                    handleDrawResponse(webSocket,data);
//                    break;
//                case "get_valid_moves":
//                    handleGetValidMove(webSocket, data);
//                    break;
//                case "resign":
//                    handleResign(webSocket, data);
//                    break;
//                case "rematch_request":
//                    handleRematchRequest(webSocket, data);
//                    break;
//                case "auth":
//                    handleAuth(webSocket, data);
//                    break;
//                default:
//                    System.out.println("Unknown message type: " + type);
//            }
//        } catch (Exception e) {
//            System.out.println("Error handleMessage!" + message);
//            e.printStackTrace();
//        }
//    }
//
//    private void handleAuth(WebSocket webSocket, Map<String, Object> data) {
//        String token = (String) data.get("token");
//
//        // Trường hợp 1: Chơi GUEST (không có token)
//        if (token == null) {
//            String guestName = "Guest_" + (new Random().nextInt(9000) + 1000);
//            String guestId = "guest_" + UUID.randomUUID().toString();
//
//            Player guestPlayer = new Player(guestId, guestName, webSocket);
//            connectionPlayerMap.put(webSocket, guestPlayer);
//
//            // Gửi thông tin Guest về client
//            Map<String, Object> response = new HashMap<>();
//            response.put("type", "player_info");
//            response.put("playerId", guestPlayer.getPlayerId());
//            response.put("playerName", guestPlayer.getPlayerName());
//            webSocket.send(gson.toJson(response));
//            return;
//        }
//
//        // Trường hợp 2: Chơi với tài khoản (có token)
//        try {
//            System.out.println(token + " " + JwtConfig.JWT_SECRET_KEY);
//            // Dùng Key bí mật để xác thực "vé"
//            Jws<Claims> claimsJws = Jwts.parserBuilder()
//                    .setSigningKey(JwtConfig.JWT_SECRET_KEY)
//                    .build()
//                    .parseClaimsJws(token);
//
//            // Nếu không ném lỗi, token là HỢP LỆ
//            Claims payload = claimsJws.getBody();
//            String userId = payload.getSubject(); // Lấy "userId"
//            String username = payload.get("username", String.class); // Lấy "username"
//
//            // (Nên kiểm tra DB xem user này có bị ban không, nhưng tạm thời bỏ qua)
//
//            // Tạo đối tượng Player
//            Player authenticatedPlayer = new Player(userId, username, webSocket);
//            connectionPlayerMap.put(webSocket, authenticatedPlayer); // Lưu player đã xác thực
//
//            // Gửi thông tin về client
//            Map<String, Object> response = new HashMap<>();
//            response.put("type", "player_info");
//            response.put("playerId", authenticatedPlayer.getPlayerId());
//            response.put("playerName", authenticatedPlayer.getPlayerName());
//            webSocket.send(gson.toJson(response));
//
//        } catch (JwtException e) {
//            // Lỗi: Token không hợp lệ (sai, hết hạn, ...)
//            sendError(webSocket, "Xác thực thất bại: " + e.getMessage());
//            webSocket.close(); // Đóng kết nối
//        }
//    }
//
//
//    private void handlePlayerJoin(WebSocket webSocket, Map<String, Object> data) {
//        Player player = connectionPlayerMap.get(webSocket);
//        String playerId = player.getPlayerId();
//        Object timeControlObj = data.get("timeControl");
//        Long preferredTime = null;
//        if (timeControlObj instanceof Number) {
//            preferredTime = ((Number) timeControlObj).longValue();
//        } else {
//            // Gửi lỗi hoặc dùng thời gian mặc định nếu client không gửi
//            sendError(player.getConnection(), "Thiếu hoặc sai định dạng timeControl.");
//            // return; // Hoặc dùng mặc định: preferredTime = 600000L; // 10 phút
//        }
//
//        // Lưu lại vào Player
//        player.setPreferredTimeMs(preferredTime);
//
//        // Thêm vào queue tìm trận
//        waitingQueue.offer(player);
//        System.out.println("Player " + player.getPlayerName() + " joined queue with time: " + preferredTime + "ms");
//        tryMatchmaking();
//    }
//
//    private void tryMatchmaking() {
//        Map<Long, List<Player>> playersByTime = new HashMap<>();
//
//        // Phân loại người chơi theo thời gian chờ
//        Iterator<Player> iterator = waitingQueue.iterator();
//        while (iterator.hasNext()) {
//            Player player = iterator.next();
//            // Kiểm tra kết nối trước khi xử lý
//            if (player == null || !player.getConnection().isOpen()) {
//                iterator.remove(); // Xóa người chơi đã offline khỏi hàng đợi
//                continue;
//            }
//            Long timePref = player.getPreferredTimeMs();
//            if (timePref != null) {
//                playersByTime.computeIfAbsent(timePref, k -> new ArrayList<>()).add(player);
//            } else {
//                // Xử lý người chơi không có preferredTime (có thể xóa hoặc cho vào nhóm mặc định)
//                System.out.println("Player " + player.getPlayerName() + " in queue without preferred time.");
//                iterator.remove();
//            }
//        }
//
//        // Tìm các nhóm thời gian có đủ 2 người trở lên
//        for (Map.Entry<Long, List<Player>> entry : playersByTime.entrySet()) {
//            Long timeControlMs = entry.getKey();
//            List<Player> waitingPlayers = entry.getValue();
//
//            while (waitingPlayers.size() >= 2) {
//                // Lấy 2 người chơi đầu tiên trong nhóm thời gian này
//                Player player1 = waitingPlayers.remove(0);
//                Player player2 = waitingPlayers.remove(0);
//
//                // Xóa họ khỏi hàng đợi gốc (quan trọng!)
//                waitingQueue.remove(player1);
//                waitingQueue.remove(player2);
//
//                System.out.println("Matching players for " + (timeControlMs / 60000) + " min: "
//                        + player1.getPlayerName() + " vs " + player2.getPlayerName());
//
//                // --- Tạo phòng và bắt đầu game (giống code cũ nhưng truyền thời gian) ---
//                String roomId = generateUniqueRoomId();
//                // ✅ Truyền thời gian vào GameRoom (sửa Constructor hoặc thêm setter)
//                GameRoom room = new GameRoom(roomId, timeControlMs, this);
//
//                // Ngẫu nhiên màu cờ
//                if (Math.random() < 0.5) {
//                    player1.setColor("white");
//                player2.setColor("black");
//                }
//                else {
//                    player1.setColor("black");
//                    player2.setColor("white");
//                }
//
//                room.addPlayer(player1);
//                room.addPlayer(player2);
//                gameRooms.put(roomId, room);
//
//                // Gửi thông tin phòng, màu cờ
//                // ... (gửi room_info, color cho cả 2 player) ...
//                for (Player player : room.getPlayers()) {
//                    Map<String, Object> roomInfo = new HashMap<>();
//                    roomInfo.put("type", "room_info");
//                    roomInfo.put("roomId", roomId);
//                    player.getConnection().send(gson.toJson(roomInfo));
//
//                    Map<String, Object> response = new HashMap<>();
//                    response.put("type", "color");
//                    response.put("color", player.getColor());
//                    player.getConnection().send(gson.toJson(response));
//                }
//
//                // Bắt đầu game VỚI THỜI GIAN ĐÚNG
//                startGame(room);
//            }
//        }
//    }
//
//    private void handleResign(WebSocket webSocket, Map<String, Object> data) {
//        Player player = connectionPlayerMap.get(webSocket);
//        if (player == null) {
//            System.err.println("handleResign called with null player.");
//            return;
//        }
//
//        String roomId = (String) data.get("roomId");
//        GameRoom room = gameRooms.get(roomId);
//
//        // --- Kiểm tra tính hợp lệ ---
//        if (room == null) {
//            sendError(player.getConnection(), "Không tìm thấy phòng.");
//            System.out.println("Resign failed: Room not found - " + roomId);
//            return;
//        }
//        // Chỉ cho phép đầu hàng khi đang chơi
//        if (!"playing".equals(room.getStatus())) {
//            sendError(player.getConnection(), "Không thể đầu hàng khi ván đấu chưa bắt đầu hoặc đã kết thúc.");
//            System.out.println("Resign failed: Game not playing in room " + roomId);
//            return;
//        }
//        if (!room.getPlayers().contains(player)) {
//            sendError(player.getConnection(), "Bạn không ở trong phòng này.");
//            System.out.println("Resign failed: Player not in room " + roomId);
//            return;
//        }
//
//        System.out.println("Player " + player.getPlayerName() + " resigned in room " + roomId);
//
//        // --- Xác định người thắng ---
//        String winnerColor = player.getColor().equals("white") ? "black" : "white";
//        Player winnerPlayer = room.getPlayerByColor(winnerColor); // Lấy đối tượng người thắng (nếu cần ID)
//
//        // --- Tạo tin nhắn kết thúc game ---
//        Map<String, Object> endData = new HashMap<>();
//        endData.put("winner", winnerColor); // Gửi màu của người thắng
//        endData.put("reason", "resignation"); // Gửi lý do là đầu hàng
//        if (winnerPlayer != null) {
//            endData.put("winnerId", winnerPlayer.getPlayerId()); // Gửi ID người thắng (Client có thể dùng)
//            endData.put("winnerName", winnerPlayer.getPlayerName()); // Gửi tên người thắng (Client có thể dùng)
//        }
//
//        // --- Gửi tin nhắn cho cả hai người chơi ---
//        notifyRoomPlayers(room,"end_game" ,endData); // Sử dụng hàm nhận Map
//
//        // --- Cập nhật trạng thái phòng và dừng timer (KHÔNG XÓA PHÒNG) ---
//        room.setStatus("finished"); // Đổi trạng thái phòng thành đã kết thúc
//        room.stopTimer();         // Dừng đồng hồ của phòng này
//    }
//
//    // ✅ THÊM HÀM XỬ LÝ TIMEOUT (được gọi bởi GameRoom)
//    public void handleTimeout(GameRoom room, String winnerColor) {
//        if (room == null) return;
//        String roomId = room.getRoomId();
//        System.out.println("Handling timeout for room " + roomId + ". Winner: " + winnerColor);
//
//        // Tạo tin nhắn kết thúc game
//        Map<String, Object> endData = new HashMap<>();
//        endData.put("winner", winnerColor);
//        endData.put("reason", "timeout");
//
//        // Gửi tin nhắn và dọn dẹp phòng
//        notifyRoomPlayers(room, "end_game" ,endData);
//        room.setStatus("finished");
//        room.stopTimer();
//    }
//
//    // ✅ SỬA LẠI cleanupRoom để gọi stopTimer VÀ shutdown service
//    private void cleanupRoom(String roomId) {
//        GameRoom removedRoom = gameRooms.remove(roomId); // Xóa phòng khỏi map
//        if (removedRoom != null) {
//            System.out.println("Cleaning up room " + roomId);
//            removedRoom.stopTimer(); // Dừng task timer (nếu đang chạy)
//            removedRoom.shutdownTimerService(); // Giải phóng executor service của phòng
//            // ... (Logic khác nếu cần) ...
//        } else {
//            System.out.println("Cleanup called for non-existent room: " + roomId);
//        }
//    }
//
//    //Cancel find Game
//    private void hanleCancelMatchMaking(WebSocket webSocket, Map<String, Object> data) {
//        waitingQueue.poll();
//    }
//
//    private void handleDrawRequest(WebSocket webSocket, Map<String, Object> data) {
//        String roomId = (String) data.get("roomId");
//        GameRoom room = gameRooms.get(roomId);
//        if(room.getIsDrawOffered() != null)
//        {
//            sendError(webSocket, "Đã có lời cầu hòa trong phòng này!");
//        }
//        Player player = connectionPlayerMap.get(webSocket);
//        Player oponentPlayer = room.getOpponent(player);
//
//        room.setIsDrawOffered(player.getColor());
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "draw_offer");
//        oponentPlayer.getConnection().send(gson.toJson(response));
//    }
//
//    private void handleGetValidMove(WebSocket webSocket, Map<String, Object> data) {
//        Player player = connectionPlayerMap.get(webSocket);
//        if (player == null) return;
//        String roomId = (String) data.get("roomId");
//        String square = (String) data.get("square"); // Ví dụ: "e2"
//        GameRoom room = gameRooms.get(roomId);
//
//        // Kiểm tra hợp lệ
//        if (room == null || !room.getStatus().equals("playing") || !room.getPlayers().contains(player) || square == null) {
//            // Không cần gửi lỗi, chỉ đơn giản là không gửi gì nếu yêu cầu không hợp lệ
//            System.out.println("Invalid get_valid_moves request from " + player.getPlayerName());
//            return;
//        }
//
//        // Chỉ tính nước đi nếu đúng lượt của người yêu cầu
//        if (!room.getCurrentTurn().equals(player.getColor())) {
//            // System.out.println("Not player's turn to get moves: " + player.getPlayerName());
//            return; // Không gửi gì nếu không phải lượt
//        }
//
//
//        // Gọi hàm trong ChessValidator
//        List<String> validMoves = room.getValidator().getValidMovesForSquare(square);
//
//        // Tạo và gửi phản hồi
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "valid_moves");
//        response.put("square", square); // Ô gốc
//        response.put("moves", validMoves); // Danh sách ô đích ["e3", "e4"]
//        player.getConnection().send(gson.toJson(response));
//    }
//
//    private void handleDrawResponse(WebSocket webSocket, Map<String, Object> data) {
//        Player respondingPlayer = connectionPlayerMap.get(webSocket);
//        if (respondingPlayer == null) return; // Không tìm thấy người chơi
//
//        String roomId = (String) data.get("roomId");
//        GameRoom room = gameRooms.get(roomId);
//        Object acceptedObj = data.get("accepted"); // Lấy giá trị accepted
//
//        // --- Thêm kiểm tra đầu vào ---
//        if (room == null || !room.getStatus().equals("playing") || !room.getPlayers().contains(respondingPlayer) || acceptedObj == null || !(acceptedObj instanceof Boolean)) {
//            sendError(webSocket, "Phản hồi cầu hòa không hợp lệ.");
//            return;
//        }
//
//        boolean accepted = (Boolean) acceptedObj; // Ép kiểu sang boolean
//
//        // --- Kiểm tra xem có lời đề nghị hòa đang chờ phản hồi từ người này không ---
//        String opponentColor = respondingPlayer.getColor().equals("white") ? "black" : "white";
//        if (!opponentColor.equals(room.getIsDrawOffered())) {
//            sendError(webSocket, "Không có lời cầu hòa nào đang chờ bạn phản hồi.");
//            return;
//        }
//
//        Player offeringPlayer = room.getPlayerByColor(opponentColor); // Lấy người đã đề nghị hòa
//
//        if (accepted) {
//            // --- Xử lý CHẤP NHẬN hòa ---
//            System.out.println("Draw accepted in room " + roomId + " by " + respondingPlayer.getPlayerName());
//
//            // Tạo tin nhắn kết thúc game
//            Map<String, Object> endData = new HashMap<>();
//            endData.put("winner", "draw"); // Kết quả là hòa
//            endData.put("reason", "agreement"); // Lý do: Thỏa thuận
//
//            // Gửi tin nhắn kết thúc cho CẢ HAI người chơi
//            notifyRoomPlayers(room, "end_game", endData);
//            room.setStatus("finished");
//            room.stopTimer();
//        } else {
//            // --- Xử lý TỪ CHỐI hòa ---
//            System.out.println("Draw rejected in room " + roomId + " by " + respondingPlayer.getPlayerName());
//
//            // Reset trạng thái cầu hòa trong phòng
//            room.setIsDrawOffered(null);
//
//            // Gửi tin nhắn từ chối cho người đã đề nghị hòa (offeringPlayer)
//            if (offeringPlayer != null && offeringPlayer.getConnection().isOpen()) {
//                Map<String, Object> rejectData = new HashMap<>();
//                rejectData.put("type", "draw_rejected"); // Client sẽ hiển thị thông báo
//                offeringPlayer.getConnection().send(gson.toJson(rejectData));
//            } else {
//                System.err.println("Không thể gửi draw_rejected, người đề nghị không online?");
//            }
//            // Người từ chối không cần nhận thông báo gì thêm
//        }
//    }
//
//
//    //start game in room
//    private void startGame(GameRoom room) {
//        room.setStatus("playing");
//        room.setCurrentTurn("white");
//        room.getValidator().resetBoard();
//        Map<String, Object> gameStartData = new HashMap<>();
//        gameStartData.put("gameState", room.getValidator().toFen());
//        gameStartData.put("currentTurn", "white");
//        gameStartData.put("initialTimeMs", room.getInitialTimeMs());
//        room.startTimer();
//        notifyRoomPlayers(room, "game_start", gameStartData);
//    }
//
//    //Send message to two players in Gameroom
//    public void notifyRoomPlayers(GameRoom room, String messagesType, Map<String, Object> data) {
//        Map<String, Object> message = new HashMap<>();
//        message.put("type", messagesType);
//        message.putAll(data);
//
//        String jsonMessage = gson.toJson(message);
//
//        for (Player player : room.getPlayers()) {
//            try {
//                if (player.getConnection().isOpen()) {
//                    player.getConnection().send(jsonMessage);
//                }
//            } catch (Exception e) {
//                System.err.println("Error sending room message: " + e.getMessage());
//            }
//        }
//    }
//
//    //Get list of all room in status waitin ( Just have one player )
//    private void broadcastRoomsList() {
//        List<Map<String, Object>> roomsList = new ArrayList<>();
//
//        for (GameRoom room : gameRooms.values()) {
//            if (room.getStatus().equals("waiting")) {
//                Map<String, Object> roomInfo = new HashMap<>();
//                roomInfo.put("roomId", room.getRoomId());
//                roomInfo.put("playerCount", room.getPlayers().size());
//                roomsList.add(roomInfo);
//            }
//        }
//
//        Map<String, Object> broadcast = new HashMap<>();
//        broadcast.put("type", "room_update");
//        broadcast.put("rooms", roomsList);
//        String jsonMessage = gson.toJson(broadcast);
//
//        // Broadcast to all connections
//        for (WebSocket conn : connectionPlayerMap.keySet()) {
//            try {
//                if (conn.isOpen()) {
//                    conn.send(jsonMessage);
//                }
//            } catch (Exception e) {
//                System.err.println("Error broadcasting rooms list: " + e.getMessage());
//            }
//        }
//    }
//
//    private String generateUniqueRoomId() {
//        Random random = new Random();
//        String roomId;
//
//        do {
//            // Tạo số ngẫu nhiên 6 chữ số (000000 - 999999)
//            int number = random.nextInt(1000000);
//            roomId = String.format("%06d", number); // Đảm bảo đủ 6 số
//        } while (gameRooms.containsKey(roomId)); // Kiểm tra trùng
//
//        return roomId;
//    }
//
//    private void handleCreateRoom(WebSocket webSocket, Map<String, Object> data) {
//        Player player = connectionPlayerMap.get(webSocket);
//        if (player == null) return;
//
//        waitingQueue.remove(player);
//
//        String roomId = generateUniqueRoomId();
//
//        GameRoom room = new GameRoom(roomId, 60000, this);
//        player.setColor("white");
//        room.addPlayer(player);
//        gameRooms.put(roomId, room);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "room_created");
//        response.put("roomId", roomId);
//        response.put("color", player.getColor());
//        webSocket.send(gson.toJson(response));
//
//        broadcastRoomsList();
//    }
//
//    private void handleJoinRoom(WebSocket webSocket, Map<String, Object> data) {
//        Player player = connectionPlayerMap.get(webSocket);
//        if (player == null) return;
//
//        waitingQueue.remove(player);
//
//        String roomId = (String) data.get("roomId");
//        GameRoom room = gameRooms.get(roomId);
//
//        if (room == null) {
//            sendError(webSocket, "Room does not exist!");
//            return;
//        }
//
//        if (room.isFull()) {
//            sendError(webSocket, "Room is already full!");
//            return;
//        }
//
//        long initialTimeMs = room.getInitialTimeMs();
//        player.setColor("black"); // Joiner is black
//        room.addPlayer(player);
//
//        // Notify new player
//        Map<String, Object> joinResponse = new HashMap<>();
//        joinResponse.put("type", "room_joined");
//        joinResponse.put("roomId", roomId);
//        joinResponse.put("color", player.getColor());
//        joinResponse.put("gameState", room.getValidator().toFen());
//        webSocket.send(gson.toJson(joinResponse));
//
//        // Notify other players about new player
//        Map<String, Object> playerJoinedData = new HashMap<>();
//        playerJoinedData.put("playerName", player.getPlayerName());
//        playerJoinedData.put("color", player.getColor());
//        notifyRoomPlayers(room, "player_joined", playerJoinedData);
//
//        // If room is full, start game
//        if (room.isFull()) {
//            startGame(room);
//        }
//
//        broadcastRoomsList();
//    }
//
//    private void handleLeaveRoom(WebSocket webSocket, Map<String, Object> data) {
//        Player player = connectionPlayerMap.get(webSocket);
//        if (player == null) return;
//
//        String roomId = (String) data.get("roomId");
//        try {
//            GameRoom room = gameRooms.get(roomId);
//        if (room != null) {
//            room.removePlayer(player);
//            if(room.isEmpty())
//            {
//                gameRooms.remove(roomId);
//            } else {
//                // Notify remaining players
//                Map<String, Object> leftData = new HashMap<>();
//                leftData.put("leftPlayer", player.getPlayerName());
//                leftData.put("reason", "Người chơi rời khỏi phòng!");
//                leftData.put("winner", room.getOpponent(player).getColor());
//                notifyRoomPlayers(room, "player_left", leftData);
//
//                // Remove room if empty
//                if (room.isEmpty()) {
//                    gameRooms.remove(roomId);
//                }
//
//                broadcastRoomsList();
//            }}}
//        catch (Exception e) {
//            System.err.println("Error leaving room: " + e.getMessage());
//        }
//        }
//
//    private void handleGameMove(WebSocket webSocket, Map<String, Object> data) {
//        Player player = connectionPlayerMap.get(webSocket);
//        if (player == null) return;
//
//        String roomId = (String) data.get("roomId");
//        GameRoom room = gameRooms.get(roomId);
//
//        if (room == null || !room.getStatus().equals("playing")) {
//            return;
//        }
//
//        // Check turn based on color, not player name
//        if (!room.getCurrentTurn().equals(player.getColor())) {
//            sendError(webSocket, "It's not your turn!");
//            return;
//        }
//
//        String from = data.get("from").toString();
//        String to = data.get("to").toString();
//        Character promo = data.get("promotion") == null? null : (Character) data.get("promotion");
//        ChessValidator.MoveResult moveResult = room.getValidator().validateMove(from, to,
//                player.getColor(),  promo);
//        if(!moveResult.isValid)
//        {
//            Map<String, Object> response = new HashMap<>();
//            response.put("type", "move_result");
//            response.put("result", false);
//            response.put("message", moveResult.message);
//            webSocket.send(gson.toJson(response));
//        }
//        else {
//            // Change turn
//            String nextTurn = player.getColor().equals("white") ? "black" : "white";
//            room.setCurrentTurn(nextTurn);
//            String fen = room.getValidator().toFen();
//            boolean isNextPlayerInCheck = room.getValidator().isKingInCheck(nextTurn, room.getValidator().getBoard()); // Dùng board hiện tại
//            Map<String, Object> fenData = new HashMap<>();
//            fenData.put("result", true);
//            fenData.put("fen", fen);
//            Map<String, String> lastMoveData = new HashMap<>();
//            lastMoveData.put("from", from); // Key là "from"
//            lastMoveData.put("to", to);   // Key là "to"
//            fenData.put("lastMove", lastMoveData);
//            fenData.put("isCheck", isNextPlayerInCheck);
//            notifyRoomPlayers(room, "move_result", fenData);
//            if(moveResult.winner != null)
//            {
//                Map<String, Object> response = new HashMap<>();
//                response.put("winner", moveResult.winner);
//                notifyRoomPlayers(room, "end_game", response);
//                room.setStatus("finished");
//                room.stopTimer();
//            }
//
//        }
//    }
//
//    private void handleRematchRequest(WebSocket webSocket, Map<String, Object> data) {
//        Player player = connectionPlayerMap.get(webSocket);
//        if (player == null) return;
//        String roomId = (String) data.get("roomId");
//        GameRoom room = gameRooms.get(roomId);
//
//        // Kiểm tra phòng và trạng thái
//        if (room == null || !"finished".equals(room.getStatus()) || !room.getPlayers().contains(player)) {
//            sendError(player.getConnection(), "Không thể yêu cầu tái đấu lúc này.");
//            return;
//        }
//
//        Player opponent = room.getOpponent(player);
//        // Kiểm tra đối thủ còn online không
//        if (opponent == null || !opponent.getConnection().isOpen()) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("type", "rematch_unavailable");
//            response.put("reason", "Đối thủ đã offline.");
//            player.getConnection().send(gson.toJson(response));
//            // Có thể cân nhắc xóa phòng luôn ở đây nếu đối thủ offline
//            // cleanupRoom(roomId);
//            return;
//        }
//
//        String playerColor = player.getColor(); // Màu của người yêu cầu
//        String opponentColor = opponent.getColor();
//
//        System.out.println("Rematch request from " + player.getPlayerName() + " (" + playerColor + ") in room " + roomId);
//
//        // Kiểm tra xem đối thủ đã yêu cầu chưa
//        if (playerColor.equals(room.getRematchRequestedByColor())) {
//            System.out.println("Player already requested rematch."); // Đã yêu cầu rồi, không làm gì thêm
//            return;
//        }
//
//        if (opponentColor.equals(room.getRematchRequestedByColor())) {
//            // --- Cả hai đã yêu cầu -> Bắt đầu tái đấu ---
//            System.out.println("Both players requested rematch. Starting rematch in room " + roomId);
//
//            // 1. Đổi màu cờ
//            room.swapPlayerColors();
//
//            // 2. Reset trạng thái phòng (bàn cờ, thời gian, status, ...)
//            room.resetForRematch(); // Hàm này đã dừng timer cũ
//
//            // 3. Lấy thông tin player mới (sau khi đổi màu)
//            Player newWhitePlayer = room.getPlayerByColor("white");
//            Player newBlackPlayer = room.getPlayerByColor("black");
//
//            // 4. Gửi game_start cho cả hai với thông tin mới
//            Map<String, Object> gameStartData = new HashMap<>();
//            gameStartData.put("gameState", room.getValidator().toFen()); // FEN mới
//            gameStartData.put("currentTurn", "white"); // Luôn bắt đầu bằng trắng
//            gameStartData.put("initialTimeMs", room.getInitialTimeMs()); // Thời gian ban đầu
//            if (newWhitePlayer != null && newBlackPlayer != null) { // Gửi lại thông tin player
//                gameStartData.put("playerWhite", Map.of("id", newWhitePlayer.getPlayerId(), "name", newWhitePlayer.getPlayerName()));
//                gameStartData.put("playerBlack", Map.of("id", newBlackPlayer.getPlayerId(), "name", newBlackPlayer.getPlayerName()));
//            }
//            // Gửi màu mới cho từng người (QUAN TRỌNG)
//            Map<String, Object> colorP1 = new HashMap<>();
//            colorP1.put("type", "color");
//            colorP1.put("color", newWhitePlayer.getColor());
//            newWhitePlayer.getConnection().send(gson.toJson(colorP1));
//
//            Map<String, Object> colorP2 = new HashMap<>();
//            colorP2.put("type", "color");
//            colorP2.put("color", newBlackPlayer.getColor());
//            newBlackPlayer.getConnection().send(gson.toJson(colorP2));
//
//            notifyRoomPlayers(room,"game_start" ,gameStartData); // Gửi game_start chung
//
//            // 5. Bắt đầu timer mới
//            room.startTimer();
//
//        } else {
//            // --- Chỉ người này yêu cầu -> Gửi lời mời cho đối thủ ---
//            System.out.println("Sending rematch offer to " + opponent.getPlayerName());
//            room.setRematchRequestedByColor(playerColor); // Đánh dấu người đã yêu cầu
//
//            Map<String, Object> offerData = new HashMap<>();
//            offerData.put("type", "rematch_offer");
//            offerData.put("offeringPlayer", player.getPlayerName());
//            opponent.getConnection().send(gson.toJson(offerData));
//
//            // Thông báo cho người gửi là đã gửi lời mời (tùy chọn)
//            Map<String, Object> ackData = new HashMap<>();
//            ackData.put("type", "rematch_offer_sent");
//            player.getConnection().send(gson.toJson(ackData));
//        }
//    }
//
//    private void handleChatMessage(WebSocket webSocket, Map<String, Object> data) {
//        Player player = connectionPlayerMap.get(webSocket);
//        if (player == null) return;
//
//        String roomId = (String) data.get("roomId");
//        String message = (String) data.get("message");
//        GameRoom room = gameRooms.get(roomId);
//        System.out.println(message + player.getPlayerName());
//
//        if (room != null) {
//            Map<String, Object> chatData = new HashMap<>();
//            chatData.put("playerName", player.getPlayerName());
//            chatData.put("message", message);
//            chatData.put("timestamp", System.currentTimeMillis());
//
//            notifyRoomPlayers(room, "chat", chatData);
//        }
//    }
//
//    private void handleGetRooms(WebSocket webSocket) {
//        List<Map<String, Object>> roomList = new ArrayList<>();
//
//        for (GameRoom room : gameRooms.values()) {
//            if (room.getStatus().equals("waiting")) {
//                Map<String, Object> roomInfo = new HashMap<>();
//                roomInfo.put("roomId", room.getRoomId());
//                roomInfo.put("playerCount", room.getPlayers().size());
//                roomInfo.put("status", room.getStatus());
//                roomList.add(roomInfo);
//            }
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "room_list");
//        response.put("rooms", roomList);
//        webSocket.send(gson.toJson(response));
//    }
//
//    private void sendError(WebSocket webSocket, String errorMessage) {
//        Map<String, Object> error = new HashMap<>();
//        error.put("type", "error");
//        error.put("message", errorMessage);
//        try {
//            webSocket.send(gson.toJson(error));
//        } catch (Exception e) {
//            System.err.println("Error sending error message: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public void onError(WebSocket webSocket, Exception ex) {
//        ex.printStackTrace();
//    }
//
//    @Override
//    public void onStart() {
//        System.out.println("Chess Server đã khởi động thành công!");
//        System.setProperty("java.awt.headless", "true");
//    }
//
//    private GameRoom findRoomByPlayer(Player player) {
//        // Kiểm tra đầu vào cơ bản
//        if (player == null) {
//            return null;
//        }
//
//        // Duyệt qua tất cả các giá trị (GameRoom instances) trong Map gameRooms
//        for (GameRoom room : gameRooms.values()) {
//            // Sử dụng phương thức contains() của danh sách người chơi trong phòng
//            // Điều này yêu cầu lớp Player phải override equals() và hashCode() đúng cách (dựa trên playerId),
//            // code Player.java của bạn đã làm điều này.
//            if (room.getPlayers().contains(player)) {
//                return room; // Trả về phòng ngay khi tìm thấy người chơi
//            }
//        }
//
//        // Nếu duyệt qua tất cả các phòng mà không tìm thấy người chơi
//        return null;
//    }
//
//    private void handlePlayerDisconnect(Player player) {
//        System.out.println("Player disconnected: " + player.getPlayerName());
//        waitingQueue.remove(player);
//        GameRoom room = findRoomByPlayer(player);
//        if (room != null) {
//            String roomId = room.getRoomId();
//            room.removePlayer(player);
//            System.out.println("Player " + player.getPlayerName() + " removed from room " + roomId + " due to disconnect.");
//
//            if (room.isEmpty()) {
//                System.out.println("Room " + roomId + " is empty after disconnect, cleaning up.");
//                cleanupRoom(roomId);
//            } else {
//                Player remainingPlayer = room.getPlayers().get(0);
//                if ("playing".equals(room.getStatus())) { // Xử lý thua nếu đang chơi
//                    String winnerColor = remainingPlayer.getColor();
//                    Map<String, Object> endData = new HashMap<>();
//                    endData.put("type", "end_game");
//                    endData.put("winner", winnerColor);
//                    endData.put("reason", "opponent_disconnected");
//                    if(remainingPlayer.getConnection().isOpen()) {
//                        remainingPlayer.getConnection().send(gson.toJson(endData));
//                    }
//                    cleanupRoom(roomId);
//                } else { // Phòng chờ hoặc kết thúc, chỉ thông báo
//                    Map<String, Object> disconnectData = new HashMap<>();
//                    disconnectData.put("type", "player_disconnected"); // Type riêng
//                    disconnectData.put("disconnectedPlayerName", player.getPlayerName());
//                    if(remainingPlayer.getConnection().isOpen()) {
//                        remainingPlayer.getConnection().send(gson.toJson(disconnectData));
//                    }
//                    // Nếu phòng 'waiting', xóa luôn
//                    if ("waiting".equals(room.getStatus())) {
//                        cleanupRoom(roomId);
//                    }
//                }
//            }
//        }
//    }
//
//    private void handleGetHistory(WebSocket webSocket, Map<String, Object> data) {
//        // 1. TẠO HOẶC LẤY DANH SÁCH LỊCH SỬ THỰC TẾ
//        // Đây là ví dụ về dữ liệu giả định (placeholder):
//        List<Map<String, Object>> historyList = new ArrayList<>();
//
//        // Ví dụ về một trận đấu (Giả sử bạn có một lớp MatchResult hoặc Map<String, Object> với các trường:
//        // playerX, playerO, winner, date)
//        Map<String, Object> match1 = new HashMap<>();
//        match1.put("playerX", "Alice");
//        match1.put("playerO", "Bob");
//        match1.put("winner", "Alice");
//        match1.put("date", new Date().getTime()); // Sử dụng timestamp
//        historyList.add(match1);
//
//        Map<String, Object> match2 = new HashMap<>();
//        match2.put("playerX", "Charlie");
//        match2.put("playerO", "David");
//        match2.put("winner", "David");
//        match2.put("date", new Date().getTime() - 3600000); // 1 giờ trước
//        historyList.add(match2);
//
//        // 2. CHUẨN BỊ PHẢN HỒI THEO ĐỊNH DẠNG CLIENT MONG MUỐN
//        Map<String, Object> response = new HashMap<>();
//        response.put("type", "history_list"); // <-- Đã sửa: Khớp với data.type === "history_list"
//        response.put("history", historyList); // <-- Đã sửa: Khớp với renderHistoryList(data.history)
//        webSocket.send(gson.toJson(response));
//    }
//
//    public void shutdown() {
//        try {
//            this.stop();
//            executorService.shutdown();
//            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//        }
//    }
//
//    public static void main(String[] args) {
//        int port = 8080;
//        ChessServer server = new ChessServer(port);
//
//        // Add shutdown hook
//        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
//
//        server.start();
//        System.out.println("WebSocket Chess Server chạy tại ws://localhost:" + port);
//    }
//}