
package com.chatapp.server.Controller;

import com.chatapp.server.Model.Entity.user;
import com.chatapp.server.Model.Service.userService;
import com.chatapp.server.Utils.GsonUtils;
import com.google.gson.Gson;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserController - Xử lý các request liên quan đến User
 * - Chỉ nhận request, parse JSON, gọi Service, và trả về response
 * - KHÔNG xử lý logic nghiệp vụ (logic thuộc về Service)
 */
public class userController {

    private final userService userService;
    private final Gson gson;

    public userController(EntityManagerFactory emf) {
        emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        this.userService = new userService(emf);
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

    // ========================= ĐĂNG NHẬP / ĐĂNG KÝ =========================

    /**
     * Đăng nhập
     * Request: { "username": "user1", "password": "123456" }
     */
    public String login(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            String username = (String) request.get("username");
            String password = (String) request.get("password");

            user loggedUser = userService.login(username, password);
            if (loggedUser != null) {
                return successResponse(loggedUser);
            } else {
                return errorResponse("Tên đăng nhập hoặc mật khẩu không đúng, hoặc user đang online");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Đăng ký tài khoản mới
     * Request: { "username": "newuser", "password": "123456" }
     */
    public String register(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            String username = (String) request.get("username");
            String password = (String) request.get("password");

            user newUser = userService.register(username, password);
            return successResponse(newUser);
        } catch (RuntimeException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Đăng nhập bằng Google
     * Request: { "googleId": "123456", "email": "user@gmail.com", "name": "User Name", "avatarUrl": "http://..." }
     *
     * ✅ ĐÚNG: Controller chỉ parse JSON và gọi Service
     * Logic "kiểm tra user tồn tại" đã được chuyển xuống Service
     *
     */
//    public String loginWithGoogle(String requestJson) {
//        try {
//            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
//            String googleId = (String) request.get("googleId");
//            String email = (String) request.get("email");
//            String name = (String) request.get("name");
//            String avatarUrl = (String) request.get("avatarUrl");
//
//            // Gọi Service xử lý logic
//            user resultUser = userService.loginOrRegisterWithGoogle(email, googleId, name, avatarUrl);
//            return successResponse(resultUser);
//        } catch (Exception e) {
//            return errorResponse("Lỗi: " + e.getMessage());
//        }
//    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Lấy danh sách tất cả user
     * Request: {}
     */
    public String getAllUsers() {
        try {
            List<user> users = userService.getAllUsers();
            return successResponse(users);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin user theo ID
     * Request: { "userId": 1 }
     */
    public String getUserById(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            user foundUser = userService.getUserById(userId);
            if (foundUser != null) {
                return successResponse(foundUser);
            } else {
                return errorResponse("Không tìm thấy user");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa user
     * Request: { "userId": 1 }
     */
    public String deleteUser(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            boolean success = userService.deleteUser(userId);
            if (success) {
                return successResponse("Xóa user thành công");
            } else {
                return errorResponse("Xóa user thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= CẬP NHẬT TRẠNG THÁI =========================

    /**
     * Cập nhật trạng thái user (Online/Offline/Busy)
     * Request: { "userId": 1, "status": "Online" }
     */
    public String updateStatus(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();
            String status = (String) request.get("status");

            boolean success = userService.updateStatus(userId, status);
            if (success) {
                return successResponse("Cập nhật trạng thái thành công");
            } else {
                return errorResponse("Cập nhật trạng thái thất bại");
            }
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
                // Đăng nhập / Đăng ký
                case "login":
                    return login(requestJson);
                case "register":
                    return register(requestJson);
//                case "loginWithGoogle":
//                    return loginWithGoogle(requestJson);

                // CRUD cơ bản
                case "getAllUsers":
                    return getAllUsers();
                case "getUserById":
                    return getUserById(requestJson);
                case "deleteUser":
                    return deleteUser(requestJson);

                // Cập nhật
                case "updateStatus":
                    return updateStatus(requestJson);

                default:
                    return errorResponse("Action không hợp lệ: " + action);
            }
        } catch (Exception e) {
            return errorResponse("Lỗi xử lý request: " + e.getMessage());
        }
    }
}