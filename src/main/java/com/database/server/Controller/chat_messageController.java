package com.database.server.Controller;

import com.database.server.DAO.chat_messageDAO.MessageWithUser;
import com.database.server.Entity.chat_message;
import com.database.server.Service.chat_messageService;
import com.database.server.Utils.GsonUtils;
import com.google.gson.Gson;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * chat_messageController - Xử lý các request liên quan đến chat_message
 * - Chỉ nhận request, parse JSON, gọi Service, và trả về response
 * - KHÔNG xử lý logic nghiệp vụ
 */
public class chat_messageController {

    private final chat_messageService messageService;
    private final Gson gson;

    public chat_messageController(EntityManagerFactory emf ) {
        emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        this.messageService = new chat_messageService(emf);
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

    // ========================= GỬI MESSAGE =========================

    /**
     * Gửi tin nhắn mới
     * Request: { "chatroomId": 1, "userId": 2, "message": "Hello" }
     */
    public String sendMessage(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();
            int userId = ((Number) request.get("userId")).intValue();
            String message = (String) request.get("message");

            MessageWithUser msg = messageService.sendMessage(chatroomId, userId, message);
            return successResponse(msg);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Lấy message theo ID
     * Request: { "messageId": 1 }
     */
    public String getMessageById(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int messageId = ((Number) request.get("messageId")).intValue();

            chat_message msg = messageService.getMessageById(messageId);
            if (msg != null) {
                return successResponse(msg);
            } else {
                return errorResponse("Không tìm thấy message");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa message
     * Request: { "messageId": 1 }
     */
    public String deleteMessage(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int messageId = ((Number) request.get("messageId")).intValue();

            boolean success = messageService.deleteMessage(messageId);
            if (success) {
                return successResponse("Xóa message thành công");
            } else {
                return errorResponse("Xóa message thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Cập nhật message
     * Request: { "messageId": 1, "newMessage": "Updated text" }
     */
    public String updateMessage(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int messageId = ((Number) request.get("messageId")).intValue();
            String newMessage = (String) request.get("newMessage");

            boolean success = messageService.updateMessage(messageId, newMessage);
            if (success) {
                return successResponse("Cập nhật message thành công");
            } else {
                return errorResponse("Cập nhật message thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= TRUY VẤN =========================

    /**
     * Lấy tất cả messages trong chat room
     * Request: { "chatroomId": 1 }
     */
    public String getMessagesByRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            List<MessageWithUser> messages = messageService.getMessagesByRoom(chatroomId);
            return successResponse(messages);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy messages của user
     * Request: { "userId": 1 }
     */
    public String getMessagesByUser(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            List<chat_message> messages = messageService.getMessagesByUser(userId);
            return successResponse(messages);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy messages trong khoảng thời gian
     * Request: { "chatroomId": 1, "startTime": "2024-01-01T00:00:00", "endTime": "2024-12-31T23:59:59" }
     */
    public String getMessagesByTimeRange(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();
            String startTimeStr = (String) request.get("startTime");
            String endTimeStr = (String) request.get("endTime");

            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            List<chat_message> messages = messageService.getMessagesByTimeRange(chatroomId, startTime, endTime);
            return successResponse(messages);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy messages gần đây
     * Request: { "chatroomId": 1, "limit": 50 }
     */
    public String getRecentMessages(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();
            int limit = ((Number) request.get("limit")).intValue();

            List<chat_message> messages = messageService.getRecentMessages(chatroomId, limit);
            return successResponse(messages);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy messages kèm thông tin user
     * Request: { "chatroomId": 1 }
     */
    public String getMessagesWithUserInfo(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            List<MessageWithUser> messages = messageService.getMessagesWithUserInfo(chatroomId);
            return successResponse(messages);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tìm kiếm messages
     * Request: { "chatroomId": 1, "keyword": "hello" }
     */
    public String searchMessages(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();
            String keyword = (String) request.get("keyword");

            List<chat_message> messages = messageService.searchMessages(chatroomId, keyword);
            return successResponse(messages);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Đếm số messages trong chat room
     * Request: { "chatroomId": 1 }
     */
    public String countMessagesByRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            int count = messageService.countMessagesByRoom(chatroomId);
            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Đếm số messages của user
     * Request: { "userId": 1 }
     */
    public String countMessagesByUser(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            int count = messageService.countMessagesByUser(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy message cuối cùng
     * Request: { "chatroomId": 1 }
     */
    public String getLastMessage(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            chat_message msg = messageService.getLastMessage(chatroomId);
            if (msg != null) {
                return successResponse(msg);
            } else {
                return errorResponse("Không có message nào");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= ROUTE HANDLER =========================

    /**
     * Xử lý request dựa trên action
     */
    public String handleRequest(String action, String requestJson) {
        try {
            switch (action) {
                // Gửi message
                case "sendMessage":
                    return sendMessage(requestJson);

                // CRUD
                case "getMessageById":
                    return getMessageById(requestJson);
                case "deleteMessage":
                    return deleteMessage(requestJson);
                case "updateMessage":
                    return updateMessage(requestJson);

                // Truy vấn
                case "getMessagesByRoom":
                    return getMessagesByRoom(requestJson);
                case "getMessagesByUser":
                    return getMessagesByUser(requestJson);
                case "getMessagesByTimeRange":
                    return getMessagesByTimeRange(requestJson);
                case "getRecentMessages":
                    return getRecentMessages(requestJson);
                case "getMessagesWithUserInfo":
                    return getMessagesWithUserInfo(requestJson);
                case "searchMessages":
                    return searchMessages(requestJson);

                // Thống kê
                case "countMessagesByRoom":
                    return countMessagesByRoom(requestJson);
                case "countMessagesByUser":
                    return countMessagesByUser(requestJson);
                case "getLastMessage":
                    return getLastMessage(requestJson);

                default:
                    return errorResponse("Action không hợp lệ: " + action);
            }
        } catch (Exception e) {
            return errorResponse("Lỗi xử lý request: " + e.getMessage());
        }
    }
}