package com.chessgame.server;


import com.database.server.MainApiServer;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.IOException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.database.server.Utils.JwtConfig;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;



public class NioWebSocketServer implements Runnable {
    private final int port;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final ManualThreadPool threadPool;
    private final Gson gson = new Gson();
    private final Map<SocketChannel, Player> connectionPlayerMap = new ConcurrentHashMap<>();
    private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final Set<SocketChannel> connections = Collections.synchronizedSet(new HashSet<>());
    private final Map<Long, ConcurrentLinkedQueue<Player>> waitingQueuesByTime = new ConcurrentHashMap<>();
    private final Map<SocketChannel, Object> writeLocks = new ConcurrentHashMap<>();
    private final Queue<SocketChannel> disconnectQueue = new ConcurrentLinkedQueue<>();

    public NioWebSocketServer(int port) throws IOException {
        this.port = port;

        int cores = Runtime.getRuntime().availableProcessors();
        this.threadPool = new ManualThreadPool(cores);
        System.out.println("HĐH: Khởi tạo Thread Pool với " + cores + " luồng worker.");

        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.bind(new InetSocketAddress(port));
        this.serverChannel.configureBlocking(false);

        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);

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
                    // Gọi handleDisconnect từ luồng Selector
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

        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ, attachment);
        attachment.key = clientKey;

        connections.add(client);
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

        // Kiểm tra xem buffer có đủ 1 frame hoàn chỉnh không
        int totalFrameSize = headerLength + maskLength + (int) payloadLength;
        if (buffer.remaining() < (totalFrameSize - headerLength)) {
            buffer.reset();
            return null;
        }

        // Đã có 1 frame hoàn chỉnh, trích xuất nó
        buffer.reset(); // Quay lại vị trí ban đầu (mark)
        byte[] completeFrame = new byte[totalFrameSize];
        buffer.get(completeFrame);

        return completeFrame;
    }

    // Vẫn chạy trong luồng Selector
    // Trong NioWebSocketServer.java

    private void handleRead(SelectionKey key) {
        if (!key.isValid()) {
            System.out.println("Bỏ qua key không hợp lệ (đã bị hủy).");
            return; // Không xử lý key này nữa
        }
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionAttachment attachment = (ConnectionAttachment) key.attachment();

        // Tắt key tạm thời CHỈ KHI bắt đầu đọc
        key.interestOps(0);

        // 1. ĐỌC DỮ LIỆU (Chạy trên luồng Selector)
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int bytesRead;

        try {
            bytesRead = client.read(buffer);

            if (bytesRead == -1) {
                // Client ngắt kết nối
                System.out.println("Client đóng kết nối (đọc -1)");
                handleDisconnect(client); // An toàn vì đang ở luồng Selector
                return;
            }

            buffer.flip();
            attachment.stagingBuffer.put(buffer);
            attachment.stagingBuffer.flip();

            // 2. XỬ LÝ HANDSHAKE (Chạy trên luồng Selector)
            if (!attachment.isHandshakeDone) {
                byte[] data = new byte[attachment.stagingBuffer.remaining()];
                attachment.stagingBuffer.get(data);

                // doHandshake (client.write) chạy trên luồng Selector
                if (doHandshake(client, new String(data, StandardCharsets.UTF_8))) {
                    attachment.isHandshakeDone = true;
                    System.out.println("Handshake thành công");
                } else {
                    System.out.println("Handshake thất bại");
                }
                attachment.stagingBuffer.compact();
            }
            // 3. GIẢI MÃ FRAME & ĐẨY LOGIC VÀO WORKER
            else {
                while (true) {
                    byte[] completeFrame = tryGetCompleteFrame(attachment.stagingBuffer);

                    if (completeFrame != null) {
                        // Giải mã frame (vẫn trên luồng Selector)
                        String jsonMessage = decodeWebSocketFrame(completeFrame);

                        // === CHỈ NÉM LOGIC GAME VÀO THREAD POOL ===
                        if (jsonMessage != null) {
                            final String finalJsonMessage = jsonMessage;
                            threadPool.submit(() -> {
                                // Worker thread chỉ xử lý logic, không đọc/ghi
                                handleGameLogic(client, attachment, finalJsonMessage);
                            });
                        }
                    } else {
                        break; // Không còn frame nào, chờ đọc thêm
                    }
                }
                attachment.stagingBuffer.compact();
            }

            // 4. Kích hoạt lại key
            if (client.isOpen()) {
                // Phải wakeup() trước khi thay đổi interestOps từ luồng khác
                // Nhưng vì chúng ta đang ở cùng luồng, chỉ cần set lại là đủ
                // Tuy nhiên, để an toàn tuyệt đối khi đăng ký lại:
                selector.wakeup();
                key.interestOps(SelectionKey.OP_READ);
            }

        } catch (SocketException e) {
            System.out.println("Socket error: " + e.getMessage());
            handleDisconnect(client); // An toàn vì đang ở luồng Selector
        } catch (IOException e) {
            handleDisconnect(client); // An toàn vì đang ở luồng Selector
        } catch (Exception e) {
            System.err.println("Lỗi handleRead: " + e.getMessage());
            e.printStackTrace();
            handleDisconnect(client); // An toàn vì đang ở luồng Selector
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

    // --- WEBSOCKET PROTOCOL ---

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
            System.out.println(" Frame quá ngắn");
            return null;
        }

        boolean isFinal = (frame[0] & 0x80) != 0;
        byte opcode = (byte) (frame[0] & 0x0F);

        // XỬ LÝ CONTROL FRAMES
        if (opcode == 0x8) {
            System.out.println("Received CLOSE frame");
            throw new IOException("Client requested close");
        }

        if (opcode == 0x9) {
            System.out.println("Received PING");
            return "__PING__";
        }

        if (opcode == 0xA) {
            System.out.println("Received PONG");
            return "__PONG__";
        }

        // Chỉ xử lý TEXT frame (0x1)
        if (opcode != 0x1) {
            System.out.println("Unknown opcode: " + opcode);
            return null;
        }

        boolean isMasked = (frame[1] & 0x80) != 0;
        long payloadLength = (frame[1] & 0x7F);
        int offset = 2;

        //XỬ LÝ EXTENDED LENGTH
        if (payloadLength == 126) {
            if (frame.length < 4) {
                System.out.println("Frame chưa đủ dữ liệu (126)");
                return null;
            }
            payloadLength = ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
            offset = 4;
        } else if (payloadLength == 127) {
            if (frame.length < 10) {
                System.out.println("Frame chưa đủ dữ liệu (127)");
                return null;
            }
            payloadLength = 0;
            for (int i = 0; i < 8; i++) {
                payloadLength = (payloadLength << 8) | (frame[2 + i] & 0xFF);
            }
            offset = 10;
        }

        // KIỂM TRA BUFFER ĐỦ LỚN
        if (frame.length < offset + (isMasked ? 4 : 0) + payloadLength) {
            System.out.println("Frame chưa đủ dữ liệu payload");
            return null;
        }

        if (!isMasked) {
            System.out.println("Client không mask frame - vi phạm RFC 6455");
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

    //THÊM: Gửi PONG frame
    private void sendPongFrame(SocketChannel client) {
        try {
            if (client.isOpen()) {
                ByteBuffer pongFrame = ByteBuffer.allocate(2);
                pongFrame.put((byte) 0x8A); // FIN + PONG opcode
                pongFrame.put((byte) 0x00);
                pongFrame.flip();
                client.write(pongFrame);
                System.out.println("Sent PONG");
            }
        } catch (IOException e) {
            System.err.println("  Không gửi được PONG");
            handleDisconnect(client);
        }
    }

    // --- GAME LOGIC ---

    private static class ConnectionAttachment {
        public boolean isHandshakeDone = false;
        public Player player = null;
        public SelectionKey key = null;
        public ByteBuffer stagingBuffer = ByteBuffer.allocate(8192);
    }

    private void handleGameLogic(SocketChannel client, ConnectionAttachment attachment, String jsonMessage) {
        if (jsonMessage == null) {
            System.out.println("⚠  jsonMessage is null");
            return;
        }

        // XỬ LÝ CONTROL FRAMES
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
                System.out.println("Invalid message format");
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
                default:
                    System.out.println("Unknown type: " + type);
                    sendErrorMessage(client, "Unknown message type");
            }

        } catch (com.google.gson.JsonSyntaxException e) {
            System.err.println("Invalid JSON: " + e.getMessage());
            sendErrorMessage(client, "Invalid JSON format");
        } catch (Exception e) {
            System.err.println("Error in handleGameLogic: " + e.getMessage());
            e.printStackTrace();
            sendErrorMessage(client, "Lỗi xử lý logic: " + e.getMessage());
        }
    }

    private void handleAuthentication(SocketChannel client, ConnectionAttachment attachment, Map<String, Object> data) {
        String token = (String) data.get("token");

        // GUEST MODE
        if (token == null || token.isEmpty()) {
            String guestName = "Guest_" + (new Random().nextInt(9000) + 1000);
            String guestId = "guest_" + UUID.randomUUID();

            Player guestPlayer = new Player(guestId, guestName, client);
            connectionPlayerMap.put(client, guestPlayer);
            attachment.player = guestPlayer;

            Map<String, Object> response = new HashMap<>();
            response.put("type", "player_info");
            response.put("playerId", guestPlayer.getPlayerId());
            response.put("playerName", guestPlayer.getPlayerName());
            response.put("isGuest", true);

            System.out.println("Guest: " + guestName);
            sendMessage(client, response);
            return;
        }

        try {
            System.out.println("Verifying JWT");

            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(JwtConfig.JWT_SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);

            Claims payload = claimsJws.getBody();
            String userId = payload.getSubject();
            String username = payload.get("username", String.class);

            Player authenticatedPlayer = new Player(userId, username, client);
            connectionPlayerMap.put(client, authenticatedPlayer);
            attachment.player = authenticatedPlayer;

            Map<String, Object> response = new HashMap<>();
            response.put("type", "player_info");
            response.put("playerId", authenticatedPlayer.getPlayerId());
            response.put("playerName", authenticatedPlayer.getPlayerName());
            response.put("isGuest", false);

            System.out.println("User: " + username);
            sendMessage(client, response);

        } catch (JwtException e) {
            System.err.println("JWT failed: " + e.getMessage());
            sendErrorMessage(client, "Token không hợp lệ: " + e.getMessage());
        }
    }

    private String generateUniqueRoomId() {
        Random random = new Random();
        String roomId;

        do {
            int number = random.nextInt(1000000);
            roomId = String.format("%06d", number); // Đảm bảo đủ 6 số
        } while (gameRooms.containsKey(roomId)); // Kiểm tra trùng

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
    }

    private void startGame(GameRoom room) {
        room.setStatus("playing");
        room.setCurrentTurn("white");
        room.getValidator().resetBoard();
        Map<String, Object> gameStartData = new HashMap<>();
        gameStartData.put("gameState", room.getValidator().toFen());
        gameStartData.put("currentTurn", "white");
        gameStartData.put("initialTimeMs", room.getInitialTimeMs());

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
            System.out.println("  Matching (song song): " + player.getPlayerName() + " vs " + opponent.getPlayerName());

            final Player p1 = player;
            final Player p2 = opponent;

            threadPool.submit(() -> {
                startNewMatch(p1, p2, preferredTime);
            });

        } else {
            System.out.println("Player " + player.getPlayerName() + " vào hàng chờ: " + preferredTime + "ms");
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
            sendMessage(player.getConnection(), Map.of(
                    "type", "room_info",
                    "roomId", roomId
            ));
            sendMessage(player.getConnection(), Map.of(
                    "type", "color",
                    "color", player.getColor()
            ));
        }

        startGame(room);
    }

    private void handleCreateRoom(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = generateUniqueRoomId();
        GameRoom room = new GameRoom(roomId, 600000, this); // Mặc định 10 phút
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

        sendMessage(player.getConnection(), Map.of(
                "type", "room_joined",
                "roomId", roomId,
                "color", player.getColor(),
                "gameState", room.getValidator().toFen()
        ));

        notifyRoomPlayers(room, "player_joined", Map.of(
                "playerName", player.getPlayerName(),
                "color", player.getColor()
        ));

        if (room.isFull()) {
            startGame(room);
        }
        broadcastRoomsList();
    }

    private void handleLeaveRoom(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);
        if (room != null) {
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
            broadcastRoomsList();
        }
    }

    private void handleGameMove(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = (String) data.get("roomId");
        GameRoom room = gameRooms.get(roomId);

        if (room == null || !room.getStatus().equals("playing")) return;
        if (!room.getCurrentTurn().equals(player.getColor())) {
            sendErrorMessage(player.getConnection(), "It's not your turn!");
            return;
        }

        String from = data.get("from").toString();
        String to = data.get("to").toString();
        Character promo = data.get("promotion") == null ? null : (Character) data.get("promotion");
        ChessValidator.MoveResult moveResult = room.getValidator().validateMove(from, to, player.getColor(), promo);

        if (!moveResult.isValid) {
            sendMessage(player.getConnection(), Map.of(
                    "type", "move_result",
                    "result", false,
                    "message", moveResult.message
            ));
        } else {
            String nextTurn = player.getColor().equals("white") ? "black" : "white";
            room.setCurrentTurn(nextTurn);
            String fen = room.getValidator().toFen();
            boolean isNextPlayerInCheck = room.getValidator().isKingInCheck(nextTurn, room.getValidator().getBoard());

            Map<String, Object> fenData = new HashMap<>();
            fenData.put("result", true);
            fenData.put("fen", fen);
            fenData.put("lastMove", Map.of("from", from, "to", to));
            fenData.put("isCheck", isNextPlayerInCheck);
            notifyRoomPlayers(room, "move_result", fenData);

            if (moveResult.winner != null) {
                notifyRoomPlayers(room, "end_game", Map.of("winner", moveResult.winner));
                room.setStatus("finished");
                room.stopTimer();
            }
        }
    }

    private void handleChatMessage(Player player, Map<String, Object> data) {
        if (player == null) return;
        String roomId = (String) data.get("roomId");
        String message = (String) data.get("message");
        GameRoom room = gameRooms.get(roomId);
        if (room != null) {
            notifyRoomPlayers(room, "chat", Map.of(
                    "playerName", player.getPlayerName(),
                    "message", message,
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    private void handleGetRooms(Player player) {
        List<Map<String, Object>> roomList = new ArrayList<>();
        for (GameRoom room : gameRooms.values()) {
            if (room.getStatus().equals("waiting")) {
                roomList.add(Map.of(
                        "roomId", room.getRoomId(),
                        "playerCount", room.getPlayers().size(),
                        "status", room.getStatus()
                ));
            }
        }
        sendMessage(player.getConnection(), Map.of("type", "room_list", "rooms", roomList));
    }

    private void broadcastRoomsList() {
        List<Map<String, Object>> roomsList = new ArrayList<>();
        for (GameRoom room : gameRooms.values()) {
            if (room.getStatus().equals("waiting")) {
                roomsList.add(Map.of("roomId", room.getRoomId(), "playerCount", room.getPlayers().size()));
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

        // Lấy đúng hàng đợi mà player đang đứng
        ConcurrentLinkedQueue<Player> queue = waitingQueuesByTime.get(player.getPreferredTimeMs());

        if (queue != null) {
            // remove() là một thao tác an toàn (thread-safe)
            boolean removed = queue.remove(player);
            if (removed) {
                System.out.println("Player " + player.getPlayerName() + " đã hủy tìm trận.");
            }
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
        sendMessage(player.getConnection(), Map.of(
                "type", "valid_moves",
                "square", square,
                "moves", validMoves
        ));
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
        if (opponent == null || !opponent.getConnection().isOpen()) {
            sendMessage(player.getConnection(), Map.of("type", "rematch_unavailable", "reason", "Đối thủ đã offline."));
            return;
        }

        String playerColor = player.getColor();
        String opponentColor = opponent.getColor();

        if (playerColor.equals(room.getRematchRequestedByColor())) return; // Đã yêu cầu rồi

        if (opponentColor.equals(room.getRematchRequestedByColor())) {
            // --- Cả hai đã yêu cầu -> Bắt đầu tái đấu ---
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
            // --- Chỉ người này yêu cầu -> Gửi lời mời cho đối thủ ---
            room.setRematchRequestedByColor(playerColor);
            sendMessage(opponent.getConnection(), Map.of("type", "rematch_offer", "offeringPlayer", player.getPlayerName()));
            sendMessage(player.getConnection(), Map.of("type", "rematch_offer_sent"));
        }
    }

    private void handleGetHistory(Player player, Map<String, Object> data) {
        // TODO: Kết nối với Database Service để lấy lịch sử thật
        List<Map<String, Object>> historyList = new ArrayList<>();
        sendMessage(player.getConnection(), Map.of("type", "history_list", "history", historyList));
    }

    private GameRoom findRoomByPlayer(Player player) {
        if (player == null) return null;
        for (GameRoom room : gameRooms.values()) {
            if (room.getPlayers().contains(player)) {
                return room;
            }
        }
        return null;
    }

    private void cleanupRoom(String roomId) {
        GameRoom removedRoom = gameRooms.remove(roomId);
        if (removedRoom != null) {
            System.out.println("Cleaning up room " + roomId);
            removedRoom.stopTimer();
            // (Giả sử GameRoom không còn timer service riêng)
        }
    }

    public void handleTimeout(GameRoom room, String winnerColor) {
        if (room == null) return;
        System.out.println("Handling timeout for room " + room.getRoomId() + ". Winner: " + winnerColor);
        Map<String, Object> endData = new HashMap<>();
        endData.put("winner", winnerColor);
        endData.put("reason", "timeout");
        notifyRoomPlayers(room, "end_game", endData);
        room.setStatus("finished");
        room.stopTimer();
    }

    private void handleDisconnect(SocketChannel client) {
        if (client == null) {
            System.err.println("handleDisconnect được gọi với client=null. Đang bỏ qua...");
            return;
        }
        try {
            sendCloseFrame(client);

            SelectionKey key = client.keyFor(selector);
            if (key != null) {
                key.cancel();
            }

            Player player = connectionPlayerMap.remove(client);
            if (player != null) {
                System.out.println("Player disconnected: " + player.getPlayerName());
                if (player.getPreferredTimeMs() != null) {
                    ConcurrentLinkedQueue<Player> queue = waitingQueuesByTime.get(player.getPreferredTimeMs());
                    if (queue != null) {
                        // remove() là một thao tác an toàn (thread-safe)
                        boolean removed = queue.remove(player);
                        if (removed) {
                            System.out.println("Player " + player.getPlayerName() + " đã hủy tìm trận.");
                        }
                    }
                }
                GameRoom room = findRoomByPlayer(player);
                if (room != null) {
                    handlePlayerDisconnectInRoom(room, player);
                }
            }
            writeLocks.remove(client);
            connections.remove(client);
            client.close();

            System.out.println("Client disconnected");
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }

    private void handlePlayerDisconnectInRoom(GameRoom room, Player player) {
        if (room == null || player == null) return;

        String status = room.getStatus();

        if ("playing".equals(status)) {
            Player opponent = room.getOpponent(player);
            if (opponent != null && opponent.getConnection().isOpen()) {
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
            // Đang chờ → Xóa phòng
            gameRooms.remove(room.getRoomId());
            broadcastRoomsList();
        }

        room.removePlayer(player);
        if (room.isEmpty()) {
            cleanupRoom(room.getRoomId());
        }
    }

    public void sendMessage(SocketChannel client, Object messageObject) {
        if (!client.isOpen()) {
            System.out.println("Cannot send - client closed");
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
                System.err.println("  Send error: " + e.getMessage());
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

    // --- MAIN ---
    public static void main(String[] args) {

        EntityManagerFactory ENTITY_MANAGER_FACTORY = null;
        try {
            NioWebSocketServer server = new NioWebSocketServer(8080);
            new Thread(server).start();
        } catch (IOException e) {
            System.err.println("  Cannot start server: " + e.getMessage());
        }

        try {
            // Lỗi ban đầu: FATAL: password authentication failed xảy ra tại đây!
            ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory("PBL4_ChessPU");
            System.out.println("✅ JPA/Hibernate đã khởi tạo thành công.");
        } catch (Exception e) {
            System.err.println("❌ LỖI KHỞI TẠO CƠ SỞ DỮ LIỆU. Đang thoát: " + e.getMessage());
            e.printStackTrace();
            return; // Dừng ứng dụng nếu DB không kết nối được
        }

        try {
            int httpPort = 8910;
            // Tạo instance của MainApiServer (là một Runnable)
            MainApiServer httpApiServer = new MainApiServer(httpPort, ENTITY_MANAGER_FACTORY);
            // Chạy nó trong một luồng (Thread) mới
            new Thread(httpApiServer).start(); 
            System.out.println("✅ HTTP API Server (MainApiServer) đang chạy trên cổng " + httpPort);
        } catch (Exception e) {
            System.err.println("❌ LỖI KHỞI CHẠY HTTP API SERVER: " + e.getMessage());
            e.printStackTrace();
        }
    }
}