package com.database.server;

import com.database.server.Controller.friendsController;
import com.database.server.Controller.matchesController;
import com.database.server.Controller.userController;
import com.sun.net.httpserver.HttpServer; 
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers; 
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence; // Cần import Persistence
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * LỚP HTTP API SERVER: Sử dụng HTTP Server thuần của Java (com.sun.net.httpserver).
 * Chạy trong một luồng riêng, lắng nghe trên cổng 8910.
 */
public class MainApiServer implements Runnable {
    
    final int port;
    private final EntityManagerFactory emf;
    
    // Khai báo các Controller
    private final userController userController;
    private final matchesController matchesController;
    private final friendsController friendsController;
    
    private HttpServer server;
    private final ExecutorService httpExecutor = Executors.newFixedThreadPool(10); 

    // Constructor nhận EMF
    public MainApiServer(int port, EntityManagerFactory emf) {
        this.port = port;
        this.emf = emf;
        
        // KHỞI TẠO CONTROLLER BẰNG EMF ĐÃ TRUYỀN VÀO
        this.userController = new userController(emf); 
        this.matchesController = new matchesController(emf); 
        this.friendsController = new friendsController(emf); 
    }
    
    // =========================================================
    //                       HELPER & CORS
    // =========================================================
    
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return new java.io.BufferedReader(isr).lines().collect(Collectors.joining("\n"));
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void setCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        headers.set("Access-Control-Max-Age", "3600");
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
    }
    
    // HÀM GIẢ ĐỊNH: Lấy ID người dùng (THAY THẾ BẰNG LOGIC JWT THỰC TẾ)
    private String extractUserIdFromHeader(HttpExchange exchange) {
        // Trong thực tế: Giải mã JWT. Hiện tại: ID giả định.
        return "1"; 
    }
    
    // =========================================================
    //                       HTTP SERVER RUNNER
    // =========================================================

    @Override
    public void run() {
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0); 
            server.setExecutor(httpExecutor); 

            registerEndpoints();

            server.start();
            System.out.println("✅ HTTP API Server (MainApiServer) started on port " + port);
        } catch (IOException e) {
            System.err.println("❌ Lỗi khởi động HTTP Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // =========================================================
    //                       ENDPOINT REGISTRATION
    // =========================================================

    private void registerEndpoints() {
        
        // --- 1. LOGIN & REGISTER ENDPOINTS (POST) ---
        server.createContext("/api/login", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { handleOptions(exchange); return; }
            if ("POST".equals(exchange.getRequestMethod())) {
                setCorsHeaders(exchange);
                String requestBody = readRequestBody(exchange);
                String responseJson = userController.handleRequest("login" ,requestBody);
                // 401 Unauthorized nếu login thất bại
                sendResponse(exchange, responseJson.contains("\"success\": false") ? 401 : 200, responseJson);
            } else {
                sendResponse(exchange, 405, "{\"success\": false, \"message\": \"Method Not Allowed\"}");
            }
        });

        server.createContext("/api/register", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) { handleOptions(exchange); return; }
            if ("POST".equals(exchange.getRequestMethod())) {
                setCorsHeaders(exchange);
                String requestBody = readRequestBody(exchange);
                String responseJson = userController.handleRequest("register", requestBody);
                // 400 Bad Request nếu register thất bại
                sendResponse(exchange, responseJson.contains("\"success\": false") ? 400 : 200, responseJson);
            } else {
                sendResponse(exchange, 405, "{\"success\": false, \"message\": \"Method Not Allowed\"}");
            }
        });

        // --- 2. DATA ENDPOINTS (GET) ---
        
        // --- Endpoint Lịch sử (GET /api/history)
