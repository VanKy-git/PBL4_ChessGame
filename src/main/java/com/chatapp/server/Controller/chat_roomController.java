package com.chatapp.server.Controller;

import com.chatapp.server.Model.DAO.chat_roomDAO.ChatRoomDetails;
import com.chatapp.server.Model.DAO.chat_roomDAO.ChatRoomStatistics;
import com.chatapp.server.Model.Entity.chat_room;
import com.chatapp.server.Service.chat_roomService;
import com.chatapp.server.Utils.GsonUtils;
import com.google.gson.Gson;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * chat_roomController - Xử lý các request liên quan đến chat_room
 * - Chỉ nhận request, parse JSON, gọi Service, và trả về response
 * - KHÔNG xử lý logic nghiệp vụ
 */
public class chat_roomController {

    private final chat_roomService chatRoomService;
    private final Gson gson;

    public chat_roomController() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        this.chatRoomService = new chat_roomService(emf);
        this.gson = GsonUtils.gson;
    }

    // ========================= HELPER METHODS =========================

    /**
     * Tạo response thành công với data
     */
    private String successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return gson.toJson(response);
    }

    /**
     * Tạo response thành công với message
     */
    private String successResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return gson.toJson(response);
    }

    /**
     * Tạo response lỗi
     */
    private String errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return gson.toJson(response);
    }

    // ========================= TẠO PHÒNG CHAT =========================

    /**
     * Tạo hoặc lấy phòng chat 1-1
     * Request: { "user1Id": 1, "user2Id": 2 }
     */
    public String createPrivateRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int user1Id = ((Number) request.get("user1Id")).intValue();
            int user2Id = ((Number) request.get("user2Id")).intValue();

            chat_room room = chatRoomService.createOrGetPrivateRoom(user1Id, user2Id);
            return successResponse(room);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tạo phòng chat dựa trên room game
     * Request: { "roomId": 1 }
     */
    public String createRoomBasedChat(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int roomId = ((Number) request.get("roomId")).intValue();

            chat_room room = chatRoomService.createRoomBasedChat(roomId);
            return successResponse(room);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Lấy chat room theo ID
     * Request: { "chatroomId": 1 }
     */
    public String getChatRoomById(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            chat_room room = chatRoomService.getChatRoomById(chatroomId);
            if (room != null) {
                return successResponse(room);
            } else {
                return errorResponse("Không tìm thấy chat room");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả chat rooms
     * Request: {}
     */
    public String getAllChatRooms() {
        try {
            List<chat_room> rooms = chatRoomService.getAllChatRooms();
            return successResponse(rooms);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa chat room
     * Request: { "chatroomId": 1 }
     */
    public String deleteChatRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            boolean success = chatRoomService.deleteChatRoom(chatroomId);
            if (success) {
                return successResponse("Xóa chat room thành công");
            } else {
                return errorResponse("Xóa chat room thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= TRUY VẤN =========================

    /**
     * Lấy các chat rooms của user
     * Request: { "userId": 1 }
     */
    public String getChatRoomsByUserId(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            List<chat_room> rooms = chatRoomService.getChatRoomsByUserId(userId);
            return successResponse(rooms);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin chi tiết chat room
     * Request: { "chatroomId": 1 }
     */
    public String getChatRoomDetails(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            ChatRoomDetails details = chatRoomService.getChatRoomDetails(chatroomId);
            if (details != null) {
                return successResponse(details);
            } else {
                return errorResponse("Không tìm thấy chat room");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy chat room theo ref_id
     * Request: { "refId": 1 }
     */
    public String getChatRoomByRefId(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int refId = ((Number) request.get("refId")).intValue();

            chat_room room = chatRoomService.getChatRoomByRefId(refId);
            if (room != null) {
                return successResponse(room);
            } else {
                return errorResponse("Không tìm thấy chat room");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= KIỂM TRA =========================

    /**
     * Kiểm tra user có trong chat room không
     * Request: { "chatroomId": 1, "userId": 1 }
     */
    public String isUserInChatRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();
            int userId = ((Number) request.get("userId")).intValue();

            boolean isIn = chatRoomService.isUserInChatRoom(chatroomId, userId);
            Map<String, Object> data = new HashMap<>();
            data.put("isInRoom", isIn);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Lấy thống kê tổng quan
     * Request: {}
     */
    public String getChatRoomStatistics() {
        try {
            ChatRoomStatistics stats = chatRoomService.getChatRoomStatistics();
            return successResponse(stats);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= ROUTE HANDLER =========================

    /**
     * Xử lý request dựa trên action
     * @param action Hành động cần thực hiện
     * @param requestJson JSON request data
     * @return JSON response
     */
    public String handleRequest(String action, String requestJson) {
        try {
            switch (action) {
                // Tạo phòng
                case "createPrivateRoom":
                    return createPrivateRoom(requestJson);
                case "createRoomBasedChat":
                    return createRoomBasedChat(requestJson);

                // CRUD
                case "getChatRoomById":
                    return getChatRoomById(requestJson);
                case "getAllChatRooms":
                    return getAllChatRooms();
                case "deleteChatRoom":
                    return deleteChatRoom(requestJson);

                // Truy vấn
                case "getChatRoomsByUserId":
                    return getChatRoomsByUserId(requestJson);
                case "getChatRoomDetails":
                    return getChatRoomDetails(requestJson);
                case "getChatRoomByRefId":
                    return getChatRoomByRefId(requestJson);

                // Kiểm tra
                case "isUserInChatRoom":
                    return isUserInChatRoom(requestJson);

                // Thống kê
                case "getChatRoomStatistics":
                    return getChatRoomStatistics();

                default:
                    return errorResponse("Action không hợp lệ: " + action);
            }
        } catch (Exception e) {
            return errorResponse("Lỗi xử lý request: " + e.getMessage());
        }
    }
}