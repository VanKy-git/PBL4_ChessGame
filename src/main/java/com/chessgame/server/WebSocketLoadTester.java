package com.chessgame.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LOAD TESTING SUITE - 1000 CONCURRENT CLIENTS
 * (Phiên bản đã sửa lỗi, sử dụng thư viện Java-WebSocket)
 */
public class WebSocketLoadTester {

    private static final String SERVER_URL = "ws://localhost:8080";
    private static final int NUM_CLIENTS = 1000;

    // Metrics
    private static final AtomicInteger connectedClients = new AtomicInteger(0);
    private static final AtomicInteger failedConnections = new AtomicInteger(0);
    private static final AtomicLong totalMessagesSent = new AtomicLong(0);
    // ✅ totalMessagesReceived sẽ được TestClient (thư viện) cập nhật chính xác
    private static final AtomicLong totalMessagesReceived = new AtomicLong(0);

    // Latency không được triển khai trong bản test gốc, giữ lại để tham khảo
    // private static final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 WebSocket Load Tester (v2 - Đã sửa lỗi)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Scanner scanner = new Scanner(System.in);
        System.out.println("\nChọn test scenario:");
        System.out.println("1. Connection Stress Test (1000 connections)");
        System.out.println("2. Message Throughput Test");
        System.out.println("3. Game Simulation Test (FIXED)");
        System.out.println("4. Concurrent Matchmaking Test");
        System.out.println("5. Full Load Test (All scenarios)");
        System.out.print("\nLựa chọn (1-5): ");

        int choice = scanner.nextInt();

