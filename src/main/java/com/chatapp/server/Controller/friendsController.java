package com.chatapp.server.Controller;

import com.chatapp.server.Model.DAO.friendsDAO;
import com.chatapp.server.Model.Entity.friends;
import com.chatapp.server.Model.Service.friendsService;
import com.chatapp.server.Utils.GsonUtils;
import com.google.gson.Gson;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * friendsController - Xử lý các request liên quan đến bạn bè
 * ✅ Chỉ parse JSON, gọi Service, trả về JSON response
 */
public class friendsController {

    private final friendsService service;
    private final Gson gson;

    public friendsController(EntityManagerFactory emf) {
        emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        friendsDAO dao = new friendsDAO(emf.createEntityManager());
        this.service = new friendsService(dao);
        this.gson = GsonUtils.gson;
    }

    // ========================= HELPER METHODS =========================
    private String successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return gson.toJson(response);
    }

    private String successResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return gson.toJson(response);
    }

    private String errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return gson.toJson(response);
    }

    // ========================= FRIENDSHIP ACTIONS =========================

    /**
     * Gửi lời mời kết bạn
     * Request: { "senderId": 1, "receiverId": 2 }
     */
//    public String sendFriendRequest(String requestJson) {
//        try {
//            Map<String, Number> request = gson.fromJson(requestJson, Map.class);
//            int senderId = request.get("senderId").intValue();
//            int receiverId = request.get("receiverId").intValue();
//
//            // Lấy user từ DAO
//            user sender = service.dao.em.find(user.class, senderId);
//            user receiver = service.dao.em.find(user.class, receiverId);
//
//            if (sender == null || receiver == null) {
//                return errorResponse("User không tồn tại");
//            }
//
//            service.sendFriendRequest(sender, receiver);
//            return successResponse("Gửi lời mời kết bạn thành công");
//        } catch (RuntimeException e) {
//            return errorResponse(e.getMessage());
//        } catch (Exception e) {
//            return errorResponse("Lỗi: " + e.getMessage());
//        }
//    }

    /**
     * Lấy danh sách bạn bè hoặc lời mời của một user
     * Request: { "userId": 1 }
     */
    public String getFriendsOfUser(String requestJson) {
        try {
            Map<String, Number> request = gson.fromJson(requestJson, Map.class);
            int userId = request.get("userId").intValue();

            List<friends> friendsList = service.getFriendsOfUser(userId);
            return successResponse(friendsList);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Chấp nhận lời mời kết bạn
     * Request: { "friendshipId": 10 }
     */
    public String acceptFriendRequest(String requestJson) {
        try {
            Map<String, Number> request = gson.fromJson(requestJson, Map.class);
            int friendshipId = request.get("friendshipId").intValue();

            boolean success = service.acceptFriendRequest(friendshipId);
            if (success) return successResponse("Chấp nhận lời mời thành công");
            else return errorResponse("Lời mời không tồn tại hoặc đã xử lý");
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Từ chối hoặc hủy lời mời kết bạn
     * Request: { "friendshipId": 10 }
     */
    public String rejectFriendRequest(String requestJson) {
        try {
            Map<String, Number> request = gson.fromJson(requestJson, Map.class);
            int friendshipId = request.get("friendshipId").intValue();

            boolean success = service.rejectFriendRequest(friendshipId);
            if (success) return successResponse("Từ chối lời mời thành công");
            else return errorResponse("Lời mời không tồn tại hoặc đã xử lý");
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa bạn bè
     * Request: { "friendshipId": 10 }
     */
    public String deleteFriendship(String requestJson) {
        try {
            Map<String, Number> request = gson.fromJson(requestJson, Map.class);
            int friendshipId = request.get("friendshipId").intValue();

            boolean success = service.deleteFriendship(friendshipId);
            if (success) return successResponse("Xóa bạn bè thành công");
            else return errorResponse("Xóa bạn bè thất bại");
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= ROUTE HANDLER =========================
    public String handleRequest(String action, String requestJson) {
        switch (action) {
//            case "sendFriendRequest":
//                return sendFriendRequest(requestJson);
            case "getFriendsOfUser":
                return getFriendsOfUser(requestJson);
            case "acceptFriendRequest":
                return acceptFriendRequest(requestJson);
            case "rejectFriendRequest":
                return rejectFriendRequest(requestJson);
            case "deleteFriendship":
                return deleteFriendship(requestJson);
            default:
                return errorResponse("Action không hợp lệ: " + action);
        }
    }
}
