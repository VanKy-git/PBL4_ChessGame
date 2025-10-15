package com.chatapp.server.Controller;

import Model.DAO.userDAO;
import Model.Entity.user;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class userController {

    private userDAO userDAO;
    private Gson gson;

    public userController(Connection conn) {
        this.userDAO = new userDAO(conn);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                                new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class,
                        (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                                LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .create();
        ;
    }

    // ========== ĐĂNG NHẬP/ĐĂNG KÝ THÔNG THƯỜNG ==========

    //Đăng nhập username - password
    public JsonObject handleLogin(String username, String password) {
        JsonObject response = new JsonObject();

        try {
            user user = userDAO.login(username, password);

            if (user != null) {
                response.addProperty("status", "success");
                response.addProperty("message", "Đăng nhập thành công");
                response.addProperty("loginMethod", "normal");
                response.add("user", gson.toJsonTree(user));

                // Cập nhật status sang online nếu cần
                userDAO.updateStatus(user.getUser_id(), "Online");
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Sai tên đăng nhập hoặc mật khẩu");
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    //Đăng kí tài khoản với username - password
    public JsonObject handleRegister(String username, String password, String email) {
        JsonObject response = new JsonObject();

        try {
            // Kiểm tra username đã tồn tại
            if (userDAO.isUsernameExists(username)) {
                response.addProperty("status", "error");
                response.addProperty("message", "Tên đăng nhập đã tồn tại");
                return response;
            }

            // Kiểm tra email đã tồn tại (nếu có)
//            if (email != null && !email.isEmpty() && userDAO.isEmailExists(email)) {
//                response.addProperty("status", "error");
//                response.addProperty("message", "Email đã được sử dụng");
//                return response;
//            }

            // Validate
            if (!isValidUsername(username)) {
                response.addProperty("status", "error");
                response.addProperty("message", "Tên đăng nhập không hợp lệ (3-20 ký tự, chỉ chữ và số)");
                return response;
            }

            if (!isValidPassword(password)) {
                response.addProperty("status", "error");
                response.addProperty("message", "Mật khẩu không hợp lệ (tối thiểu 6 ký tự)");
                return response;
            }

            // Tạo tài khoản
            user newUser = userDAO.createUser(username, password);

            if (newUser != null) {
                response.addProperty("status", "success");
                response.addProperty("message", "Đăng ký thành công");
                response.add("user", gson.toJsonTree(newUser));
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể tạo tài khoản");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    //Đăng xuất
    public JsonObject handleLogout(int userId) {
        JsonObject response = new JsonObject();

        try {
            boolean success = userDAO.updateStatus(userId, "Oline");

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Đăng xuất thành công");
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể đăng xuất");
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== GOOGLE OAUTH ==========

    //Đăng nhập Google Oauth
    public JsonObject handleGoogleLogin(String googleToken) {
        JsonObject response = new JsonObject();

        try {
            GoogleUserInfo googleInfo = verifyGoogleToken(googleToken);

            if (googleInfo == null) {
                response.addProperty("status", "error");
                response.addProperty("message", "Token Google không hợp lệ");
                return response;
            }

            user existingUser = userDAO.getUserByGoogleId(googleInfo.googleId);

            if (existingUser != null) {
                response.addProperty("status", "success");
                response.addProperty("message", "Đăng nhập Google thành công");
                response.addProperty("loginMethod", "google");
                response.add("user", gson.toJsonTree(existingUser));
                userDAO.updateStatus(existingUser.getUser_id(), "Online");
                userDAO.updateProvider(existingUser.getUser_id(), "google account");//
            } else {
                user newUser = userDAO.createUserWithGoogle(
                        googleInfo.name,
                        googleInfo.email,
                        googleInfo.googleId,
                        googleInfo.picture
                );

                if (newUser != null) {
                    response.addProperty("status", "success");
                    response.addProperty("message", "Đăng ký Google thành công");
                    response.addProperty("loginMethod", "google");
                    response.addProperty("isNewUser", true);
                    response.add("user", gson.toJsonTree(newUser));
                } else {
                    response.addProperty("status", "error");
                    response.addProperty("message", "Không thể tạo tài khoản từ Google");
                }
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi xác thực Google: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Liên kết tài khoản với Google
     */
    public JsonObject linkGoogleAccount(int userId, String googleToken) {
        JsonObject response = new JsonObject();

        try {
            GoogleUserInfo googleInfo = verifyGoogleToken(googleToken);

            if (googleInfo == null) {
                response.addProperty("status", "error");
                response.addProperty("message", "Token Google không hợp lệ");
                return response;
            }

            user existingUser = userDAO.getUserByGoogleId(googleInfo.googleId);
            if (existingUser != null && existingUser.getUser_id() != userId) {
                response.addProperty("status", "error");
                response.addProperty("message", "Tài khoản Google đã được liên kết với tài khoản khác");
                return response;
            }

            boolean success = userDAO.linkGoogleAccount(userId, googleInfo.googleId);

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Liên kết tài khoản Google thành công");
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể liên kết tài khoản Google");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi xác thực Google: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Hủy liên kết tài khoản Google
     */
    public JsonObject unlinkGoogleAccount(int userId) {
        JsonObject response = new JsonObject();

        try {
            user user = userDAO.getUser(userId);
            if (user == null) {
                response.addProperty("status", "error");
                response.addProperty("message", "Không tìm thấy người dùng");
                return response;
            }

            if (!userDAO.hasPassword(userId)) {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể hủy liên kết. Vui lòng đặt mật khẩu trước");
                return response;
            }

            boolean success = userDAO.unlinkGoogleAccount(userId);

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Hủy liên kết tài khoản Google thành công");
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể hủy liên kết tài khoản Google");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== CẬP NHẬT THÔNG TIN USER ==========

    /**
     * Cập nhật username
     */
//    public JsonObject updateUsername(int userId, String newUsername) {
//        JsonObject response = new JsonObject();
//
//        try {
//            if (!isValidUsername(newUsername)) {
//                response.addProperty("status", "error");
//                response.addProperty("message", "Tên đăng nhập không hợp lệ");
//                return response;
//            }
//
//            if (userDAO.isUsernameExists(newUsername)) {
//                response.addProperty("status", "error");
//                response.addProperty("message", "Tên đăng nhập đã tồn tại");
//                return response;
//            }
//
//            boolean success = userDAO.updateUsername(userId, newUsername);
//
//            if (success) {
//                response.addProperty("status", "success");
//                response.addProperty("message", "Cập nhật username thành công");
//                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
//            } else {
//                response.addProperty("status", "error");
//                response.addProperty("message", "Không thể cập nhật username");
//            }
//
//        } catch (SQLException e) {
//            response.addProperty("status", "error");
//            response.addProperty("message", "Lỗi database: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return response;
//    }

    /**
     * Cập nhật email
     */
//    public JsonObject updateEmail(int userId, String newEmail) {
//        JsonObject response = new JsonObject();
//
//        try {
//            if (newEmail == null || !isValidEmail(newEmail)) {
//                response.addProperty("status", "error");
//                response.addProperty("message", "Email không hợp lệ");
//                return response;
//            }
//
//            if (userDAO.isEmailExists(newEmail)) {
//                response.addProperty("status", "error");
//                response.addProperty("message", "Email đã được sử dụng");
//                return response;
//            }
//
//            boolean success = userDAO.updateEmail(userId, newEmail);
//
//            if (success) {
//                response.addProperty("status", "success");
//                response.addProperty("message", "Cập nhật email thành công");
//                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
//            } else {
//                response.addProperty("status", "error");
//                response.addProperty("message", "Không thể cập nhật email");
//            }
//
//        } catch (SQLException e) {
//            response.addProperty("status", "error");
//            response.addProperty("message", "Lỗi database: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return response;
//    }

    /**
     * Đổi mật khẩu
     */
    public JsonObject changePassword(int userId, String oldPassword, String newPassword) {
        JsonObject response = new JsonObject();

        try {
            user user = userDAO.getUser(userId);
            if (user == null) {
                response.addProperty("status", "error");
                response.addProperty("message", "Không tìm thấy người dùng");
                return response;
            }

            // Kiểm tra mật khẩu cũ
            if (user.getPassword() != null && !user.getPassword().equals(oldPassword)) {
                response.addProperty("status", "error");
                response.addProperty("message", "Mật khẩu cũ không đúng");
                return response;
            }

            if (!isValidPassword(newPassword)) {
                response.addProperty("status", "error");
                response.addProperty("message", "Mật khẩu mới không hợp lệ (tối thiểu 6 ký tự)");
                return response;
            }

            boolean success = userDAO.updatePassword(userId, newPassword);

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Đổi mật khẩu thành công");
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể đổi mật khẩu");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Cập nhật avatar
     */
    public JsonObject updateAvatar(int userId, String avatarUrl) {
        JsonObject response = new JsonObject();

        try {
            boolean success = userDAO.updateAvatar(userId, avatarUrl);

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Cập nhật avatar thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể cập nhật avatar");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Cập nhật toàn bộ thông tin user
     */
    public JsonObject updateUserProfile(user updatedUser) {
        JsonObject response = new JsonObject();

        try {
            boolean success = userDAO.updateUser(updatedUser);

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Cập nhật thông tin thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(updatedUser.getUser_id())));
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể cập nhật thông tin");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== QUẢN LÝ TÀI KHOẢN ==========

    /**
     * Xóa tài khoản (soft delete)
     */
    public JsonObject deleteAccount(int userId) {
        JsonObject response = new JsonObject();

        try {
            boolean success = userDAO.deleteUser(userId);

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Xóa tài khoản thành công");
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể xóa tài khoản");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Khôi phục tài khoản đã xóa
     */
//    public JsonObject restoreAccount(int userId) {
//        JsonObject response = new JsonObject();
//
//        try {
//            boolean success = userDAO.restoreUser(userId);
//
//            if (success) {
//                response.addProperty("status", "success");
//                response.addProperty("message", "Khôi phục tài khoản thành công");
//                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
//            } else {
//                response.addProperty("status", "error");
//                response.addProperty("message", "Không thể khôi phục tài khoản");
//            }
//
//        } catch (SQLException e) {
//            response.addProperty("status", "error");
//            response.addProperty("message", "Lỗi database: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return response;
//    }

    // ========== TRẬN ĐẤU & ELO ==========

    /**
     * Cập nhật kết quả trận đấu - Thắng
     */
    public JsonObject updateMatchWin(int userId, int eloChange) {
        JsonObject response = new JsonObject();

        try {
            boolean success = userDAO.updateMatchResultWin(userId, eloChange);

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Cập nhật kết quả thắng thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể cập nhật kết quả");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Cập nhật kết quả trận đấu - Thua
     */
    public JsonObject updateMatchLoss(int userId, int eloChange) {
        JsonObject response = new JsonObject();

        try {
            boolean success = userDAO.updateMatchResultLoss(userId, eloChange);

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Cập nhật kết quả thua thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể cập nhật kết quả");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Cập nhật ELO rating thủ công
     */
    public JsonObject updateEloRating(int userId, int newElo) {
        JsonObject response = new JsonObject();

        try {
            boolean success = userDAO.updateEloRating(userId, newElo);

            if (success) {
                response.addProperty("status", "success");
                response.addProperty("message", "Cập nhật ELO thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không thể cập nhật ELO");
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== LEADERBOARD & RANKING ==========

    /**
     * Lấy bảng xếp hạng
     */
    public JsonObject getLeaderboard(String sortBy, Integer limit) {
        JsonObject response = new JsonObject();

        try {
            List<user> leaderboard;

            if (limit != null && limit > 0) {
                leaderboard = userDAO.getTopUsersByElo(limit);
                sortBy = "elo";
            } else {
                switch (sortBy) {
                    case "elo":
                        leaderboard = userDAO.getUsersOrderByElo();
                        break;
                    case "wins":
                        leaderboard = userDAO.getUsersOrderByWins();
                        break;
                    case "recent":
                        leaderboard = userDAO.getUsersOrderByCreateTimeDesc();
                        break;
                    default:
                        leaderboard = userDAO.getUsersOrderByElo();
                        sortBy = "elo";
                }
            }

            response.addProperty("status", "success");
            response.addProperty("sortBy", sortBy);
            response.addProperty("count", leaderboard.size());
            response.add("data", gson.toJsonTree(leaderboard));

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi khi lấy bảng xếp hạng: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Tìm kiếm users
     */
    public JsonObject searchUsers(String keyword) {
        JsonObject response = new JsonObject();

        try {
            List<user> users = userDAO.searchUsersByName(keyword);

            response.addProperty("status", "success");
            response.addProperty("keyword", keyword);
            response.addProperty("count", users.size());
            response.add("data", gson.toJsonTree(users));

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi khi tìm kiếm: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Lấy users theo khoảng ELO
     */
    public JsonObject getUsersByEloRange(int minElo, int maxElo) {
        JsonObject response = new JsonObject();

        try {
            List<user> users = userDAO.getUsersByEloRange(minElo, maxElo);

            response.addProperty("status", "success");
            response.addProperty("minElo", minElo);
            response.addProperty("maxElo", maxElo);
            response.addProperty("count", users.size());
            response.add("data", gson.toJsonTree(users));

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi khi lấy users: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== THÔNG TIN USER ==========

    /**
     * Lấy thông tin user
     */
    public JsonObject getUserInfo(int userId) {
        JsonObject response = new JsonObject();

        try {
            user user = userDAO.getUser(userId);

            if (user != null) {
                response.addProperty("status", "success");
                response.add("data", gson.toJsonTree(user));
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không tìm thấy user");
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi khi lấy thông tin user: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Lấy thống kê user
     */
    public JsonObject getUserStatistics(int userId) {
        JsonObject response = new JsonObject();

        try {
            userDAO.UserStatistics stats = userDAO.getUserStatistics(userId);

            if (stats != null) {
                response.addProperty("status", "success");
                response.add("data", gson.toJsonTree(stats));
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Không tìm thấy user");
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi khi lấy thống kê: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Lấy danh sách tất cả users
     */
    public JsonObject getAllUsers() {
        JsonObject response = new JsonObject();

        try {
            List<user> users = userDAO.getAllUsers();

            response.addProperty("status", "success");
            response.addProperty("count", users.size());
            response.add("data", gson.toJsonTree(users));

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi khi lấy danh sách users: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Lấy users với phân trang
     */
    public JsonObject getUsersWithPagination(int page, int pageSize) {
        JsonObject response = new JsonObject();

        try {
            List<user> users = userDAO.getUsersWithPagination(page, pageSize);
            int totalUsers = userDAO.getTotalUserCount();
            int totalPages = (int) Math.ceil((double) totalUsers / pageSize);

            response.addProperty("status", "success");
            response.addProperty("page", page);
            response.addProperty("pageSize", pageSize);
            response.addProperty("totalUsers", totalUsers);
            response.addProperty("totalPages", totalPages);
            response.add("data", gson.toJsonTree(users));

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Lỗi khi lấy danh sách users: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== GOOGLE TOKEN VERIFICATION ==========

    private GoogleUserInfo verifyGoogleToken(String idTokenString) {
        try {
            // TODO: Implement real Google Token verification
            // Production code should use Google API Client Library

            JsonObject tokenPayload = gson.fromJson(idTokenString, JsonObject.class);

            GoogleUserInfo info = new GoogleUserInfo();
            info.name = tokenPayload.get("name").getAsString();
            info.email = tokenPayload.get("email").getAsString();
            info.googleId = tokenPayload.get("sub").getAsString();
            info.picture = tokenPayload.has("picture") ? tokenPayload.get("picture").getAsString() : null;

            return info;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class GoogleUserInfo {
        String googleId;
        String email;
        String name;
        String picture;
    }

    // ========== VALIDATION ==========

    private boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]{3,20}$");
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.length() >= 6;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ========== UTILITY METHODS ==========

    public JsonObject parseMessage(String message) {
        try {
            return gson.fromJson(message, JsonObject.class);
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("status", "error");
            error.addProperty("message", "Invalid JSON format");
            return error;
        }
    }

    public String toJson(Object object) {
        return gson.toJson(object);
    }

    public JsonObject createErrorResponse(String errorMessage) {
        JsonObject error = new JsonObject();
        error.addProperty("status", "error");
        error.addProperty("message", errorMessage);
        return error;
    }

    public JsonObject createSuccessResponse(String message) {
        JsonObject success = new JsonObject();
        success.addProperty("status", "success");
        success.addProperty("message", message);
        return success;
    }
}