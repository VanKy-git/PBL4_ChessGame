package com.database.server;

import com.database.server.Controller.userController;
import com.google.gson.Gson;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class MainApiServer {
    private static final Gson gson = new Gson();
    final static int PORT = 8910;

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        userController userController = new userController(emf);

        // 1. Khởi tạo Javalin app nhưng CHƯA start ngay
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.anyHost(); // hoặc chỉ định host cụ thể
                    rule.exposeHeader("Content-Type");
                    rule.exposeHeader("Authorization");
                });
            });
        });

        // 2. Đăng ký các routes (endpoints) TRƯỚC khi start server
        
        // Login
        app.post("/api/login", ctx -> {
            String requestbody = ctx.body();
            String responeJson = userController.handleRequest("login", requestbody);
            ctx.result(responeJson);
        });

        // Register
        app.post("/api/register", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("register", requestBody);

            if (responseJson.contains("\"success\": false")) {
                ctx.status(400);
            } else {
                ctx.status(200);
            }
            ctx.result(responseJson);
        });

        // Leaderboard
        app.get("/api/leaderboard", ctx -> {
            System.out.println("Received request for leaderboard"); // Log debug
            String responseJson = userController.handleRequest("getLeaderboard", "{}");
            ctx.result(responseJson);
        });

        // 3. Start server sau khi đã đăng ký hết routes
        app.start(PORT);
        
        System.out.println("HTTP API Server started on port " + PORT);
        System.out.println("Registered routes:");
        System.out.println(" - POST /api/login");
        System.out.println(" - POST /api/register");
        System.out.println(" - GET  /api/leaderboard");
    }
    
    // Giữ lại phương thức start() tĩnh để tương thích nếu Main.java gọi
    public static void start() {
        main(new String[0]);
    }
}
