package com.chatapp.server.Test;

import com.chatapp.server.Controller.friendsController;
import jakarta.persistence.EntityManagerFactory;

public class friendsTest {

    public static void main(String[] args) {
        EntityManagerFactory emf = null;
        friendsController controller = new friendsController(emf);
//
//        // 1️⃣ Gửi lời mời kết bạn
//        String sendRequestJson = """
//            { "senderId": 1, "receiverId": 2 }
//        """;
//        System.out.println("=== sendFriendRequest ===");
//        System.out.println(controller.handleRequest("sendFriendRequest", sendRequestJson));

        // 2️⃣ Lấy danh sách bạn bè của user
        String getFriendsJson = """
            { "userId": 1 }
        """;
        System.out.println("=== getFriendsOfUser ===");
        System.out.println(controller.handleRequest("getFriendsOfUser", getFriendsJson));

        // 3️⃣ Chấp nhận lời mời kết bạn
        String acceptJson = """
            { "friendshipId": 1 }
        """;
        System.out.println("=== acceptFriendRequest ===");
        System.out.println(controller.handleRequest("acceptFriendRequest", acceptJson));

        // 4️⃣ Từ chối lời mời kết bạn
        String rejectJson = """
            { "friendshipId": 2 }
        """;
        System.out.println("=== rejectFriendRequest ===");
        System.out.println(controller.handleRequest("rejectFriendRequest", rejectJson));

        // 5️⃣ Xóa bạn bè
        String deleteJson = """
            { "friendshipId": 3 }
        """;
        System.out.println("=== deleteFriendship ===");
        System.out.println(controller.handleRequest("deleteFriendship", deleteJson));
    }
}
