package com.database.server;

import com.database.server.Controller.userController;
import com.database.server.Utils.JwtConfig;
import com.google.gson.Gson;
import io.javalin.Javalin;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Map;

public class MainApiServer {
    private static final Gson gson = new Gson();
    final static int PORT = 8910;

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        userController userController = new userController(emf);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.reflectClientOrigin = true; // Tự động phản chiếu origin của client
                    it.allowCredentials = true;
                    it.exposeHeader("Content-Type");
                    it.exposeHeader("Authorization");
                });
            });
        });
        
        // Xử lý thủ công OPTIONS request để đảm bảo preflight hoạt động tốt nhất
        app.options("/*", ctx -> {
            String origin = ctx.header("Origin");
            if (origin != null) {
                ctx.header("Access-Control-Allow-Origin", origin);
            }
            ctx.header("Access-Control-Allow-Credentials", "true");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.status(200);
        });

        // --- Public Routes ---
        app.post("/api/login", ctx -> {
            String requestbody = ctx.body();
            String responeJson = userController.handleRequest("login", requestbody);
            ctx.result(responeJson);
        });

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

        app.get("/api/leaderboard", ctx -> {
            String responseJson = userController.handleRequest("getLeaderboard", "{}");
            ctx.result(responseJson);
        });

        // --- Authenticated Routes ---
        app.before("/api/user/*", ctx -> {
            if (ctx.method().equals("OPTIONS")) {
                return;
            }
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(401).result("{\"error\":\"Unauthorized\"}").skipRemainingHandlers();
                return;
            }
            String token = authHeader.substring(7);
            try {
                Jws<Claims> claimsJws = Jwts.parserBuilder()
                        .setSigningKey(JwtConfig.JWT_SECRET_KEY)
                        .build()
                        .parseClaimsJws(token);
                ctx.attribute("userId", claimsJws.getBody().getSubject());
            } catch (JwtException e) {
                ctx.status(401).result("{\"error\":\"Invalid token\"}").skipRemainingHandlers();
            }
        });

        app.get("/api/user/me", ctx -> {
            String userId = ctx.attribute("userId");
            String requestJson = gson.toJson(Map.of("userId", Integer.parseInt(userId)));
            String responseJson = userController.handleRequest("getUserById", requestJson);
            ctx.result(responseJson);
        });

        app.post("/api/user/change-password", ctx -> {
            String userId = ctx.attribute("userId");
            Map<String, Object> requestBody = gson.fromJson(ctx.body(), Map.class);
            requestBody.put("userId", Integer.parseInt(userId));
            String requestJson = gson.toJson(requestBody);
            String responseJson = userController.handleRequest("changePassword", requestJson);
             if (responseJson.contains("\"success\": false")) {
                ctx.status(400);
            } else {
                ctx.status(200);
            }
            ctx.result(responseJson);
        });


        app.start(PORT);
        
        System.out.println("HTTP API Server started on port " + PORT);
        System.out.println("Registered routes:");
        System.out.println(" - POST /api/login");
        System.out.println(" - POST /api/register");
        System.out.println(" - GET  /api/leaderboard");
        System.out.println(" - GET  /api/user/me (Authenticated)");
        System.out.println(" - POST /api/user/change-password (Authenticated)");
    }
    
    public static void start() {
        main(new String[0]);
    }
}
