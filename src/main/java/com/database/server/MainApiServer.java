package com.database.server;

import com.database.server.Controller.userController;
import com.google.gson.Gson;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class MainApiServer {
    private static final Gson gson = new Gson();
    final static int PORT = 8910;
    public static void main(String[] args){
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        userController userController = new userController(emf);
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.anyHost(); // hoặc chỉ định host cụ thể
                    rule.exposeHeader("Content-Type");
                    rule.exposeHeader("Authorization");
                });
            });
        }).start(PORT);
        System.out.println("Server started on port " + PORT);
        app.post("/api/login", ctx ->
        {
            String requestbody = ctx.body();
            String responeJson = userController.handleRequest("login" ,requestbody);
            ctx.result(responeJson);
        });
        app.post("/api/register", ctx -> {
            String requestBody = ctx.body();
            String responseJson = userController.handleRequest("register", requestBody);

            // Kiểm tra xem controller trả về lỗi hay thành công
            if (responseJson.contains("\"success\": false")) {
                ctx.status(400); // 400 Bad Request
            } else {
                ctx.status(200); // 200 OK
            }

            ctx.result(responseJson);
        });
    }
}

