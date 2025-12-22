package com.chessgame.server;

import com.database.server.DAO.userDAO;
import com.database.server.Entity.friends;
import com.database.server.Entity.user;
import com.database.server.Service.friendsService;
import com.database.server.Service.userService;
import com.database.server.Utils.JwtConfig;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public class NioWebSocketServer implements Runnable {
    private final int port;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final ManualThreadPool threadPool;
    private final Gson gson = new Gson();
    private final Map<SocketChannel, Player> connectionPlayerMap = new ConcurrentHashMap<>();
    private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final Map<Long, ConcurrentLinkedQueue<Player>> waitingQueuesByTime = new ConcurrentHashMap<>();
    private final Map<SocketChannel, Object> writeLocks = new ConcurrentHashMap<>();
    private final Queue<SocketChannel> disconnectQueue = new ConcurrentLinkedQueue<>();
    private static final String STOCKFISH_PATH = "D:\\Programing\\PBL4_ChessGame/src/main/resources/stockfish/stockfish-windows.exe";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final EntityManagerFactory emf;
    private final userService userService;
    private final friendsService friendsService;
    private final Map<String, ScheduledFuture<?>> disconnectionTimers = new ConcurrentHashMap<>();


    public NioWebSocketServer(int port) throws IOException {
        this.port = port;

        int cores = Runtime.getRuntime().availableProcessors();
        this.threadPool = new ManualThreadPool(cores);
        System.out.println("HĐH: Khởi tạo Thread Pool với " + cores + " luồng worker.");

        this.emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        this.userService = new userService(emf);
        this.friendsService = new friendsService(emf);

        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.bind(new InetSocketAddress(port));
        this.serverChannel.configureBlocking(false);

        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        startHeartbeatAndTimeoutTask();
        System.out.println("Server đang chạy trên cổng " + port);
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (!key.isValid()) {
                        iter.remove();
                        continue;
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    iter.remove();
                }
                SocketChannel clientToDisconnect;
                while ((clientToDisconnect = disconnectQueue.poll()) != null) {
                    handleDisconnect(clientToDisconnect);
                }
            } catch (IOException e) {
                System.err.println("Lỗi Event Loop: " + e.getMessage());
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);

        ConnectionAttachment attachment = new ConnectionAttachment();
        attachment.stagingBuffer = ByteBuffer.allocate(8192);
        attachment.lastActiveTime = System.currentTimeMillis();

        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ, attachment);
        attachment.key = clientKey;

        System.out.println("Client mới: " + client.getRemoteAddress());
    }

    private byte[] tryGetCompleteFrame(ByteBuffer buffer) {
        buffer.mark();

        if (buffer.remaining() < 2) {
            buffer.reset();
            return null;
        }

        byte b0 = buffer.get();
        byte b1 = buffer.get();

        long payloadLength = (b1 & 0x7F);
        int headerLength = 2;
        int maskLength = (b1 & 0x80) != 0 ? 4 : 0;

        if (payloadLength == 126) {
            if (buffer.remaining() < 2) {
                buffer.reset();
                return null;
            }
            payloadLength = buffer.getShort() & 0xFFFF;
            headerLength = 4;
        } else if (payloadLength == 127) {
            if (buffer.remaining() < 8) {
                buffer.reset();
                return null;
            }
            payloadLength = buffer.getLong();
            headerLength = 10;
        }

        int totalFrameSize = headerLength + maskLength + (int) payloadLength;
        if (buffer.remaining() < (totalFrameSize - headerLength)) {
            buffer.reset();
            return null;
        }

        buffer.reset();
        byte[] completeFrame = new byte[totalFrameSize];
        buffer.get(completeFrame);

        return completeFrame;
    }

    private void handleRead(SelectionKey key) {
        if (!key.isValid()) {
            return;
        }
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionAttachment attachment = (ConnectionAttachment) key.attachment();
        attachment.lastActiveTime = System.currentTimeMillis();

        key.interestOps(0);

        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int bytesRead;

        try {
            bytesRead = client.read(buffer);

            if (bytesRead == -1) {
                handleDisconnect(client);
                return;
            }

            buffer.flip();
            attachment.stagingBuffer.put(buffer);
            attachment.stagingBuffer.flip();

            if (!attachment.isHandshakeDone) {
                byte[] data = new byte[attachment.stagingBuffer.remaining()];
                attachment.stagingBuffer.get(data);

                if (doHandshake(client, new String(data, StandardCharsets.UTF_8))) {
                    attachment.isHandshakeDone = true;
                }
                attachment.stagingBuffer.compact();
            }
            else {
                while (true) {
                    byte[] completeFrame = tryGetCompleteFrame(attachment.stagingBuffer);

                    if (completeFrame != null) {
                        String jsonMessage = decodeWebSocketFrame(completeFrame);

                        if (jsonMessage != null) {
                            final String finalJsonMessage = jsonMessage;
                            threadPool.submit(() -> {
                                handleGameLogic(client, attachment, finalJsonMessage);
                            });
                        }
                    } else {
                        break;
                    }
                }
                attachment.stagingBuffer.compact();
            }

            if (client.isOpen()) {
                selector.wakeup();
                key.interestOps(SelectionKey.OP_READ);
            }

        } catch (SocketException e) {
            handleDisconnect(client);
        } catch (IOException e) {
            handleDisconnect(client);
        } catch (Exception e) {
            e.printStackTrace();
            handleDisconnect(client);
        }
    }


    private void sendCloseFrame(SocketChannel client) {
        try {
            if (client.isOpen()) {
                ByteBuffer closeFrame = ByteBuffer.allocate(2);
                closeFrame.put((byte) 0x88);
                closeFrame.put((byte) 0x00);
                closeFrame.flip();
                client.write(closeFrame);
            }
        } catch (IOException e) {
            // Bỏ qua
        }
    }

    private boolean doHandshake(SocketChannel client, String rawRequest) throws Exception {
        String key = "";
        String[] lines = rawRequest.split("\r\n");
        for (String line : lines) {
            if (line.startsWith("Sec-WebSocket-Key: ")) {
                key = line.substring(19);
                break;
            }
        }
        if (key.isEmpty()) return false;

        String magicString = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest((key + magicString).getBytes(StandardCharsets.UTF_8));
        String acceptKey = Base64.getEncoder().encodeToString(hash);

        String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                "\r\n";

        client.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
        return true;
    }

    private String decodeWebSocketFrame(byte[] frame) throws IOException {
        if (frame.length < 2) {
            return null;
        }

        byte opcode = (byte) (frame[0] & 0x0F);

        if (opcode == 0x8) {
            throw new IOException("Client requested close");
        }

        if (opcode == 0x9) {
            return "__PING__";
        }

        if (opcode == 0xA) {
            return "__PONG__";
        }

        if (opcode != 0x1) {
            return null;
        }

        boolean isMasked = (frame[1] & 0x80) != 0;
        long payloadLength = (frame[1] & 0x7F);
        int offset = 2;

        if (payloadLength == 126) {
            if (frame.length < 4) return null;
            payloadLength = ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
            offset = 4;
        } else if (payloadLength == 127) {
            if (frame.length < 10) return null;
            payloadLength = 0;
            for (int i = 0; i < 8; i++) {
                payloadLength = (payloadLength << 8) | (frame[2 + i] & 0xFF);
            }
            offset = 10;
        }

        if (frame.length < offset + (isMasked ? 4 : 0) + payloadLength) {
            return null;
        }

        if (!isMasked) {
            throw new IOException("Client must mask frames");
        }

        byte[] mask = new byte[4];
        System.arraycopy(frame, offset, mask, 0, 4);
        offset += 4;

        byte[] payload = new byte[(int) payloadLength];
        System.arraycopy(frame, offset, payload, 0, (int) payloadLength);

        byte[] unmaskedPayload = new byte[(int) payloadLength];
        for (int i = 0; i < payloadLength; i++) {
            unmaskedPayload[i] = (byte) (payload[i] ^ mask[i % 4]);
        }

        return new String(unmaskedPayload, StandardCharsets.UTF_8);
    }

    private byte[] encodeWebSocketFrame(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        int length = payload.length;

        ByteBuffer frame;
        if (length < 126) {
            frame = ByteBuffer.allocate(2 + length);
            frame.put((byte) 0x81);
            frame.put((byte) length);
        } else if (length < 65536) {
            frame = ByteBuffer.allocate(4 + length);
            frame.put((byte) 0x81);
            frame.put((byte) 126);
            frame.put((byte) ((length >> 8) & 0xFF));
            frame.put((byte) (length & 0xFF));
        } else {
            frame = ByteBuffer.allocate(10 + length);
            frame.put((byte) 0x81);
            frame.put((byte) 127);
            for (int i = 7; i >= 0; i--) {
                frame.put((byte) ((length >> (8 * i)) & 0xFF));
            }
        }

        frame.put(payload);
        frame.flip();
        return frame.array();
    }

    private void sendPongFrame(SocketChannel client) {
        try {
            if (client.isOpen()) {
                ByteBuffer pongFrame = ByteBuffer.allocate(2);
                pongFrame.put((byte) 0x8A);
                pongFrame.put((byte) 0x00);
                pongFrame.flip();
                client.write(pongFrame);
            }
        } catch (IOException e) {
            handleDisconnect(client);
        }
    }

    private static class ConnectionAttachment {
        public boolean isHandshakeDone = false;
        public Player player = null;
        public SelectionKey key = null;
        public ByteBuffer stagingBuffer = ByteBuffer.allocate(8192);
        public long lastActiveTime;
    }

    private void handleGameLogic(SocketChannel client, ConnectionAttachment attachment, String jsonMessage) {
        if (jsonMessage == null) {
            return;
        }

        if ("__PING__".equals(jsonMessage)) {
            sendPongFrame(client);
            return;
        }

        if ("__PONG__".equals(jsonMessage)) {
            return;
        }

        try {
            Map<String, Object> data = gson.fromJson(jsonMessage, Map.class);
            if (data == null || !data.containsKey("type")) {
                sendErrorMessage(client, "Invalid message format");
                return;
            }

            String type = (String) data.get("type");

            Player player = attachment.player;
            if (player == null) {
                player = connectionPlayerMap.get(client);
            }

            if (player == null && !"auth".equals(type)) {
                sendErrorMessage(client, "Cần phải xác thực (auth) trước.");
                return;
            }
            switch (type) {
                case "auth":
                    handleAuthentication(client, attachment, data);
                    break;
                case "join":
                    handlePlayerJoin(player, data);
                    break;
                case "create_room":
                    handleCreateRoom(player, data);
                    break;
                case "join_room":
                    handleJoinRoom(player, data);
                    break;
                case "watch_room":
                    handleWatchRoom(player, data);
                    break;
                case "leave_room":
                    handleLeaveRoom(player, data);
                    break;
                case "move_request":
                    handleGameMove(player, data);
                    break;
                case "chat":
                    handleChatMessage(player, data);
                    break;
                case "get_rooms":
                    handleGetRooms(player);
                    break;
                case "get_history":
                    handleGetHistory(player, data);
                    break;
                case "cancel_matchmaking":
                    hanleCancelMatchMaking(player, data);
                    break;
                case "draw_request":
                    handleDrawRequest(player, data);
                    break;
                case "draw_response":
                    handleDrawResponse(player, data);
                    break;
                case "get_valid_moves":
                    handleGetValidMove(player, data);
                    break;
                case "resign":
                    handleResign(player, data);
                    break;
                case "rematch_request":
                    handleRematchRequest(player, data);
                    break;
                case "create_ai_game":
                    handleCreateAiGame(player, data);
                    break;
                case "take_back_request":
                    handleTakeBackRequest(player, data);
                    break;
                case "search_users":
                    handleSearchUsers(player, data);
                    break;
                case "get_friends":
                    handleGetFriends(player);
                    break;
                case "friend_request":
                    handleFriendRequest(player, data);
                    break;
                case "accept_friend":
                    handleAcceptFriend(player, data);
                    break;
                case "reject_friend":
                    handleRejectFriend(player, data);
                    break;
                case "invite_friend":
                    handleInviteFriend(player, data);
                    break;
                case "invite_response":
                    handleInviteResponse(player, data);
                    break;
                case "pong":
                    break;
                default:
                    sendErrorMessage(client, "Unknown message type");
            }

        } catch (com.google.gson.JsonSyntaxException e) {
            sendErrorMessage(client, "Invalid JSON format");
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorMessage(client, "Lỗi xử lý logic: " + e.getMessage());
        }
    }

    private void handleCreateAiGame(Player player, Map<String, Object> data) {
        if (player == null) return;

        try {
            String roomId = generateUniqueRoomId();
            long timeControl = 600000;
            if (data.get("timeControl") instanceof Number) {
                timeControl = ((Number) data.get("timeControl")).longValue();
            }

            int elo = 1350;
            if (data.get("elo") instanceof Number) {
                elo = ((Number) data.get("elo")).intValue();
            }
            
            String preferredColor = (String) data.get("color"); // "white", "black", or "random"

            GameRoom room = new GameRoom(roomId, timeControl, this);

            StockfishEngine engine = new StockfishEngine(STOCKFISH_PATH);
            engine.setElo(elo);
            room.setStockfishEngine(engine);

            Player botPlayer = new Player("STOCKFISH_AI_" + roomId, "Stockfish (Elo " + elo + ")", null);

            boolean playerIsWhite;
            if ("white".equals(preferredColor)) {
                playerIsWhite = true;
            } else if ("black".equals(preferredColor)) {
                playerIsWhite = false;
            } else {
                playerIsWhite = Math.random() < 0.5;
            }

            if (playerIsWhite) {
                player.setColor("white");
                botPlayer.setColor("black");
            } else {
                player.setColor("black");
                botPlayer.setColor("white");
            }

            room.addPlayer(player);
            room.addPlayer(botPlayer);
            gameRooms.put(roomId, room);

            sendMessage(player.getConnection(), Map.of("type", "room_created", "roomId", roomId, "color", player.getColor(), "isAiGame", true));
            sendMessage(player.getConnection(), Map.of("type", "room_joined", "roomId", roomId, "color", player.getColor(), "isAiGame", true));

            startGame(room);

            if ("white".equals(botPlayer.getColor())) {
                triggerStockfishMove(room, botPlayer);
            }

        } catch (IOException e) {
            sendErrorMessage(player.getConnection(), "Không thể khởi động AI Engine.");
        }
    }
    
    private void handleTakeBackRequest(Player player, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        
        if (room == null) {
            sendErrorMessage(player.getConnection(), "Phòng không tồn tại.");
            return;
        }
        
        if (room.getStockfishEngine() == null) {
            sendErrorMessage(player.getConnection(), "Chỉ có thể đi lại khi chơi với máy.");
            return;
        }
        
        if (!room.getStatus().equals("playing")) {
            sendErrorMessage(player.getConnection(), "Ván đấu không đang diễn ra.");
            return;
        }
        
        // Check if it's player's turn (cannot take back while AI is thinking)
        if (!room.getCurrentTurn().equals(player.getColor())) {
             sendErrorMessage(player.getConnection(), "Không thể đi lại khi máy đang suy nghĩ.");
             return;
        }

        boolean success = room.takeBackMove();
        
        if (success) {
            // Sync Stockfish internal board
            try {
                room.getStockfishEngine().setPosition(room.getValidator().toFen());
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "take_back_result");
            response.put("success", true);
            response.put("fen", room.getValidator().toFen());
            sendMessage(player.getConnection(), response);
        } else {
            sendMessage(player.getConnection(), Map.of("type", "take_back_result", "success", false, "message", "Không thể đi lại (chưa đủ nước đi)."));
        }
    }

    private void handleAuthentication(SocketChannel client, ConnectionAttachment attachment, Map<String, Object> data) {
        String token = (String) data.get("token");

        if (token == null || token.isEmpty()) {
            String guestName = "Guest_" + (new Random().nextInt(9000) + 1000);
            String guestId = "guest_" + UUID.randomUUID();

            Player guestPlayer = new Player(guestId, guestName, client);
            connectionPlayerMap.put(client, guestPlayer);
            attachment.player = guestPlayer;

            sendMessage(client, Map.of("type", "player_info", "playerId", guestPlayer.getPlayerId(), "playerName", guestPlayer.getPlayerName(), "isGuest", true));
            return;
        }

        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(JwtConfig.JWT_SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);

            Claims payload = claimsJws.getBody();
            String userId = payload.getSubject();
            String username = payload.get("username", String.class);

            // Reconnection logic
            if (disconnectionTimers.containsKey(userId)) {
                ScheduledFuture<?> task = disconnectionTimers.remove(userId);
                if (task != null) task.cancel(false);
                
                System.out.println("Player " + username + " reconnected!");

                Player oldPlayer = null;
                SocketChannel oldSocket = null;
                for (Map.Entry<SocketChannel, Player> entry : connectionPlayerMap.entrySet()) {
                    if (entry.getValue().getPlayerId().equals(userId)) {
                        oldPlayer = entry.getValue();
                        oldSocket = entry.getKey();
                        break;
                    }
                }

                if (oldPlayer != null) {
                    connectionPlayerMap.remove(oldSocket);
                    connectionPlayerMap.put(client, oldPlayer);
                    oldPlayer.setConnection(client);
                    attachment.player = oldPlayer;

                    sendMessage(client, Map.of("type", "player_info", "playerId", oldPlayer.getPlayerId(), "playerName", oldPlayer.getPlayerName(), "isGuest", false));

                    GameRoom room = findRoomByPlayer(oldPlayer);
                    if (room != null) {
                        Map<String, Object> restoreData = new HashMap<>();
                        restoreData.put("type", "game_state_restore");
                        restoreData.put("fen", room.getValidator().toFen());
                        restoreData.put("color", oldPlayer.getColor());
                        restoreData.put("roomId", room.getRoomId());
                        restoreData.put("whiteTime", room.getWhiteTimeMs());
                        restoreData.put("blackTime", room.getBlackTimeMs());
                        restoreData.put("currentTurn", room.getCurrentTurn());
                        restoreData.put("moveHistory", room.getMoveHistory());
                        
                        Player p1 = room.getPlayerByColor("white");
                        Player p2 = room.getPlayerByColor("black");
                        restoreData.put("playerWhite", Map.of("id", p1.getPlayerId(), "name", p1.getPlayerName()));
                        restoreData.put("playerBlack", Map.of("id", p2.getPlayerId(), "name", p2.getPlayerName()));

                        sendMessage(client, restoreData);

                        Player opponent = room.getOpponent(oldPlayer);
                        if (opponent != null && opponent.getConnection() != null && opponent.getConnection().isOpen()) {
                            sendMessage(opponent.getConnection(), Map.of("type", "opponent_reconnect_alert"));
                        }
                    }
                    return;
                }
            }

            // Normal authentication
            Player authenticatedPlayer = new Player(userId, username, client);
            connectionPlayerMap.put(client, authenticatedPlayer);
            attachment.player = authenticatedPlayer;

            userService.updateStatus(Integer.parseInt(userId), "Online");

            sendMessage(client, Map.of("type", "player_info", "playerId", authenticatedPlayer.getPlayerId(), "playerName", authenticatedPlayer.getPlayerName(), "isGuest", false));

        } catch (JwtException e) {
            sendErrorMessage(client, "Token không hợp lệ: " + e.getMessage());
        }
    }

    private String generateUniqueRoomId() {
        Random random = new Random();
        String roomId;
        do {
            int number = random.nextInt(1000000);
            roomId = String.format("%06d", number);
        } while (gameRooms.containsKey(roomId));
        return roomId;
    }

    public void notifyRoomPlayers(GameRoom room, String messagesType, Map<String, Object> data) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", messagesType);
        message.putAll(data);

        for (Player player : room.getPlayers()) {
            if (player.getConnection() != null && player.getConnection().isOpen()) {
                sendMessage(player.getConnection(), message);
            }
        }
        for (Player spectator : room.getSpectators()) {
            if (spectator.getConnection() != null && spectator.getConnection().isOpen()) {
                sendMessage(spectator.getConnection(), message);
            }
        }
    }

    private void startGame(GameRoom room) {
        room.setStatus("playing");
        room.setCurrentTurn("white");
        // room.getValidator().resetBoard(); // Resetting is done in constructor, maybe not needed here unless for rematch
        Map<String, Object> gameStartData = new HashMap<>();
        gameStartData.put("gameState", room.getValidator().toFen());
        gameStartData.put("currentTurn", "white");
        gameStartData.put("initialTimeMs", room.getInitialTimeMs());
        gameStartData.put("isAiGame", room.getStockfishEngine() != null); // Add isAiGame flag

        Player p1 = room.getPlayerByColor("white");
        Player p2 = room.getPlayerByColor("black");
        if (p1 != null && p2 != null) {
            gameStartData.put("playerWhite", Map.of("id", p1.getPlayerId(), "name", p1.getPlayerName()));
            gameStartData.put("playerBlack", Map.of("id", p2.getPlayerId(), "name", p2.getPlayerName()));
        }

        room.startTimer();
        notifyRoomPlayers(room, "game_start", gameStartData);
    }

    private void handlePlayerJoin(Player player, Map<String, Object> data) {
        if (player == null) return;
        Object timeControlObj = data.get("timeControl");
        Long preferredTime;
        if (timeControlObj instanceof Number) {
            preferredTime = ((Number) timeControlObj).longValue();
        } else {
            sendErrorMessage(player.getConnection(), "Thiếu hoặc sai định dạng timeControl.");
            return;
        }
        player.setPreferredTimeMs(preferredTime);
        ConcurrentLinkedQueue<Player> queue = waitingQueuesByTime.computeIfAbsent(
                preferredTime,
                k -> new ConcurrentLinkedQueue<>()
        );
        Player opponent = queue.poll();

        if (opponent != null) {
            final Player p1 = player;
            final Player p2 = opponent;

            threadPool.submit(() -> {
                startNewMatch(p1, p2, preferredTime);
            });

        } else {
            queue.offer(player);
        }
    }

    private void startNewMatch(Player player1, Player player2, long timeControlMs) {
        String roomId = generateUniqueRoomId();
        GameRoom room = new GameRoom(roomId, timeControlMs, this);

        if (Math.random() < 0.5) {
            player1.setColor("white");
            player2.setColor("black");
        } else {
            player1.setColor("black");
            player2.setColor("white");
        }

        room.addPlayer(player1);
        room.addPlayer(player2);
        gameRooms.put(roomId, room);
        for (Player player : room.getPlayers()) {
            sendMessage(player.getConnection(), Map.of("type", "room_info", "roomId", roomId));
            sendMessage(player.getConnection(), Map.of("type", "color", "color", player.getColor()));
        }

        startGame(room);
    }

    private void handleCreateRoom(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = generateUniqueRoomId();
        GameRoom room = new GameRoom(roomId, 600000, this);
        player.setColor("white");
        room.addPlayer(player);
        gameRooms.put(roomId, room);
        sendMessage(player.getConnection(), Map.of("type", "room_created", "roomId", roomId, "color", player.getColor()));
        broadcastRoomsList();
    }

    private void handleJoinRoom(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = (String) data.get("roomId");
        if (roomId == null || roomId.isEmpty()) {
            sendErrorMessage(player.getConnection(), "Room ID is required!");
            return;
        }
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            sendErrorMessage(player.getConnection(), "Room does not exist!");
            return;
        }
        if (room.isFull()) {
            sendErrorMessage(player.getConnection(), "Room is already full!");
            return;
        }
        player.setColor("black");
        room.addPlayer(player);

        sendMessage(player.getConnection(), Map.of("type", "room_joined", "roomId", roomId, "color", player.getColor(), "gameState", room.getValidator().toFen()));

        notifyRoomPlayers(room, "player_joined", Map.of("playerName", player.getPlayerName(), "color", player.getColor()));

        if (room.isFull()) {
            startGame(room);
        }
        broadcastRoomsList();
    }

    private void handleWatchRoom(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = (String) data.get("roomId");
        if (roomId == null || roomId.isEmpty()) {
            sendErrorMessage(player.getConnection(), "Room ID is required!");
            return;
        }
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            sendErrorMessage(player.getConnection(), "Room does not exist!");
            return;
        }
        
        room.addSpectator(player);
        
        Map<String, Object> watchData = new HashMap<>();
        watchData.put("type", "room_watched");
        watchData.put("roomId", roomId);
        watchData.put("gameState", room.getValidator().toFen());
        watchData.put("currentTurn", room.getCurrentTurn());
        watchData.put("whiteTime", room.getWhiteTimeMs());
        watchData.put("blackTime", room.getBlackTimeMs());
        watchData.put("moveHistory", room.getMoveHistory());
        
        Player p1 = room.getPlayerByColor("white");
        Player p2 = room.getPlayerByColor("black");
        if (p1 != null) watchData.put("playerWhite", Map.of("id", p1.getPlayerId(), "name", p1.getPlayerName()));
        if (p2 != null) watchData.put("playerBlack", Map.of("id", p2.getPlayerId(), "name", p2.getPlayerName()));
        
        sendMessage(player.getConnection(), watchData);
    }

    private void handleLeaveRoom(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        if (room != null) {
            if (room.getSpectators().contains(player)) {
                room.removeSpectator(player);
            } else {
                room.removePlayer(player);
                if (room.isEmpty()) {
                    gameRooms.remove(roomId);
                } else {
                    Map<String, Object> leftData = new HashMap<>();
                    leftData.put("leftPlayer", player.getPlayerName());
                    leftData.put("reason", "Người chơi rời khỏi phòng!");
                    leftData.put("winner", room.getOpponent(player).getColor());
                    notifyRoomPlayers(room, "player_left", leftData);
                }
            }
            broadcastRoomsList();
        }
    }

    private void handleGameMove(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        if (room == null) return;

        String from = data.get("from").toString();
        String to = data.get("to").toString();
        Character promo = data.get("promotion") == null ? null : ((String)data.get("promotion")).charAt(0);

        boolean moveSuccess = processMove(room, player, from, to, promo);

        if (moveSuccess && "playing".equals(room.getStatus())) {
            Player opponent = room.getOpponent(player);
            if (opponent.getConnection() == null && room.getStockfishEngine() != null) {
                triggerStockfishMove(room, opponent);
            }
        }
    }

    private boolean processMove(GameRoom room, Player player, String from, String to, Character promo) {
        if (!"playing".equals(room.getStatus())) return false;
        if (!room.getCurrentTurn().equals(player.getColor())) {
            if (player.getConnection() != null)
                sendErrorMessage(player.getConnection(), "Chưa tới lượt của bạn!");
            return false;
        }

        ChessValidator.MoveResult moveResult = room.getValidator().validateMove(from, to, player.getColor(), promo);

        if (!moveResult.isValid) {
            if (player.getConnection() != null) {
                sendMessage(player.getConnection(), Map.of("type", "move_result", "result", false, "message", moveResult.message));
            }
            return false;
        }

        String nextTurn = player.getColor().equals("white") ? "black" : "white";
        room.setCurrentTurn(nextTurn);

        if (room.getStockfishEngine() != null) {
            try {
                room.getStockfishEngine().setPosition(room.getValidator().toFen());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String fen = room.getValidator().toFen();
        boolean isNextPlayerInCheck = room.getValidator().isKingInCheck(nextTurn, room.getValidator().getBoard());

        Map<String, Object> fenData = new HashMap<>();
        fenData.put("type", "move_result");
        fenData.put("result", true);
        fenData.put("fen", fen);
        fenData.put("lastMove", Map.of("from", from, "to", to));
        fenData.put("isCheck", isNextPlayerInCheck);
        
        room.addMoveToHistory(fenData);

        notifyRoomPlayers(room, "move_result", fenData);

        if (moveResult.winner != null) {
            notifyRoomPlayers(room, "end_game", Map.of("winner", moveResult.winner));
            room.setStatus("finished");
            room.shutdownTimerService();
        }

        return true;
    }

    private void triggerStockfishMove(GameRoom room, Player botPlayer) {
        threadPool.submit(() -> {
            try {
                int thinkTime = 2000;

                String bestMoveUCI = room.getStockfishEngine().getBestMove(thinkTime);

                if (bestMoveUCI == null) {
                    return;
                }

                String from = bestMoveUCI.substring(0, 2);
                String to = bestMoveUCI.substring(2, 4);
                Character promo = null;

                if (bestMoveUCI.length() > 4) {
                    promo = bestMoveUCI.charAt(4);
                }

                processMove(room, botPlayer, from, to, promo);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleChatMessage(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = (String) data.get("roomId");
        String message = (String) data.get("message");
        GameRoom room = gameRooms.get(roomId);
        if (room != null) {
            notifyRoomPlayers(room, "chat", Map.of("playerName", player.getPlayerName(), "message", message, "timestamp", System.currentTimeMillis()));
        }
    }

    private void handleGetRooms(Player player) {
        List<Map<String, Object>> roomList = new ArrayList<>();
        for (GameRoom room : gameRooms.values()) {
            if (room.getStatus().equals("waiting") || room.getStatus().equals("playing")) {
                roomList.add(Map.of("roomId", room.getRoomId(), "playerCount", room.getPlayers().size(), "status", room.getStatus()));
            }
        }
        sendMessage(player.getConnection(), Map.of("type", "room_list", "rooms", roomList));
    }

    private void broadcastRoomsList() {
        List<Map<String, Object>> roomsList = new ArrayList<>();
        for (GameRoom room : gameRooms.values()) {
            if (room.getStatus().equals("waiting") || room.getStatus().equals("playing")) {
                roomsList.add(Map.of("roomId", room.getRoomId(), "playerCount", room.getPlayers().size(), "status", room.getStatus()));
            }
        }
        Map<String, Object> broadcast = Map.of("type", "room_update", "rooms", roomsList);
        for (SocketChannel conn : connectionPlayerMap.keySet()) {
            if (conn.isOpen()) {
                sendMessage(conn, broadcast);
            }
        }
    }

    private void hanleCancelMatchMaking(Player player, Map<String, Object> data) {
        if (player == null || player.getPreferredTimeMs() == null) return;

        ConcurrentLinkedQueue<Player> queue = waitingQueuesByTime.get(player.getPreferredTimeMs());

        if (queue != null) {
            boolean removed = queue.remove(player);
        }
    }

    private void handleDrawRequest(Player player, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        if (room == null) return;

        if (room.getIsDrawOffered() != null) {
            sendErrorMessage(player.getConnection(), "Đã có lời cầu hòa trong phòng này!");
            return;
        }
        Player oponentPlayer = room.getOpponent(player);
        if (oponentPlayer == null || !oponentPlayer.getConnection().isOpen()) {
            sendErrorMessage(player.getConnection(), "Đối thủ đã offline.");
            return;
        }

        room.setIsDrawOffered(player.getColor());
        sendMessage(oponentPlayer.getConnection(), Map.of("type", "draw_offer"));
    }

    private void handleDrawResponse(Player player, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        Object acceptedObj = data.get("accepted");
        if (room == null || acceptedObj == null || !(acceptedObj instanceof Boolean)) {
            sendErrorMessage(player.getConnection(), "Phản hồi cầu hòa không hợp lệ.");
            return;
        }
        boolean accepted = (Boolean) acceptedObj;
        String opponentColor = player.getColor().equals("white") ? "black" : "white";
        if (!opponentColor.equals(room.getIsDrawOffered())) {
            sendErrorMessage(player.getConnection(), "Không có lời cầu hòa nào đang chờ bạn.");
            return;
        }

        Player offeringPlayer = room.getPlayerByColor(opponentColor);
        if (accepted) {
            notifyRoomPlayers(room, "end_game", Map.of("winner", "draw", "reason", "agreement"));
            room.setStatus("finished");
            room.stopTimer();
        } else {
            room.setIsDrawOffered(null);
            if (offeringPlayer != null && offeringPlayer.getConnection().isOpen()) {
                sendMessage(offeringPlayer.getConnection(), Map.of("type", "draw_rejected"));
            }
        }
    }

    private void handleGetValidMove(Player player, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        String square = (String) data.get("square");
        GameRoom room = gameRooms.get(roomId);
        if (room == null || !room.getStatus().equals("playing") || !room.getCurrentTurn().equals(player.getColor())) {
            return;
        }
        List<String> validMoves = room.getValidator().getValidMovesForSquare(square);
        sendMessage(player.getConnection(), Map.of("type", "valid_moves", "square", square, "moves", validMoves));
    }

    private void handleResign(Player player, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        if (room == null || !"playing".equals(room.getStatus()) || !room.getPlayers().contains(player)) {
            sendErrorMessage(player.getConnection(), "Không thể đầu hàng lúc này.");
            return;
        }
        String winnerColor = player.getColor().equals("white") ? "black" : "white";
        notifyRoomPlayers(room, "end_game", Map.of("winner", winnerColor, "reason", "resignation"));
        room.setStatus("finished");
        room.stopTimer();
    }

    private void handleRematchRequest(Player player, Map<String, Object> data) {
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        if (room == null || !"finished".equals(room.getStatus()) || !room.getPlayers().contains(player)) {
            sendErrorMessage(player.getConnection(), "Không thể tái đấu lúc này.");
            return;
        }
        Player opponent = room.getOpponent(player);
        if (opponent == null || opponent.getConnection() == null || !opponent.getConnection().isOpen()) {
            sendMessage(player.getConnection(), Map.of("type", "rematch_unavailable", "reason", "Đối thủ đã offline."));
            return;
        }

        String playerColor = player.getColor();
        String opponentColor = opponent.getColor();

        if (playerColor.equals(room.getRematchRequestedByColor())) return;

        if (opponentColor.equals(room.getRematchRequestedByColor())) {
            room.swapPlayerColors();
            room.resetForRematch();

            Player newWhitePlayer = room.getPlayerByColor("white");
            Player newBlackPlayer = room.getPlayerByColor("black");

            Map<String, Object> gameStartData = new HashMap<>();
            gameStartData.put("gameState", room.getValidator().toFen());
            gameStartData.put("currentTurn", "white");
            gameStartData.put("initialTimeMs", room.getInitialTimeMs());
            gameStartData.put("playerWhite", Map.of("id", newWhitePlayer.getPlayerId(), "name", newWhitePlayer.getPlayerName()));
            gameStartData.put("playerBlack", Map.of("id", newBlackPlayer.getPlayerId(), "name", newBlackPlayer.getPlayerName()));

            sendMessage(newWhitePlayer.getConnection(), Map.of("type", "color", "color", "white"));
            sendMessage(newBlackPlayer.getConnection(), Map.of("type", "color", "color", "black"));
            notifyRoomPlayers(room, "game_start", gameStartData);
            room.startTimer();
        } else {
            room.setRematchRequestedByColor(playerColor);
            sendMessage(opponent.getConnection(), Map.of("type", "rematch_offer", "offeringPlayer", player.getPlayerName()));
            sendMessage(player.getConnection(), Map.of("type", "rematch_offer_sent"));
        }
    }

    private void handleGetHistory(Player player, Map<String, Object> data) {
        List<Map<String, Object>> historyList = new ArrayList<>();
        sendMessage(player.getConnection(), Map.of("type", "history_list", "history", historyList));
    }

    private GameRoom findRoomByPlayer(Player player) {
        if (player == null) return null;
        for (GameRoom room : gameRooms.values()) {
            if (room.getPlayers().contains(player)) {
                return room;
            }
            if (room.getSpectators().contains(player)) {
                return room;
            }
        }
        return null;
    }

    private void cleanupRoom(String roomId) {
        GameRoom removedRoom = gameRooms.remove(roomId);
        if (removedRoom != null) {
            removedRoom.stopTimer();
        }
    }

    public void handleTimeout(GameRoom room, String winnerColor) {
        if (room == null) return;
        Map<String, Object> endData = new HashMap<>();
        endData.put("winner", winnerColor);
        endData.put("reason", "timeout");
        notifyRoomPlayers(room, "end_game", endData);
        room.setStatus("finished");
        room.stopTimer();
    }

    private void handleDisconnect(SocketChannel client) {
        if (client == null) {
            return;
        }
        
        Player player = connectionPlayerMap.get(client);
        if (player != null) {
            GameRoom room = findRoomByPlayer(player);
            if (room != null && "playing".equals(room.getStatus())) {
                System.out.println("Player " + player.getPlayerName() + " disconnected during game. Waiting 60s...");
                
                Player opponent = room.getOpponent(player);
                if (opponent != null && opponent.getConnection() != null && opponent.getConnection().isOpen()) {
                    sendMessage(opponent.getConnection(), Map.of("type", "opponent_disconnect_alert"));
                }

                ScheduledFuture<?> task = scheduler.schedule(() -> {
                    System.out.println("Player " + player.getPlayerName() + " timed out. Ending game.");
                    handlePlayerDisconnectInRoom(room, player);
                    connectionPlayerMap.remove(client);
                    disconnectionTimers.remove(player.getPlayerId());
                    if (!player.getPlayerId().startsWith("guest_")) {
                        userService.updateStatus(Integer.parseInt(player.getPlayerId()), "Offline");
                    }
                }, 60, TimeUnit.SECONDS);
                
                disconnectionTimers.put(player.getPlayerId(), task);
                
            } else {
                connectionPlayerMap.remove(client);
                if (!player.getPlayerId().startsWith("guest_")) {
                    userService.updateStatus(Integer.parseInt(player.getPlayerId()), "Offline");
                }
            }
        }

        try {
            sendCloseFrame(client);
            SelectionKey key = client.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
            writeLocks.remove(client);
            client.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private void handlePlayerDisconnectInRoom(GameRoom room, Player player) {
        if (room == null || player == null) return;

        String status = room.getStatus();

        if ("playing".equals(status)) {
            Player opponent = room.getOpponent(player);
            if (opponent != null && opponent.getConnection() != null && opponent.getConnection().isOpen()) {
                String winnerColor = opponent.getColor();
                Map<String, Object> endData = new HashMap<>();
                endData.put("winner", winnerColor);
                endData.put("reason", "opponent_disconnected");
                endData.put("disconnectedPlayer", player.getPlayerName());
                sendMessage(opponent.getConnection(), Map.of("type", "end_game", "data", endData));
            }
            room.setStatus("finished");
            room.stopTimer();
        } else if ("waiting".equals(status)) {
            gameRooms.remove(room.getRoomId());
            broadcastRoomsList();
        }

        room.removePlayer(player);
        if (room.isEmpty()) {
            cleanupRoom(room.getRoomId());
        }
    }

    public void sendMessage(SocketChannel client, Object messageObject) {
        if (client == null || !client.isOpen()) {
            return;
        }

        Object lock = writeLocks.computeIfAbsent(client, k -> new Object());

        synchronized (lock) {
            try {
                String jsonMessage = gson.toJson(messageObject);
                byte[] responseFrame = encodeWebSocketFrame(jsonMessage);

                ByteBuffer buffer = ByteBuffer.wrap(responseFrame);
                while (buffer.hasRemaining()) {
                    client.write(buffer);
                }
            } catch (IOException e) {
                disconnectQueue.offer(client);
                selector.wakeup();
            }
        }
    }

    public void sendErrorMessage(SocketChannel client, String errorMessage) {
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("type", "error");
        errorMap.put("message", errorMessage);
        sendMessage(client, errorMap);
    }

    private void startHeartbeatAndTimeoutTask() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (SelectionKey key : selector.keys()) {
                if (key.channel() instanceof SocketChannel && key.isValid()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    ConnectionAttachment attachment = (ConnectionAttachment) key.attachment();
                    if (attachment != null && attachment.isHandshakeDone) {
                        if (now - attachment.lastActiveTime > 60000) {
                            disconnectQueue.offer(client);
                        } else if (now - attachment.lastActiveTime > 30000) {
                            sendMessage(client, Map.of("type", "ping"));
                        }
                    }
                }
            }
            selector.wakeup();
        }, 10, 10, TimeUnit.SECONDS);
    }

//    private void handleSearchUsers(Player player, Map<String, Object> data) {
//        String keyword = (String) data.get("keyword");
//        if (keyword == null || keyword.trim().isEmpty()) {
//            sendMessage(player.getConnection(), Map.of("type", "search_results", "users", new ArrayList<>()));
//            return;
//        }
//        userDAO dao = new userDAO(emf.createEntityManager());
//        List<user> users = dao.searchUsers(keyword);
//        List<Map<String, Object>> userDTOs = new ArrayList<>();
//        for (user u : users) {
//            Map<String, Object> dto = new HashMap<>();
//            dto.put("userId", u.getUserId());
//            dto.put("userName", u.getUserName());
//            dto.put("elo", u.getEloRating());
//            userDTOs.add(dto);
//        }
//        sendMessage(player.getConnection(), Map.of("type", "search_results", "users", userDTOs));
//    }

    // mới thay đổi
    private void handleSearchUsers(Player player, Map<String, Object> data) {
        String keyword = (String) data.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            sendMessage(player.getConnection(), Map.of("type", "search_results", "users", new ArrayList<>()));
            return;
        }

        int currentUserId = -1;
        Set<Integer> friendIds = new HashSet<>();
        Set<Integer> pendingIds = new HashSet<>(); // (Tùy chọn) Để check cả trạng thái đã gửi lời mời

        // 1. Lấy danh sách ID bạn bè hiện tại của người dùng
        if (!player.getPlayerId().startsWith("guest_")) {
            try {
                currentUserId = Integer.parseInt(player.getPlayerId());
                List<Map<String, Object>> myFriends = friendsService.getFriendsOfUser(currentUserId);

                for (Map<String, Object> f : myFriends) {
                    String status = (String) f.get("status");
                    int fid = (int) f.get("friend_id");

                    if ("accepted".equalsIgnoreCase(status)) {
                        friendIds.add(fid); // Đã là bạn bè
                    } else if ("pending".equalsIgnoreCase(status)) {
                        pendingIds.add(fid); // Đang chờ kết bạn
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        userDAO dao = new userDAO(emf.createEntityManager());
        List<user> users = dao.searchUsers(keyword);

        List<Map<String, Object>> userDTOs = new ArrayList<>();

        for (user u : users) {
            // Bỏ qua chính mình
            if (u.getUserId() == currentUserId) continue;

            Map<String, Object> dto = new HashMap<>();
            dto.put("userId", u.getUserId());
            dto.put("userName", u.getUserName());
            dto.put("elo", u.getEloRating());
            dto.put("avatarUrl", u.getAvatarUrl());

            // 2. Kiểm tra trạng thái bạn bè
            if (friendIds.contains(u.getUserId())) {
                dto.put("relationship", "friend"); // Đã là bạn
            } else if (pendingIds.contains(u.getUserId())) {
                dto.put("relationship", "pending"); // Đã gửi/nhận lời mời
            } else {
                dto.put("relationship", "none"); // Chưa kết bạn
            }

            userDTOs.add(dto);
        }

        sendMessage(player.getConnection(), Map.of("type", "search_results", "users", userDTOs));
    }

//    private void handleGetFriends(Player player) {
//        List<friends> friendsList = friendsService.getFriendsOfUser(Integer.parseInt(player.getPlayerId()));
//        List<Map<String, Object>> friendDTOs = new ArrayList<>();
//
//        for (friends f : friendsList) {
//            Map<String, Object> dto = new HashMap<>();
//            dto.put("friendship_id", f.getFriendshipId());
//            dto.put("status", f.getStatus());
//
//            user friendUser = (f.getUser1().getUserId() == Integer.parseInt(player.getPlayerId())) ? f.getUser2() : f.getUser1();
//
//            dto.put("friend_id", friendUser.getUserId());
//            dto.put("friend_name", friendUser.getUserName());
//            dto.put("friend_status", friendUser.getStatus());
//
//            friendDTOs.add(dto);
//        }
//
//        sendMessage(player.getConnection(), Map.of("type", "friends_list", "friends", friendDTOs));
//    }

    //mới
    private void handleGetFriends(Player player) {
        // 1. Kiểm tra Guest
        if (player.getPlayerId().startsWith("guest_")) {
            sendMessage(player.getConnection(), Map.of("type", "friends_list", "friends", new ArrayList<>()));
            return;
        }

        try {
            int userId = Integer.parseInt(player.getPlayerId());

            // 2. Gọi Service
            // ✅ SỬA LỖI: Khai báo đúng kiểu List<Map<String, Object>>
            // Service đã làm hết việc (lấy avatar, xác định sender_id) rồi.
            List<Map<String, Object>> friendsList = friendsService.getFriendsOfUser(userId);

            // 3. Gửi thẳng về Client (Không cần vòng lặp for nữa)
            sendMessage(player.getConnection(), Map.of("type", "friends_list", "friends", friendsList));

        } catch (NumberFormatException e) {
            System.err.println("Lỗi ID người chơi: " + player.getPlayerId());
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorMessage(player.getConnection(), "Lỗi lấy danh sách bạn bè: " + e.getMessage());
        }
    }

    private void handleFriendRequest(Player player, Map<String, Object> data) {
        int receiverId = ((Number) data.get("receiverId")).intValue();
        try {
            friendsService.sendFriendRequest(Integer.parseInt(player.getPlayerId()), receiverId);
            sendMessage(player.getConnection(), Map.of("type", "friend_request_sent", "receiverId", receiverId));
        } catch (Exception e) {
            sendErrorMessage(player.getConnection(), e.getMessage());
        }
    }

    private void handleAcceptFriend(Player player, Map<String, Object> data) {
        int friendshipId = ((Number) data.get("friendshipId")).intValue();
        if (friendsService.acceptFriendRequest(friendshipId)) {
            sendMessage(player.getConnection(), Map.of("type", "friend_request_accepted", "friendshipId", friendshipId));
        } else {
            sendErrorMessage(player.getConnection(), "Could not accept friend request.");
        }
    }

    private void handleRejectFriend(Player player, Map<String, Object> data) {
        int friendshipId = ((Number) data.get("friendshipId")).intValue();
        if (friendsService.rejectFriendRequest(friendshipId)) {
            sendMessage(player.getConnection(), Map.of("type", "friend_request_rejected", "friendshipId", friendshipId));
        } else {
            sendErrorMessage(player.getConnection(), "Could not reject friend request.");
        }
    }

//    private void handleInviteFriend(Player player, Map<String, Object> data) {
//        String friendId = String.valueOf(data.get("friendId"));
//        Player friend = getPlayerById(friendId);
//        if (friend != null && friend.getConnection() != null && friend.getConnection().isOpen()) {
//            if (findRoomByPlayer(friend) != null) {
//                sendErrorMessage(player.getConnection(), "Player is already in a game.");
//                return;
//            }
//            sendMessage(friend.getConnection(), Map.of("type", "game_invite", "fromPlayerId", player.getPlayerId(), "fromPlayerName", player.getPlayerName()));
//            sendMessage(player.getConnection(), Map.of("type", "invite_sent", "friendId", friendId));
//        } else {
//            sendErrorMessage(player.getConnection(), "Player is not online.");
//        }
//    }

    //mới
    private void handleInviteFriend(Player player, Map<String, Object> data) {
        String friendId = String.valueOf(data.get("friendId"));

        // Tìm người bạn trong danh sách kết nối
        Player friend = getPlayerById(friendId);

        if (friend != null && friend.getConnection() != null && friend.getConnection().isOpen()) {
            if (findRoomByPlayer(friend) != null) {
                sendErrorMessage(player.getConnection(), "Người chơi đang trong trận đấu khác.");
                return;
            }

            // 👇 LẤY AVATAR CỦA NGƯỜI MỜI (PLAYER) TỪ DB ĐỂ GỬI KÈM 👇
            // Vì object Player trong RAM có thể không lưu avatar, nên lấy từ DB cho chắc
            user senderInfo = userService.getUserById(Integer.parseInt(player.getPlayerId()));
            String senderAvatar = (senderInfo != null) ? senderInfo.getAvatarUrl() : "";

            // Tạo gói tin mời
            Map<String, Object> inviteData = new HashMap<>();
            inviteData.put("type", "game_invite");
            inviteData.put("fromPlayerId", player.getPlayerId());
            inviteData.put("fromPlayerName", player.getPlayerName());
            inviteData.put("fromAvatarUrl", senderAvatar); // ✅ Gửi thêm Avatar
            inviteData.put("timeControl", data.get("timeControl")); // Gửi kèm thời gian muốn chơi

            // Gửi cho người bạn
            sendMessage(friend.getConnection(), inviteData);

            // Phản hồi cho người mời
            sendMessage(player.getConnection(), Map.of("type", "invite_sent", "friendId", friendId));
        } else {
            sendErrorMessage(player.getConnection(), "Người chơi không online.");
        }
    }

    private void handleInviteResponse(Player player, Map<String, Object> data) {
        boolean accepted = (Boolean) data.get("accepted");
        String opponentId = (String) data.get("opponentId");
        Player opponent = getPlayerById(opponentId);
        if (opponent != null && opponent.getConnection() != null && opponent.getConnection().isOpen()) {
            if (accepted) {
                startNewMatch(player, opponent, 600000); // 10 minutes default
            } else {
                sendMessage(opponent.getConnection(), Map.of("type", "invite_rejected", "fromPlayerId", player.getPlayerId()));
            }
        }
    }

    private Player getPlayerById(String playerId) {
        for (Player p : connectionPlayerMap.values()) {
            if (p.getPlayerId().equals(playerId)) {
                return p;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            NioWebSocketServer server = new NioWebSocketServer(8080);
            new Thread(server).start();
        } catch (IOException e) {
            System.err.println("  Cannot start server: " + e.getMessage());
        }
    }
}
