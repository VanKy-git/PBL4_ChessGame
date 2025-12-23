package com.database.server;

import com.database.server.Controller.userController;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class MainApiServer {

    final static int PORT = 8910;

    public static void main(String[] args) {
        // 1. Khởi tạo EntityManagerFactory
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");

        // 2. Khởi tạo Controller (Chỉ giữ lại userController)
        userController userController = new userController(emf);

        // 3. Cấu hình Javalin (CORS, JSON)
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                rule.anyHost(); // Cho phép mọi nguồn truy cập (Development)
                rule.exposeHeader("Content-Type");
                rule.exposeHeader("Authorization");
            }));
        }).start(PORT);

        System.out.println("✅ Server started on port " + PORT);

        // ==========================================
        //          AUTH ENDPOINTS (POST)
        // ==========================================

        // --- Đăng nhập ---
        app.post("/api/login", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("login", requestBody);

            // Nếu response chứa "success": false thì trả về 401, ngược lại 200
            ctx.status(responseJson.contains("\"success\": false") ? 401 : 200);
            ctx.result(responseJson);
        });

        // --- Đăng ký ---
        app.post("/api/register", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("register", requestBody);

            ctx.status(responseJson.contains("\"success\": false") ? 400 : 200);
            ctx.result(responseJson);
        });

        // --- Đăng nhập bằng Google ---
        app.post("/api/loginWithGoogle", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("loginWithGoogle", requestBody);

            ctx.status(responseJson.contains("\"success\": false") ? 401 : 200);
            ctx.result(responseJson);
        });

        // --- Đăng ký bằng Google ---
        app.post("/api/registerWithGoogle", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("registerWithGoogle", requestBody);

            ctx.status(responseJson.contains("\"success\": false") ? 400 : 200);
            ctx.result(responseJson);
        });

        // --- Đăng xuất (Logout) ---
        // Thường logout chỉ cần xóa token phía client, nhưng nếu cần API thì đây:
        app.post("/api/logout", ctx -> {
            // Có thể thêm logic lưu log đăng xuất nếu cần
            ctx.status(200).result("{\"success\": true, \"message\": \"Đăng xuất thành công\"}");
        });

        // ==========================================
        //        ACCOUNT ENDPOINTS (GET/POST)
        // ==========================================

        // --- Lấy thông tin tài khoản (GET /api/account?playerId=xxx) ---
        app.get("/api/account", ctx -> {
            String userId = ctx.queryParam("playerId");

            if (userId == null) {
                ctx.status(400).result("{\"success\": false, \"message\": \"Thiếu tham số playerId!\"}");
                return;
            }

            // Tạo chuỗi JSON giả lập để gửi vào controller (vì controller cũ nhận JSON string)
            String jsonInput = String.format("{ \"userId\": %s }", userId);

            String responseJson = userController.handleRequest("getUserById", jsonInput);
            ctx.result(responseJson);
        });

        // --- Cập nhật thông tin tài khoản ---
        app.post("/api/account/update", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("updateAccount", requestBody);
            ctx.result(responseJson);
        });

        // --- Đổi mật khẩu ---
        app.post("/api/account/change-password", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("changePassword", requestBody);
            ctx.result(responseJson);
        });

        // --- Cập nhật trạng thái (Online/Offline/Playing) ---
        app.post("/api/updateStatus", ctx -> {
            String requestBody = ctx.body();

            if (requestBody.isEmpty()) {
                ctx.status(400).result("{\"success\": false, \"message\": \"Request body is empty\"}");
                return;
            }

            String responseJson = userController.handleRequest("updateStatus", requestBody);
            ctx.result(responseJson);
        });
    }
}