server.createContext("/api/history", exchange -> {
    if ("OPTIONS".equals(exchange.getRequestMethod())) { 
        handleOptions(exchange); 
        return; 
    }

    if ("GET".equals(exchange.getRequestMethod())) {
        setCorsHeaders(exchange);

        try {
            // Lấy query từ URL
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            // Lấy playerId từ query
            String userId = params.getOrDefault("playerId", null);

            if (userId == null) {
                sendResponse(exchange, 400, """
                {
                  "success": false,
                  "message": "Thiếu tham số playerId!"
                }
                """);
                return;
            }

            // Gọi controller
            String responseJson = matchesController.getHistoryByUserId(userId);

            // Trả về JSON
            sendResponse(exchange, 200, responseJson);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, """
            {
              "success": false,
              "message": "Lỗi Server khi xử lý /api/history!"
            }
            """);
        }

    } else {
        sendResponse(exchange, 405, """
        {
          "success": false,
          "message": "Method Not Allowed"
        }
        """);
    }
});


     // --- Endpoint Bạn bè (GET/DELETE /api/friends)
     server.createContext("/api/friends", exchange -> {
        String requestMethod = exchange.getRequestMethod();
        
        // 🚨 Đảm bảo CORS Headers luôn được set đầu tiên
        setCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(requestMethod)) { 
            exchange.sendResponseHeaders(204, -1); // Trả về 204 No Content cho Preflight
            return; 
        }

        if ("GET".equalsIgnoreCase(requestMethod)) {
            try {
                // Lấy query từ URL (để tìm playerId)
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                
                // Lấy playerId từ query (thay vì Header như code cũ)
                String userId = params.getOrDefault("playerId", null);

                if (userId == null) {
                    sendResponse(exchange, 400, """
                    {
                      "success": false,
                      "message": "Thiếu tham số playerId!"
                    }
                    """);
                    return;
                }

                // Gọi controller
                String getFriendsJson = String.format("""
                    { "userId": %s }
                """, userId);
                
                String responseJson = friendsController.handleRequest("getFriendsOfUser", getFriendsJson);

                // Trả về JSON
                sendResponse(exchange, 200, responseJson);

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, """
                {
                  "success": false,
                  "message": "Lỗi Server khi xử lý GET /api/friends!"
                }
                """);
            }
        } else if ("DELETE".equalsIgnoreCase(requestMethod)) { // 🚨 FIX: Sử dụng equalsIgnoreCase
             try {
                // Lấy friendshipId từ query (e.g., /api/friends?friendshipId=5)
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                String friendshipId = params.getOrDefault("friendshipId", null);

                if (friendshipId == null) {
                    sendResponse(exchange, 400, """
                    {
                      "success": false,
                      "message": "Thiếu tham số friendshipId!"
                    }
                    """);
                    return;
                }

                String deleteJson = String.format("""
                    { "friendshipId": %s }
                """, friendshipId);

                // Gọi controller để xóa
                String responseJson = friendsController.handleRequest("deleteFriendship", deleteJson);
                
                // Nếu xóa thành công, trả về 200 hoặc 204 No Content
                sendResponse(exchange, responseJson.contains("\"success\": true") ? 200 : 400, responseJson);

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, """
                {
                  "success": false,
                  "message": "Lỗi Server khi xử lý DELETE /api/friends!"
                }
                """);
            }
        } else {
            sendResponse(exchange, 405, "{\"success\": false, \"message\": \"Method Not Allowed\"}");
        }
    });

// --- Endpoint Bảng xếp hạng (GET /api/leaderboard)
server.createContext("/api/leaderboard", exchange -> {
    if ("OPTIONS".equals(exchange.getRequestMethod())) { 
        handleOptions(exchange); 
        return; 
    }

    if ("GET".equals(exchange.getRequestMethod())) {
        setCorsHeaders(exchange);
        
        try {
            // Leaderboard thường không cần playerId
            // String responseJson = userController.getLeaderboard();

            // // Trả về JSON
            // sendResponse(exchange, 200, responseJson);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, """
            {
              "success": false,
              "message": "Lỗi Server khi xử lý /api/leaderboard!"
            }
            """);
        }

    } else {
        sendResponse(exchange, 405, "{\"success\": false, \"message\": \"Method Not Allowed\"}");
    }
});

// --- Endpoint Tài khoản (GET /api/account)
// server.createContext("/api/account", exchange -> {
//     if ("OPTIONS".equals(exchange.getRequestMethod())) { 
//         handleOptions(exchange); 
//         return; 
//     }

//     if ("GET".equals(exchange.getRequestMethod())) {
//         setCorsHeaders(exchange);
        
//         try {
//             // Lấy query từ URL (để tìm playerId)
//             String query = exchange.getRequestURI().getQuery();
//             Map<String, String> params = parseQuery(query);
            
//             // Lấy playerId từ query
//             String userId = params.getOrDefault("playerId", null);

//             if (userId == null) {
//                 sendResponse(exchange, 400, """
//                 {
//                   "success": false,
//                   "message": "Thiếu tham số playerId!"
//                 }
//                 """);
//                 return;
//             }

//             // Gọi controller
//             String responseJson = userController.getAccountDetails(userId);

//             // Trả về JSON
//             sendResponse(exchange, 200, responseJson);

//         } catch (Exception e) {
//             e.printStackTrace();
//             sendResponse(exchange, 500, """
//             {
//               "success": false,
//               "message": "Lỗi Server khi xử lý /api/account!"
//             }
//             """);
//         }

//     } else {
//         sendResponse(exchange, 405, "{\"success\": false, \"message\": \"Method Not Allowed\"}");
//     }
// });
    }


    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
    
        if (query == null || query.isEmpty()) {
            return result;
        }
    
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                result.put(parts[0], parts[1]);
            }
        }
    
        return result;
    }
    
}