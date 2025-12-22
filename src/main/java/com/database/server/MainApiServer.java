package com.database.server;

import com.database.server.Controller.friendsController;
import com.database.server.Controller.matchesController;
import com.database.server.Controller.userController;
import com.google.gson.Gson;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class MainApiServer {
    private static final Gson gson = new Gson();
    final static int PORT = 8910;

    public static void main(String[] args) {
        // 1. Khởi tạo kết nối DB và các Controller
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");

        userController userController = new userController(emf);
        matchesController matchesController = new matchesController(emf);
        friendsController friendsController = new friendsController(emf);

        // 2. Cấu hình Javalin App
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.anyHost();
                    rule.exposeHeader("Content-Type");
                    rule.exposeHeader("Authorization");
                });
            });
        }).start(PORT);

        System.out.println("✅ Server started on port " + PORT);

        // ==================== AUTH API ====================

        app.post("/api/login", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("login", requestBody);
            ctx.status(responseJson.contains("\"success\": false") ? 401 : 200);
            ctx.result(responseJson);
        });

        app.post("/api/register", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("register", requestBody);
            ctx.status(responseJson.contains("\"success\": false") ? 400 : 200);
            ctx.result(responseJson);
        });

        app.post("/api/registerWithGoogle", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("registerWithGoogle", requestBody);
            ctx.status(responseJson.contains("\"success\": false") ? 400 : 200);
            ctx.result(responseJson);
        });

        app.post("/api/loginWithGoogle", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("loginWithGoogle", requestBody);
            ctx.status(responseJson.contains("\"success\": false") ? 401 : 200);
            ctx.result(responseJson);
        });

        // ==================== HISTORY API ====================

//        app.get("/api/history", ctx -> {
//            try {
//                String userId = ctx.queryParam("playerId");
//                if (userId == null) {
//                    ctx.status(400).result("{\"success\": false, \"message\": \"Thiếu tham số playerId!\"}");
//                    return;
//                }
//                String responseJson = matchesController.getHistoryByUserId(userId);
//                ctx.result(responseJson);
//            } catch (Exception e) {
//                e.printStackTrace();
//                ctx.status(500).result("{\"success\": false, \"message\": \"Lỗi Server!\"}");
//            }
//        });

        // ==================== FRIENDS API ====================

        app.get("/api/friends", ctx -> {
            try {
                String userId = ctx.queryParam("playerId");
                if (userId == null) {
                    ctx.status(400).result("{\"success\": false, \"message\": \"Thiếu tham số playerId!\"}");
                    return;
                }
                // Giữ nguyên logic format chuỗi JSON của code cũ để tương thích controller
                String getFriendsJson = String.format("{ \"userId\": %s }", userId);
                String responseJson = friendsController.handleRequest("getFriendsOfUser", getFriendsJson);
                ctx.result(responseJson);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("{\"success\": false, \"message\": \"Lỗi Server!\"}");
            }
        });

        // ==================== ACCOUNT/PROFILE API ====================

        app.get("/api/account", ctx -> {
            try {
                String userId = ctx.queryParam("playerId");
                if (userId == null) {
                    ctx.status(400).result("{\"success\": false, \"message\": \"Thiếu tham số playerId!\"}");
                    return;
                }
                String getUserJson = String.format("{ \"userId\": %s }", userId);
                String responseJson = userController.handleRequest("getUserById", getUserJson);
                ctx.result(responseJson);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("{\"success\": false, \"message\": \"Lỗi Server!\"}");
            }
        });

        app.post("/api/account/update", ctx -> {
            try {
                String requestBody = ctx.body();
                String responseJson = userController.handleRequest("updateAccount", requestBody);
                ctx.result(responseJson);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("{\"success\": false, \"message\": \"Lỗi Server!\"}");
            }
        });

        app.post("/api/account/change-password", ctx -> {
            try {
                String requestBody = ctx.body();
                String responseJson = userController.handleRequest("changePassword", requestBody);
                ctx.result(responseJson);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("{\"success\": false, \"message\": \"Lỗi Server!\"}");
            }
        });

        app.post("/api/updateStatus", ctx -> {
            try {
                String requestBody = ctx.body();
                if (requestBody == null || requestBody.trim().isEmpty()) {
                    ctx.status(400).result("{\"success\": false, \"message\": \"Request body is empty\"}");
                    return;
                }
                String responseJson = userController.handleRequest("updateStatus", requestBody);
                ctx.result(responseJson);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("{\"success\": false, \"message\": \"Server error\"}");
            }
        });
    }
}