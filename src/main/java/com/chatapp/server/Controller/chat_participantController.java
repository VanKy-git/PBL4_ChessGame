package com.chatapp.server.Controller;

import com.chatapp.server.Model.DAO.chat_participantDAO.ParticipantWithUser;
import com.chatapp.server.Model.Entity.chat_participant;
import com.chatapp.server.Model.Service.chat_participantService;
import com.chatapp.server.Utils.GsonUtils;
import com.google.gson.Gson;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * chat_participantController - Xử lý các request liên quan đến chat_participant
 * - Chỉ nhận request, parse JSON, gọi Service, và trả về response
 * - KHÔNG xử lý logic nghiệp vụ
 */
public class chat_participantController {

    private final chat_participantService participantService;
    private final Gson gson;

    public chat_participantController(EntityManagerFactory emf ) {
        emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        this.participantService = new chat_participantService(emf);
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

    // ========================= THÊM / XÓA PARTICIPANT =========================

    /**
     * Thêm user vào chat room
     * Request: { "chatroomId": 1, "userId": 2 }
     */
    public String addParticipant(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();
            int userId = ((Number) request.get("userId")).intValue();

            chat_participant participant = participantService.addParticipant(chatroomId, userId);
            return successResponse(participant);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa user khỏi chat room
     * Request: { "chatroomId": 1, "userId": 2 }
     */
    public String removeParticipant(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();
            int userId = ((Number) request.get("userId")).intValue();

            boolean success = participantService.removeParticipant(chatroomId, userId);
            if (success) {
                return successResponse("Xóa participant thành công");
            } else {
                return errorResponse("Xóa participant thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= TRUY VẤN =========================

    /**
     * Lấy participant theo composite key
     * Request: { "chatroomId": 1, "userId": 2 }
     */
    public String getParticipant(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();
            int userId = ((Number) request.get("userId")).intValue();

            chat_participant participant = participantService.getParticipant(chatroomId, userId);
            if (participant != null) {
                return successResponse(participant);
            } else {
                return errorResponse("Không tìm thấy participant");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả participants trong chat room
     * Request: { "chatroomId": 1 }
     */
    public String getParticipantsByRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            List<chat_participant> participants = participantService.getParticipantsByRoom(chatroomId);
            return successResponse(participants);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả chat rooms mà user tham gia
     * Request: { "userId": 1 }
     */
    public String getParticipantsByUser(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            List<chat_participant> participants = participantService.getParticipantsByUser(userId);
            return successResponse(participants);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy participants với thông tin user đầy đủ
     * Request: { "chatroomId": 1 }
     */
    public String getParticipantsWithUserInfo(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            List<ParticipantWithUser> participants = participantService.getParticipantsWithUserInfo(chatroomId);
            return successResponse(participants);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= KIỂM TRA =========================

    /**
     * Kiểm tra user có phải là participant không
     * Request: { "chatroomId": 1, "userId": 2 }
     */
    public String isParticipant(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();
            int userId = ((Number) request.get("userId")).intValue();

            boolean isParticipant = participantService.isParticipant(chatroomId, userId);
            Map<String, Object> data = new HashMap<>();
            data.put("isParticipant", isParticipant);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Đếm số participants trong chat room
     * Request: { "chatroomId": 1 }
     */
    public String countParticipantsByRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int chatroomId = ((Number) request.get("chatroomId")).intValue();

            int count = participantService.countParticipantsByRoom(chatroomId);
            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Đếm số chat rooms mà user tham gia
     * Request: { "userId": 1 }
     */
    public String countRoomsByUser(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            int count = participantService.countRoomsByUser(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            return successResponse(data);
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
                // Thêm / Xóa
                case "addParticipant":
                    return addParticipant(requestJson);
                case "removeParticipant":
                    return removeParticipant(requestJson);

                // Truy vấn
                case "getParticipant":
                    return getParticipant(requestJson);
                case "getParticipantsByRoom":
                    return getParticipantsByRoom(requestJson);
                case "getParticipantsByUser":
                    return getParticipantsByUser(requestJson);
                case "getParticipantsWithUserInfo":
                    return getParticipantsWithUserInfo(requestJson);

                // Kiểm tra
                case "isParticipant":
                    return isParticipant(requestJson);

                // Thống kê
                case "countParticipantsByRoom":
                    return countParticipantsByRoom(requestJson);
                case "countRoomsByUser":
                    return countRoomsByUser(requestJson);

                default:
                    return errorResponse("Action không hợp lệ: " + action);
            }
        } catch (Exception e) {
            return errorResponse("Lỗi xử lý request: " + e.getMessage());
        }
    }
}