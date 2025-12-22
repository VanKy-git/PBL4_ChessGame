package com.database.server.Controller;

import com.database.server.Entity.user;
import com.database.server.Service.userService;
import com.database.server.Utils.GsonUtils;
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

    private String errorResponse(String message) {Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return gson.toJson(response);
    }


    // ========================= ĐĂNG NHẬP / ĐĂNG KÝ =========================

    public String login(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            Map<String, Object> loginResult = userService.login(username, password);
            return successResponse(loginResult);
        }
        catch (RuntimeException e) {
            return errorResponse("userController:" +e.getMessage());
        }
        catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

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

    // ========================= GOOGLE OAUTH =========================

    public String registerWithGoogle(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            String idToken = (String) request.get("idToken");

            if (idToken == null || idToken.isEmpty()) {
                return errorResponse("ID token is required");
            }

            user newUser = userService.registerWithGoogle(idToken); // có thể ném exception khi email đã tồn tại

            return successResponse(newUser);

        } catch (RuntimeException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Lỗi khi đăng ký bằng Google: " + e.getMessage());
        }
    }

    public String loginWithGoogle(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            String idToken = (String) request.get("idToken");

            if (idToken == null || idToken.isEmpty()) {
                return errorResponse("ID token is required");
            }

            user existingUser = userService.loginWithGoogle(idToken);

            if (existingUser == null) {
                // Không tìm thấy -> yêu cầu register
                return errorResponse("User not found. Please register with Google first.");
            }

            return successResponse(existingUser);

        } catch (RuntimeException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Lỗi khi đăng nhập bằng Google: " + e.getMessage());
        }
    }

//    public String loginWithGoogle(String requestJson) {
//        try {
//            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
//            String idToken = (String) request.get("idToken");
//
//            if (idToken == null || idToken.isEmpty()) {
//                return errorResponse("ID token is required");
//            }
//
//            user resultUser = userService.authenticateWithGoogle(idToken);
//            if (resultUser != null) {
//                return successResponse(resultUser);
//            } else {
//                return errorResponse("Google authentication failed");
//            }
//        } catch (RuntimeException e) {
//            return errorResponse(e.getMessage());
//        } catch (Exception e) {
//            return errorResponse("Lỗi: " + e.getMessage());
//        }
//    }

    public String linkGoogleAccount(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();
            String idToken = (String) request.get("idToken");

            if (idToken == null || idToken.isEmpty()) {
                return errorResponse("ID token is required");
            }

            boolean success = userService.linkGoogleToExistingAccount(userId, idToken);
            if (success) {
                return successResponse("Liên kết tài khoản Google thành công");
            } else {
                return errorResponse("Liên kết thất bại");
            }
        } catch (RuntimeException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    public String unlinkGoogleAccount(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = ((Number) request.get("userId")).intValue();

            boolean success = userService.unlinkGoogle(userId);
            if (success) {
                return successResponse("Hủy liên kết Google thành công");
            } else {
                return errorResponse("Hủy liên kết thất bại");
            }
        } catch (RuntimeException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= CRUD CƠ BẢN =========================

    public String getAllUsers() {
        try {
            List<user> users = userService.getAllUsers();
            return successResponse(users);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

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

    // ========================= BẢNG XẾP HẠNG =========================
    public String getLeaderboard() {
        try {
            List<user> users = userService.getLeaderboard();
            return successResponse(users);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= ROUTE HANDLER =========================

    public String handleRequest(String action, String requestJson) {
        try {
            switch (action) {
                // Đăng nhập / Đăng ký
                case "login":
                    return login(requestJson);
                case "register":
                    return register(requestJson);
                case "registerWithGoogle":
                    return registerWithGoogle(requestJson);
                case "loginWithGoogle":
                    return loginWithGoogle(requestJson);
                case "linkGoogleAccount":
                    return linkGoogleAccount(requestJson);
                case "unlinkGoogleAccount":
                    return unlinkGoogleAccount(requestJson);

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

                // Bảng xếp hạng
                case "getLeaderboard":
                    return getLeaderboard();

                default:
                    return errorResponse("Action không hợp lệ: " + action);
            }
        } catch (Exception e) {
            return errorResponse("Lỗi xử lý request: " + e.getMessage());
        }
    }
}
