//
//package com.chatapp.server.Controller;
//
//import com.chatapp.server.Model.Entity.user;
//import com.chatapp.server.Model.Service.userService;
//import com.chatapp.server.Utils.GsonUtils;
//import com.google.gson.Gson;
//import jakarta.persistence.EntityManagerFactory;
//import jakarta.persistence.Persistence;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * UserController - Xử lý các request liên quan đến User
// * - Chỉ nhận request, parse JSON, gọi Service, và trả về response
// * - KHÔNG xử lý logic nghiệp vụ (logic thuộc về Service)
// */
//public class userController {
//
//    private final userService userService;
//    private final Gson gson;
//
//    public userController(EntityManagerFactory emf) {
//        emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
//        this.userService = new userService(emf);
//        this.gson = GsonUtils.gson;
//    }
//
//    // ========================= HELPER METHODS =========================
//
//    /**
//     * Tạo response thành công với data
//     */
//    private String successResponse(Object data) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("data", data);
//        return gson.toJson(response);
//    }
//
//    /**
//     * Tạo response thành công với message
//     */
//    private String successResponse(String message) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("message", message);
//        return gson.toJson(response);
//    }
//
//    /**
//     * Tạo response lỗi
//     */
//    private String errorResponse(String message) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", false);
//        response.put("error", message);
//        return gson.toJson(response);
//    }
//
//    // ========================= ĐĂNG NHẬP / ĐĂNG KÝ =========================
//
//    /**
//     * Đăng nhập
//     * Request: { "username": "user1", "password": "123456" }
//     */
//    public String login(String requestJson) {
//        try {
//            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
//            String username = (String) request.get("username");
//            String password = (String) request.get("password");
//
//            user loggedUser = userService.login(username, password);
//            if (loggedUser != null) {
//                return successResponse(loggedUser);
//            } else {
//                return errorResponse("Tên đăng nhập hoặc mật khẩu không đúng, hoặc user đang online");
//            }
//        } catch (Exception e) {
//            return errorResponse("Lỗi: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Đăng ký tài khoản mới
//     * Request: { "username": "newuser", "password": "123456" }
//     */
//    public String register(String requestJson) {
//        try {
//            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
//            String username = (String) request.get("username");
//            String password = (String) request.get("password");
//
//            user newUser = userService.register(username, password);
//            return successResponse(newUser);
//        } catch (RuntimeException e) {
//            return errorResponse(e.getMessage());
//        } catch (Exception e) {
//            return errorResponse("Lỗi: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Đăng nhập bằng Google
//     * Request: { "googleId": "123456", "email": "user@gmail.com", "name": "User Name", "avatarUrl": "http://..." }
//     *
//     * ✅ ĐÚNG: Controller chỉ parse JSON và gọi Service
//     * Logic "kiểm tra user tồn tại" đã được chuyển xuống Service
//     *
//     */
////    public String loginWithGoogle(String requestJson) {
////        try {
////            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
////            String googleId = (String) request.get("googleId");
////            String email = (String) request.get("email");
////            String name = (String) request.get("name");
////            String avatarUrl = (String) request.get("avatarUrl");
////
////            // Gọi Service xử lý logic
////            user resultUser = userService.loginOrRegisterWithGoogle(email, googleId, name, avatarUrl);
////            return successResponse(resultUser);
////        } catch (Exception e) {
////            return errorResponse("Lỗi: " + e.getMessage());
////        }
////    }
//
//    // ========================= CRUD CƠ BẢN =========================
//
//    /**
//     * Lấy danh sách tất cả user
//     * Request: {}
//     */
//    public String getAllUsers() {
//        try {
//            List<user> users = userService.getAllUsers();
//            return successResponse(users);
//        } catch (Exception e) {
//            return errorResponse("Lỗi: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Lấy thông tin user theo ID
//     * Request: { "userId": 1 }
//     */
//    public String getUserById(String requestJson) {
//        try {
//            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
//            int userId = ((Number) request.get("userId")).intValue();
//
//            user foundUser = userService.getUserById(userId);
//            if (foundUser != null) {
//                return successResponse(foundUser);
//            } else {
//                return errorResponse("Không tìm thấy user");
//            }
//        } catch (Exception e) {
//            return errorResponse("Lỗi: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Xóa user
//     * Request: { "userId": 1 }
//     */
//    public String deleteUser(String requestJson) {
//        try {
//            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
//            int userId = ((Number) request.get("userId")).intValue();
//
//            boolean success = userService.deleteUser(userId);
//            if (success) {
//                return successResponse("Xóa user thành công");
//            } else {
//                return errorResponse("Xóa user thất bại");
//            }
//        } catch (Exception e) {
//            return errorResponse("Lỗi: " + e.getMessage());
//        }
//    }
//
//    // ========================= CẬP NHẬT TRẠNG THÁI =========================
//
//    /**
//     * Cập nhật trạng thái user (Online/Offline/Busy)
//     * Request: { "userId": 1, "status": "Online" }
//     */
//    public String updateStatus(String requestJson) {
//        try {
//            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
//            int userId = ((Number) request.get("userId")).intValue();
//            String status = (String) request.get("status");
//
//            boolean success = userService.updateStatus(userId, status);
//            if (success) {
//                return successResponse("Cập nhật trạng thái thành công");
//            } else {
//                return errorResponse("Cập nhật trạng thái thất bại");
//            }
//        } catch (Exception e) {
//            return errorResponse("Lỗi: " + e.getMessage());
//        }
//    }
//
//    // ========================= ROUTE HANDLER =========================
//
//    /**
//     * Xử lý request dựa trên action
//     * @param action Hành động cần thực hiện
//     * @param requestJson JSON request data
//     * @return JSON response
//     */
//    public String handleRequest(String action, String requestJson) {
//        try {
//            switch (action) {
//                // Đăng nhập / Đăng ký
//                case "login":
//                    return login(requestJson);
//                case "register":
//                    return register(requestJson);
////                case "loginWithGoogle":
////                    return loginWithGoogle(requestJson);
//
//                // CRUD cơ bản
//                case "getAllUsers":
//                    return getAllUsers();
//                case "getUserById":
//                    return getUserById(requestJson);
//                case "deleteUser":
//                    return deleteUser(requestJson);
//
//                // Cập nhật
//                case "updateStatus":
//                    return updateStatus(requestJson);
//
//                default:
//                    return errorResponse("Action không hợp lệ: " + action);
//            }
//        } catch (Exception e) {
//            return errorResponse("Lỗi xử lý request: " + e.getMessage());
//        }
//    }
//}

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

            // File trên trả về Map (loginResult) chứ không phải đối tượng User thuần
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

            // Gọi service để đăng ký
            user newUser = userService.registerWithGoogle(idToken);

            // Tạo Map response đầy đủ thông tin để trả về client
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", newUser.getUserId());
            responseData.put("username", newUser.getUserName());
            responseData.put("email", newUser.getEmail());
            responseData.put("avatar", newUser.getAvatarUrl()); // Hoặc newUser.getAvatarUrl() tùy entity của bạn
            responseData.put("status", newUser.getStatus());
            responseData.put("elo", newUser.getEloRating()); // Nếu có trường điểm ELO

            return successResponse(responseData);

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

            // Gọi service để tìm user
            user existingUser = userService.loginWithGoogle(idToken);

            if (existingUser == null) {
                return errorResponse("Tài khoản không tồn tại. Vui lòng đăng ký trước!");
            }

            // Tạo Map response đầy đủ thông tin (Giống hệt register)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", existingUser.getUserId());
            responseData.put("username", existingUser.getUserName());
            responseData.put("email", existingUser.getEmail());
            responseData.put("avatar", existingUser.getAvatarUrl()); // Hoặc existingUser.getAvatarUrl()
            responseData.put("status", existingUser.getStatus());
            responseData.put("elo", existingUser.getEloRating());

            return successResponse(responseData);

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
            if (!request.containsKey("userId")) return errorResponse("Thiếu tham số userId!");
            if (!request.containsKey("status")) return errorResponse("Thiếu tham số status!");

            // Logic mới: Xử lý trường hợp Gson parse số thành Double
            Object userIdObj = request.get("userId");
            int userId;
            if (userIdObj instanceof Double) userId = ((Double) userIdObj).intValue();
            else userId = Integer.parseInt(userIdObj.toString());

            String status = (String) request.get("status");
            boolean success = userService.updateStatus(userId, status);

            if (success) return successResponse("Cập nhật trạng thái thành công");
            else return errorResponse("Cập nhật trạng thái thất bại - User không tồn tại");

        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // Hàm cập nhật thông tin tài khoản (Username, Email, Avatar)
    public String updateAccount(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            // Xử lý parse ID an toàn hơn
            int userId = Integer.parseInt(request.get("playerId").toString());
            String username = (String) request.get("username");
            String email = (String) request.get("email");
            String avatarUrl = (String) request.get("avatarUrl");

            boolean success = userService.updateAccount(userId, username, email, avatarUrl);
            if (success) {
                user updatedUser = userService.getUserById(userId);
                return successResponse(updatedUser);
            } else {
                return errorResponse("Cập nhật tài khoản thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // Hàm đổi mật khẩu
    public String changePassword(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int userId = Integer.parseInt(request.get("playerId").toString());
            String oldPassword = (String) request.get("oldPassword");
            String newPassword = (String) request.get("newPassword");

            boolean success = userService.changePassword(userId, oldPassword, newPassword);
            if (success) return successResponse("Đổi mật khẩu thành công");
            else return errorResponse("Đổi mật khẩu thất bại");
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

                case "updateAccount":
                    return updateAccount(requestJson);
                case "changePassword":
                    return changePassword(requestJson);

                default:
                    return errorResponse("Action không hợp lệ: " + action);
            }
        } catch (Exception e) {
            return errorResponse("Lỗi xử lý request: " + e.getMessage());
        }
    }
}