        switch (choice) {
            case 1:
                runConnectionStressTest();
                break;
            case 2:
                runThroughputTest();
                break;
            case 3:
                runGameSimulationTest();
                break;
            case 4:
                runConcurrentMatchmakingTest();
                break;
            case 5:
                runFullLoadTest();
                break;
            default:
                System.out.println("❌ Invalid choice");
        }
    }

    // ========== TEST 1: CONNECTION STRESS TEST ==========
    private static void runConnectionStressTest() throws Exception {
        System.out.println("\n📊 TEST 1: Connection Stress Test");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Connecting " + NUM_CLIENTS + " clients...\n");

        ExecutorService executor = Executors.newFixedThreadPool(50);
        List<Future<TestClient>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Connect all clients
        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int clientId = i;
            futures.add(executor.submit(() -> {
                try {
                    TestClient client = new TestClient("Client_" + clientId);
                    // ✅ Sử dụng connectBlocking để chờ kết nối và auth
                    client.connectAndAuth();
                    connectedClients.incrementAndGet();

                    if (clientId % 100 == 0) {
                        System.out.println("✅ Connected: " + connectedClients.get() + " clients");
                    }
                    return client;
                } catch (Exception e) {
                    failedConnections.incrementAndGet();
                    System.err.println("❌ Connection failed: " + e.getMessage());
                    return null;
                }
            }));
        }

        // Wait for all connections
        List<TestClient> clients = new ArrayList<>();
        for (Future<TestClient> future : futures) {
            TestClient client = future.get();
            if (client != null) {
                clients.add(client);
            }
        }

        long connectionTime = System.currentTimeMillis() - startTime;

        System.out.println("\n📈 Results:");
        System.out.println("   Total clients: " + NUM_CLIENTS);
        System.out.println("   ✅ Connected: " + connectedClients.get());
        System.out.println("   ❌ Failed: " + failedConnections.get());
        System.out.println("   ⏱️  Time: " + connectionTime + "ms");
        if (connectionTime > 0) {
            System.out.println("   📊 Rate: " + (connectedClients.get() * 1000 / connectionTime) + " conn/sec");
        }

        System.out.println("\n⏳ Keeping connections alive for 10 seconds...");
        Thread.sleep(10000);

        System.out.println("\n🔌 Disconnecting all clients...");
        for (TestClient client : clients) {
            client.disconnect();
        }

        executor.shutdown();
        System.out.println("\n✅ Test completed!");
    }

    // ========== TEST 2: MESSAGE THROUGHPUT TEST ==========
    private static void runThroughputTest() throws Exception {
        // (Logic này giữ nguyên, nhưng giờ đã chính xác hơn
        // vì totalMessagesReceived được đếm đúng)
        System.out.println("\n📊 TEST 2: Message Throughput Test");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int numClients = 100;
        int messagesPerClient = 100;

        System.out.println("Clients: " + numClients);
        System.out.println("Messages per client: " + messagesPerClient);
        System.out.println("\n🔗 Connecting clients...\n");

        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<TestClient> clients = new ArrayList<>();

        // Connect clients
        for (int i = 0; i < numClients; i++) {
            TestClient client = new TestClient("ThroughputClient_" + i);
            client.connectAndAuth();
            clients.add(client);
        }

        System.out.println("✅ " + numClients + " clients connected\n");
        System.out.println("📤 Sending messages...\n");

        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(numClients);

        // Send messages concurrently
        for (TestClient client : clients) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerClient; i++) {
                        client.sendChatMessage("Test message " + i);
                        totalMessagesSent.incrementAndGet();
                        Thread.sleep(10); // Simulate small delay
                    }
                } catch (Exception e) {
                    System.err.println("❌ Send error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long totalTime = System.currentTimeMillis() - startTime;

        System.out.println("\n⏳ Waiting 5s for all responses...");
        Thread.sleep(5000); // Chờ tin nhắn phản hồi

        System.out.println("\n📈 Results:");
        System.out.println("   Messages sent: " + totalMessagesSent.get());
        System.out.println("   Messages received: " + totalMessagesReceived.get());
        System.out.println("   ⏱️  Total time: " + totalTime + "ms");
        if (totalTime > 0) {
            System.out.println("   📊 Throughput: " + (totalMessagesSent.get() * 1000 / totalTime) + " msg/sec");
        }

        // Cleanup
        for (TestClient client : clients) {
            client.disconnect();
        }
        executor.shutdown();
        System.out.println("\n✅ Test completed!");
    }

    // ========== TEST 3: GAME SIMULATION TEST ==========
    private static void runGameSimulationTest() throws Exception {
        System.out.println("\n📊 TEST 3: Game Simulation Test (FIXED)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int numGames = 500; // 500 games = 1000 players
        System.out.println("Simulating " + numGames + " concurrent games (" + (numGames * 2) + " players)\n");

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(numGames);
        AtomicInteger failedGames = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numGames; i++) {
            final int gameId = i;
            executor.submit(() -> {
                try {
                    simulateGame(gameId);
                } catch (Exception e) {
                    System.err.println("Game " + gameId + " FAILED: " + e.getMessage());
                    failedGames.incrementAndGet();
                } finally {
                    latch.countDown();
                    long completed = numGames - latch.getCount();
                    if (completed % 50 == 0) {
                        System.out.println("... Games processed: " + completed);
                    }
                }
            });
        }

        latch.await();
        long totalTime = System.currentTimeMillis() - startTime;

        System.out.println("\n📈 Results:");
        System.out.println("   Games simulated: " + numGames);
        System.out.println("   ✅ Games success: " + (numGames - failedGames.get()));
        System.out.println("   ❌ Games failed: " + failedGames.get());
        System.out.println("   ⏱️  Total time: " + totalTime + "ms");
        if (totalTime > 0) {
            System.out.println("   📊 Games/sec: " + (numGames * 1000 / totalTime));
        }

        executor.shutdown();
        System.out.println("\n✅ Test completed!");
    }

    private static void simulateGame(int gameId) throws Exception {
        TestClient player1 = null;
        TestClient player2 = null;

        try {
            player1 = new TestClient("Player1_" + gameId);
            player2 = new TestClient("Player2_" + gameId);

            player1.connectAndAuth();
            player2.connectAndAuth();

            // ✅ SỬA LỖI: Player 1 tạo phòng và chờ server trả về Room ID
            player1.createRoom(); // Hàm này giờ đã đợi phản hồi
            String roomId = player1.getRoomId();
            if (roomId == null) {
                throw new RuntimeException("P1 failed to create room or get Room ID");
            }

            // SỬA LỖI: Player 2 tham gia đúng phòng
            player2.joinRoom(roomId);
            Thread.sleep(50); // Đợi server xử lý

            // Simulate 10 moves
            // (Thực tế, client trắng đen phải luân phiên
            // nhưng để test tải thì gửi đồng thời cũng được)
            player1.sendMove("e2", "e4", null);
            player2.sendMove("e7", "e5", null);
            Thread.sleep(50);
            player1.sendMove("g1", "f3", null);
            player2.sendMove("b8", "c6", null);

        } finally {
            // Cleanup
            if (player1 != null) player1.disconnect();
            if (player2 != null) player2.disconnect();
        }
    }

    // ========== TEST 4: CONCURRENT MATCHMAKING TEST ==========
    private static void runConcurrentMatchmakingTest() throws Exception {
        System.out.println("\n📊 TEST 4: Concurrent Matchmaking Test");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        int numPlayers = 1000;
        System.out.println("Testing matchmaking with " + numPlayers + " players\n");

        ExecutorService executor = Executors.newFixedThreadPool(50);
        List<TestClient> clients = new ArrayList<>();
        CountDownLatch connectLatch = new CountDownLatch(numPlayers);

        long startTime = System.currentTimeMillis();

        // Connect all players
        for (int i = 0; i < numPlayers; i++) {
            final int playerId = i;
            executor.submit(() -> {
                try {
                    TestClient client = new TestClient("Matchmaking_" + playerId);
                    client.connectAndAuth();
                    synchronized (clients) {
                        clients.add(client);
                    }
                    if (clients.size() % 100 == 0) {
                        System.out.println("✅ Connected: " + clients.size() + " players");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Connection failed: " + e.getMessage());
                } finally {
                    connectLatch.countDown();
                }
            });
        }

        connectLatch.await();
        System.out.println("\n✅ All " + clients.size() + " players connected\n");
        System.out.println("🎮 Starting matchmaking...\n");

        // All players join matchmaking
        CountDownLatch joinLatch = new CountDownLatch(clients.size());
        long matchmakingStart = System.currentTimeMillis();

        for (TestClient client : clients) {
            executor.submit(() -> {
                try {
                    client.joinMatchmaking(600000L); // 10 minutes
                } catch (Exception e) {
                    // System.err.println("❌ Matchmaking failed: " + e.getMessage());
                } finally {
                    joinLatch.countDown();
                }
            });
        }

        joinLatch.await();
        long matchmakingTime = System.currentTimeMillis() - matchmakingStart;

        System.out.println("\n⏳ Waiting 5s for matches to be processed...");
        Thread.sleep(5000);

        System.out.println("\n📈 Results:");
        System.out.println("   Players: " + clients.size());
        System.out.println("   Expected matches: " + (clients.size() / 2));
        System.out.println("   ⏱️  Matchmaking time: " + matchmakingTime + "ms");
        if (matchmakingTime > 0) {
            System.out.println("   📊 Joins/sec: " + (clients.size() * 1000 / matchmakingTime));
        }

        // Cleanup
        for (TestClient client : clients) {
            client.disconnect();
        }
        executor.shutdown();
        System.out.println("\n✅ Test completed!");
    }

    // ========== TEST 5: FULL LOAD TEST ==========
    private static void runFullLoadTest() throws Exception {
        System.out.println("\n🔥 FULL LOAD TEST - Running all scenarios");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        runConnectionStressTest();
        Thread.sleep(2000);
        resetMetrics();

        runThroughputTest();
        Thread.sleep(2000);
        resetMetrics();

        runGameSimulationTest();
        Thread.sleep(2000);
        resetMetrics();

        runConcurrentMatchmakingTest();

        System.out.println("\n✅ All tests completed!");
    }

    private static void resetMetrics() {
        connectedClients.set(0);
        failedConnections.set(0);
        totalMessagesSent.set(0);
        totalMessagesReceived.set(0);
    }

    // ========== TEST CLIENT IMPLEMENTATION (SỬA LỖI) ==========
    static class TestClient extends WebSocketClient {

        private final String name;
        private volatile String roomId;
        // Latch để chờ auth thành công
        private final CountDownLatch authLatch = new CountDownLatch(1);
        // Latch để chờ nhận được ID phòng
        private CountDownLatch roomLatch = new CountDownLatch(1);

        public TestClient(String name) throws URISyntaxException {
            super(new URI(SERVER_URL));
            this.name = name;
        }

        public void connectAndAuth() throws InterruptedException {
            // Kết nối và chờ tối đa 5 giây
            if (!this.connectBlocking(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to connect");
            }
            // Chờ auth thành công tối đa 5 giây
            if (!authLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to authenticate");
            }
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            // System.out.println(name + " connected");
            // Tự động gửi auth khi kết nối
            sendMessage("{\"type\":\"auth\",\"token\":\"\"}");
        }

        @Override
        public void onMessage(String message) {
            // ✅ ĐẾM CHÍNH XÁC
            totalMessagesReceived.incrementAndGet();

            try {
                Map<String, Object> data = gson.fromJson(message,
                        new TypeToken<Map<String, Object>>() {}.getType()
                );

                String type = (String) data.get("type");
                if (type == null) return;

                switch (type) {
                    case "player_info":
                        authLatch.countDown(); // Auth thành công
                        break;
                    case "room_created":
                        // ✅ SỬA LỖI: Đọc Room ID từ server
                        this.roomId = (String) data.get("roomId");
                        roomLatch.countDown(); // Báo cho hàm createRoom biết
                        break;
                    case "room_info": // Khi matchmaking thành công
                        this.roomId = (String) data.get("roomId");
                        break;
                }
            } catch (Exception e) {
                // System.err.println("Failed to parse message: " + message);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            // System.out.println(name + " disconnected: " + reason);
        }

        @Override
        public void onError(Exception ex) {
            // System.err.println(name + " error: " + ex.getMessage());
        }

        // --- Các hàm hành động ---

        private void sendMessage(String json) {
            // Thư viện tự xử lý frame encoding
            this.send(json);
        }

        public void createRoom() throws Exception {
            this.roomLatch = new CountDownLatch(1); // Reset latch
            sendMessage("{\"type\":\"create_room\"}");

            // ✅ SỬA LỖI: Chờ cho đến khi onMessage nhận được "room_created"
            if (!roomLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Did not receive room_created response");
            }
        }

        public void joinRoom(String roomId) throws Exception {
            this.roomId = roomId;
            sendMessage("{\"type\":\"join_room\",\"roomId\":\"" + roomId + "\"}");
        }

        public void joinMatchmaking(Long timeControl) throws Exception {
            sendMessage("{\"type\":\"join\",\"timeControl\":" + timeControl + "}");
        }

        public void sendMove(String from, String to, Character promo) throws Exception {
            String promoStr = promo != null ? ",\"promotion\":\"" + promo + "\"" : "";
            sendMessage("{\"type\":\"move_request\",\"roomId\":\"S_ROOM_ID\",\"from\":\"" + from + "\",\"to\":\"" + to + "\"" + promoStr + "}".replace("S_ROOM_ID", this.roomId));
        }

        public void sendChatMessage(String msg) throws Exception {
            sendMessage("{\"type\":\"chat\",\"roomId\":\"test\",\"message\":\"" + msg + "\"}");
        }

        public String getRoomId() {
            return roomId;
        }

        public void disconnect() {
            try {
                this.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}