package com.chatapp.server.Test;


import com.chatapp.server.Controller.matchesController;

public class matchesTest {
    public static void main(String[] args) {
        matchesController controller = new matchesController();

        // JSON gồm 2 đối tượng user
        String createJson = """
        {
            "player1": {
                "user_id": 1,
                "username": "Alice",
                "elo_rating": 1200
            },
            "player2": {
                "user_id": 2,
                "username": "Bob",
                "elo_rating": 1250
            }
        }
        """;

//        System.out.println("===== TẠO TRẬN MỚI =====");
//        System.out.println(controller.handleRequest("createMatch", createJson));

        System.out.println("\n===== DANH SÁCH TRẬN =====");
        System.out.println(controller.handleRequest("getAllMatches", "{}"));
    }
}
