package com.database.server.Controller;

import com.database.server.DAO.roomDAO.RoomWithPlayer;
import com.database.server.DAO.roomDAO.RoomStatistics;
import com.database.server.Service.roomService;
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
 * Controller xử lý các request liên quan đến Room
 * Chuyển đổi giữa JSON và Object, gọi RoomService để xử lý logic
 */
public class roomController {

    private final roomService roomService;
    private final Gson gson;

    public roomController(EntityManagerFactory emf) {
        emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        this.roomService = new roomService(emf);
        this.gson = GsonUtils.gson;
    }

    // ========================= HELPER METHODS =========================

    /**
     * Tạo response thành công với data
     * @param data Dữ liệu trả về
     * @return JSON string chứa response
     */
    private String successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return gson.toJson(response);
    }

    /**
     * Tạo response thành công với message
     * @param message Thông báo
     * @return JSON string chứa response
     */
    private String successResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return gson.toJson(response);
    }

    /**
     * Tạo response lỗi
     * @param message Thông báo lỗi
     * @return JSON string chứa response lỗi
     */
    private String errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return gson.toJson(response);
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Lấy thông tin phòng theo ID
     * Request: { "roomId": 1 }
     * @param requestJson JSON request
     * @return JSON response chứa thông tin phòng
     */
    public String getRoomById(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int roomId = ((Number) request.get("roomId")).intValue();

            RoomWithPlayer room = roomService.getRoomById(roomId);
            if (room != null) {
                return successResponse(room);
            } else {
                return errorResponse("Không tìm thấy phòng");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách tất cả các phòng
     * Request: {}
     * @return JSON response chứa danh sách phòng
     */
    public String getAllRooms() {
        try {
            List<RoomWithPlayer> rooms = roomService.getAllRooms();
            return successResponse(rooms);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa phòng
     * Request: { "roomId": 1 }
     * @param requestJson JSON request
     * @return JSON response xác nhận xóa
     */
    public String deleteRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int roomId = ((Number) request.get("roomId")).intValue();

            boolean success = roomService.deleteRoom(roomId);
            if (success) {
                return successResponse("Xóa phòng thành công");
            } else {
                return errorResponse("Xóa phòng thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= TẠO VÀ THAM GIA PHÒNG =========================

    /**
     * Tạo phòng mới
     * Request: { "hostId": 1 }
     * @param requestJson JSON request
     * @return JSON response chứa thông tin phòng vừa tạo
     */
    public String createRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int hostId = ((Number) request.get("hostId")).intValue();

            RoomWithPlayer room = roomService.createRoom(hostId);
            if (room != null) {
                return successResponse(room);
            } else {
                return errorResponse("Tạo phòng thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tham gia phòng
     * Request: { "roomId": 1, "guestId": 2 }
     * @param requestJson JSON request
     * @return JSON response xác nhận tham gia
     */
    public String joinRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int roomId = ((Number) request.get("roomId")).intValue();
            int guestId = ((Number) request.get("guestId")).intValue();

            boolean success = roomService.joinRoom(roomId, guestId);
            if (success) {
                RoomWithPlayer room = roomService.getRoomById(roomId);
                return successResponse(room);
            } else {
                return errorResponse("Tham gia phòng thất bại. Phòng có thể đã đầy hoặc không tồn tại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Rời khỏi phòng
     * Request: { "roomId": 1, "userId": 2 }
     * @param requestJson JSON request
     * @return JSON response xác nhận rời phòng
     */
    public String leaveRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int roomId = ((Number) request.get("roomId")).intValue();
            int userId = ((Number) request.get("userId")).intValue();

            boolean success = roomService.leaveRoom(roomId, userId);
            if (success) {
                return successResponse("Rời phòng thành công");
            } else {
                return errorResponse("Rời phòng thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= CẬP NHẬT TRẠNG THÁI =========================

    /**
     * Cập nhật trạng thái phòng
     * Request: { "roomId": 1, "status": "Active" }
     * @param requestJson JSON request
     * @return JSON response xác nhận cập nhật
     */
    public String updateRoomStatus(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int roomId = ((Number) request.get("roomId")).intValue();
            String status = (String) request.get("status");

            boolean success = roomService.updateRoomStatus(roomId, status);
            if (success) {
                return successResponse("Cập nhật trạng thái thành công");
            } else {
                return errorResponse("Cập nhật trạng thái thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Đóng phòng
     * Request: { "roomId": 1 }
     * @param requestJson JSON request
     * @return JSON response xác nhận đóng phòng
     */
    public String closeRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int roomId = ((Number) request.get("roomId")).intValue();

            boolean success = roomService.closeRoom(roomId);
            if (success) {
                return successResponse("Đóng phòng thành công");
            } else {
                return errorResponse("Đóng phòng thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Mở lại phòng
     * Request: { "roomId": 1 }
     * @param requestJson JSON request
     * @return JSON response xác nhận mở lại phòng
     */
    public String reopenRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int roomId = ((Number) request.get("roomId")).intValue();

            boolean success = roomService.reopenRoom(roomId);
            if (success) {
                return successResponse("Mở lại phòng thành công");
            } else {
                return errorResponse("Mở lại phòng thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= TRUY VẤN THEO ĐIỀU KIỆN =========================

    /**
     * Lấy các phòng của một user
     * Request: { "userId": 1 }
     * @param requestJson JSON request
     * @return JSON response chứa danh sách phòng của user
     */
    public String getRoomsByUserId(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            List<RoomWithPlayer> rooms = roomService.getRoomsByUserId(userId);
            return successResponse(rooms);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy các phòng theo trạng thái
     * Request: { "status": "Waiting" }
     * @param requestJson JSON request
     * @return JSON response chứa danh sách phòng theo trạng thái
     */
    public String getRoomsByStatus(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            String status = (String) request.get("status");

            List<RoomWithPlayer> rooms = roomService.getRoomsByStatus(status);
            return successResponse(rooms);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách phòng đang chờ (có thể tham gia)
     * Request: {}
     * @return JSON response chứa danh sách phòng có thể tham gia
     */
    public String getAvailableRooms() {
        try {
            List<RoomWithPlayer> rooms = roomService.getAvailableRooms();
            return successResponse(rooms);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách phòng đang chơi
     * Request: {}
     * @return JSON response chứa danh sách phòng đang active
     */
    public String getActiveRooms() {
        try {
            List<RoomWithPlayer> rooms = roomService.getActiveRooms();
            return successResponse(rooms);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy các phòng được tạo sau một thời điểm
     * Request: { "afterTime": "2024-01-01T00:00:00" }
     * @param requestJson JSON request
     * @return JSON response chứa danh sách phòng
     */
    public String getRoomsCreatedAfter(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            String afterTimeStr = (String) request.get("afterTime");
            LocalDateTime afterTime = LocalDateTime.parse(afterTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            List<RoomWithPlayer> rooms = roomService.getRoomsCreatedAfter(afterTime);
            return successResponse(rooms);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= BUSINESS LOGIC =========================

    /**
     * Tìm trận nhanh (Quick Match)
     * - Tự động tìm phòng đang chờ hoặc tạo phòng mới
     * Request: { "userId": 1 }
     * @param requestJson JSON request
     * @return JSON response xác nhận quick match
     */
    public String quickMatch(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            boolean success = roomService.quickMatch(userId);
            if (success) {
                RoomWithPlayer currentRoom = roomService.getCurrentRoom(userId);
                return successResponse(currentRoom);
            } else {
                return errorResponse("Quick match thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra user có đang trong phòng không
     * Request: { "userId": 1 }
     * @param requestJson JSON request
     * @return JSON response chứa trạng thái
     */
    public String isUserInRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            boolean inRoom = roomService.isUserInRoom(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("inRoom", inRoom);

            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy phòng hiện tại của user
     * Request: { "userId": 1 }
     * @param requestJson JSON request
     * @return JSON response chứa thông tin phòng hiện tại
     */
    public String getCurrentRoom(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            RoomWithPlayer room = roomService.getCurrentRoom(userId);
            if (room != null) {
                return successResponse(room);
            } else {
                return errorResponse("User không ở trong phòng nào");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Lấy tổng số phòng
     * Request: {}
     * @return JSON response chứa tổng số phòng
     */
    public String getTotalRoomCount() {
        try {
            int count = roomService.getTotalRoomCount();
            Map<String, Object> data = new HashMap<>();
            data.put("totalRooms", count);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy số lượng phòng theo trạng thái
     * Request: { "status": "Waiting" }
     * @param requestJson JSON request
     * @return JSON response chứa số lượng phòng
     */
    public String getRoomCountByStatus(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            String status = (String) request.get("status");

            int count = roomService.getRoomCountByStatus(status);
            Map<String, Object> data = new HashMap<>();
            data.put("status", status);
            data.put("count", count);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy thống kê tổng quan về phòng
     * Request: {}
     * @return JSON response chứa thống kê chi tiết
     */
    public String getRoomStatistics() {
        try {
            RoomStatistics stats = roomService.getRoomStatistics();
            return successResponse(stats);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= ROUTE HANDLER =========================

    /**
     * Xử lý request dựa trên action
     * @param action Hành động cần thực hiện (ví dụ: "createRoom", "joinRoom")
     * @param requestJson JSON request data
     * @return JSON response
     */
    public String handleRequest(String action, String requestJson) {
        try {
            switch (action) {
                // CRUD cơ bản
                case "getRoomById":
                    return getRoomById(requestJson);
                case "getAllRooms":
                    return getAllRooms();
                case "deleteRoom":
                    return deleteRoom(requestJson);

                // Tạo và tham gia phòng
                case "createRoom":
                    return createRoom(requestJson);
                case "joinRoom":
                    return joinRoom(requestJson);
                case "leaveRoom":
                    return leaveRoom(requestJson);

                // Cập nhật trạng thái
                case "updateRoomStatus":
                    return updateRoomStatus(requestJson);
                case "closeRoom":
                    return closeRoom(requestJson);
                case "reopenRoom":
                    return reopenRoom(requestJson);

                // Truy vấn theo điều kiện
                case "getRoomsByUserId":
                    return getRoomsByUserId(requestJson);
                case "getRoomsByStatus":
                    return getRoomsByStatus(requestJson);
                case "getAvailableRooms":
                    return getAvailableRooms();
                case "getActiveRooms":
                    return getActiveRooms();
                case "getRoomsCreatedAfter":
                    return getRoomsCreatedAfter(requestJson);

                // Business logic
                case "quickMatch":
                    return quickMatch(requestJson);
                case "isUserInRoom":
                    return isUserInRoom(requestJson);
                case "getCurrentRoom":
                    return getCurrentRoom(requestJson);

                // Thống kê
                case "getTotalRoomCount":
                    return getTotalRoomCount();
                case "getRoomCountByStatus":
                    return getRoomCountByStatus(requestJson);
                case "getRoomStatistics":
                    return getRoomStatistics();

                default:
                    return errorResponse("Action không hợp lệ: " + action);
            }
        } catch (Exception e) {
            return errorResponse("Lỗi xử lý request: " + e.getMessage());
        }
    }
}