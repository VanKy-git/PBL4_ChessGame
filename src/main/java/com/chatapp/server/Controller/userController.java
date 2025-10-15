package com.chatapp.server.Controller;

import Model.DAO.userDAO;
import Model.Entity.user;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

// Import Google OAuth Libraries
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;


public class userController {

    private userDAO userDAO;
    private Gson gson;
    private static final String GOOGLE_CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID_HERE.apps.googleusercontent.com"; // Nhận ID token gg trả về chưa thông tin

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
    }

    // ========== ĐĂNG NHẬP/ĐĂNG KÝ THÔNG THƯỜNG ==========

    //Đăng nhập username - password
    public JsonObject handleLogin(String username, String password) {
        JsonObject response = new JsonObject();

        try {
            user user = userDAO.login(username, password);

            if (user != null && user.getStatus() != "Online") {
                response.addProperty("message", "Đăng nhập thành công");
                response.addProperty("loginMethod", "normal");
                response.add("user", gson.toJsonTree(user));

                // Cập nhật status sang online nếu cần
                userDAO.updateStatus(user.getUser_id(), "Online");
            } else if (user != null && user.getStatus() == "Online") {
                response.addProperty("message", "Lỗi!!! Tài khoản đã được đăng nhập trên thiết bị khác!");
                }
                else {
                    response.addProperty("message", "Sai tên đăng nhập hoặc mật khẩu");
                }
        } catch (SQLException e) {
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    //Đăng kí tài khoản với username - password
    public JsonObject handleRegister(String username, String password, String email) {
        JsonObject response = new JsonObject();

        try {
            // Kiểm tra username đã tồn tại
            if (userDAO.isUsernameExists(username)) {
                response.addProperty("message", "Tên đăng nhập đã tồn tại");
                return response;
            }

            // Validate
            if (!isValidUsername(username)) {
                response.addProperty("message", "Tên đăng nhập không hợp lệ (3-20 ký tự, chỉ chữ và số)");
                return response;
            }

            if (!isValidPassword(password)) {
                response.addProperty("message", "Mật khẩu không hợp lệ (tối thiểu 6 ký tự)");
                return response;
            }

            // Tạo tài khoản
            user newUser = userDAO.createUser(username, password);

            if (newUser != null) {
                response.addProperty("message", "Đăng ký thành công");
                response.add("user", gson.toJsonTree(newUser));
            } else {
                response.addProperty("message", "Không thể tạo tài khoản");
            }

        } catch (SQLException e) {
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    //Đăng xuất
    public JsonObject handleLogout(int userId) {
        JsonObject response = new JsonObject();

        try {
            boolean success = userDAO.updateStatus(userId, "Offline");

            if (success) {
                response.addProperty("message", "Đăng xuất thành công");
            } else {
                response.addProperty("message", "Không thể đăng xuất");
            }
        } catch (SQLException e) {
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== GOOGLE OAUTH ==========

    //Đăng nhập Google Oauth
    public JsonObject handleGoogleLogin(String googleToken) {
        JsonObject response = new JsonObject();

        try {
            GoogleUserInfo googleInfo = verifyGoogleToken(googleToken);

            if (googleInfo == null) {
                response.addProperty("message", "Token Google không hợp lệ");
                return response;
            }

            user existingUser = userDAO.getUserByGoogleId(googleInfo.googleId);

            if (existingUser != null) {
                response.addProperty("message", "Đăng nhập Google thành công");
                response.addProperty("loginMethod", "google");
                response.add("user", gson.toJsonTree(existingUser));
                userDAO.updateStatus(existingUser.getUser_id(), "Online");
                userDAO.updateProvider(existingUser.getUser_id(), "google account");
            } else {
                user newUser = userDAO.createUserWithGoogle(
                        googleInfo.name,
                        googleInfo.email,
                        googleInfo.googleId,
                        googleInfo.picture
                );

                if (newUser != null) {
                    response.addProperty("message", "Đăng ký Google thành công");
                    response.addProperty("loginMethod", "google");
                    response.addProperty("isNewUser", true);
                    response.add("user", gson.toJsonTree(newUser));
                } else {
                    response.addProperty("message", "Không thể tạo tài khoản từ Google");
                }
            }

        } catch (SQLException e) {
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
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
                response.addProperty("message", "Token Google không hợp lệ");
                return response;
            }

            user existingUser = userDAO.getUserByGoogleId(googleInfo.googleId);
            if (existingUser != null && existingUser.getUser_id() != userId) {
                response.addProperty("message", "Tài khoản Google đã được liên kết với tài khoản khác");
                return response;
            }

            boolean success = userDAO.linkGoogleAccount(userId, googleInfo.googleId);

            if (success) {
                response.addProperty("message", "Liên kết tài khoản Google thành công");
            } else {
                response.addProperty("message", "Không thể liên kết tài khoản Google");
            }

        } catch (SQLException e) {
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
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
                response.addProperty("message", "Không tìm thấy người dùng");
                return response;
            }

            if (!userDAO.hasPassword(userId)) {
                response.addProperty("message", "Không thể hủy liên kết. Vui lòng đặt mật khẩu trước");
                return response;
            }

            boolean success = userDAO.unlinkGoogleAccount(userId);

            if (success) {
                response.addProperty("message", "Hủy liên kết tài khoản Google thành công");
            } else {
                response.addProperty("message", "Không thể hủy liên kết tài khoản Google");
            }

        } catch (SQLException e) {
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== CẬP NHẬT THÔNG TIN USER ==========

    /**
     * Đổi mật khẩu
     */
    public JsonObject changePassword(int userId, String oldPassword, String newPassword) {
        JsonObject response = new JsonObject();

        try {
            user user = userDAO.getUser(userId);
            if (user == null) {
                response.addProperty("message", "Không tìm thấy người dùng");
                return response;
            }

            // Kiểm tra mật khẩu cũ
            if (user.getPassword() != null && !user.getPassword().equals(oldPassword)) {
                response.addProperty("message", "Mật khẩu cũ không đúng");
                return response;
            }

            if (!isValidPassword(newPassword)) {
                response.addProperty("message", "Mật khẩu mới không hợp lệ (tối thiểu 6 ký tự)");
                return response;
            }

            boolean success = userDAO.updatePassword(userId, newPassword);

            if (success) {
                response.addProperty("message", "Đổi mật khẩu thành công");
            } else {
                response.addProperty("message", "Không thể đổi mật khẩu");
            }

        } catch (SQLException e) {
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
                response.addProperty("message", "Cập nhật avatar thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
            } else {
                response.addProperty("message", "Không thể cập nhật avatar");
            }

        } catch (SQLException e) {
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
                response.addProperty("message", "Cập nhật thông tin thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(updatedUser.getUser_id())));
            } else {
                response.addProperty("message", "Không thể cập nhật thông tin");
            }

        } catch (SQLException e) {
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
                response.addProperty("message", "Xóa tài khoản thành công");
            } else {
                response.addProperty("message", "Không thể xóa tài khoản");
            }

        } catch (SQLException e) {
            response.addProperty("message", "Lỗi database: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== TRẬN ĐẤU & ELO ==========

    /**
     * Cập nhật kết quả trận đấu - Thắng
     */
    public JsonObject updateMatchWin(int userId, int eloChange) {
        JsonObject response = new JsonObject();

        try {
            boolean success = userDAO.updateMatchResultWin(userId, eloChange);

            if (success) {
                response.addProperty("message", "Cập nhật kết quả thắng thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
            } else {
                response.addProperty("message", "Không thể cập nhật kết quả");
            }

        } catch (SQLException e) {
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
                response.addProperty("message", "Cập nhật kết quả thua thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
            } else {
                response.addProperty("message", "Không thể cập nhật kết quả");
            }

        } catch (SQLException e) {
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
                response.addProperty("message", "Cập nhật ELO thành công");
                response.add("user", gson.toJsonTree(userDAO.getUser(userId)));
            } else {
                response.addProperty("message", "Không thể cập nhật ELO");
            }

        } catch (SQLException e) {
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

            response.addProperty("sortBy", sortBy);
            response.addProperty("count", leaderboard.size());
            response.add("data", gson.toJsonTree(leaderboard));

        } catch (SQLException e) {
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

            response.addProperty("keyword", keyword);
            response.addProperty("count", users.size());
            response.add("data", gson.toJsonTree(users));

        } catch (SQLException e) {
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

            response.addProperty("minElo", minElo);
            response.addProperty("maxElo", maxElo);
            response.addProperty("count", users.size());
            response.add("data", gson.toJsonTree(users));

        } catch (SQLException e) {
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
                response.add("data", gson.toJsonTree(user));
            } else {
                response.addProperty("message", "Không tìm thấy user");
            }
        } catch (SQLException e) {
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
                response.add("data", gson.toJsonTree(stats));
            } else {
                response.addProperty("message", "Không tìm thấy user");
            }
        } catch (SQLException e) {
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

            response.addProperty("count", users.size());
            response.add("data", gson.toJsonTree(users));

        } catch (SQLException e) {
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

            response.addProperty("page", page);
            response.addProperty("pageSize", pageSize);
            response.addProperty("totalUsers", totalUsers);
            response.addProperty("totalPages", totalPages);
            response.add("data", gson.toJsonTree(users));

        } catch (SQLException e) {
            response.addProperty("message", "Lỗi khi lấy danh sách users: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    // ========== GOOGLE TOKEN VERIFICATION ==========

    /**
     * Xác thực Google ID Token và trả về thông tin user
     * Phương thức này kết nối thực tế với Google để verify token
     */
    private GoogleUserInfo verifyGoogleToken(String idTokenString) {
        try {
            // Khởi tạo Google ID Token Verifier
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                    .setIssuer("https://accounts.google.com")
                    .build();

            // Verify token - Bước này kết nối với Google
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken != null) {
                Payload payload = idToken.getPayload();

                // Kiểm tra email đã verified chưa
                boolean emailVerified = payload.getEmailVerified();
                if (!emailVerified) {
                    System.err.println("Email chưa được xác thực bởi Google");
                    return null;
                }

                // Tạo GoogleUserInfo object
                GoogleUserInfo info = new GoogleUserInfo();
                info.googleId = payload.getSubject();
                info.email = payload.getEmail();
                info.name = (String) payload.get("name");
                info.picture = (String) payload.get("picture");
                info.emailVerified = emailVerified;

                return info;
            } else {
                System.err.println("Token Google không hợp lệ hoặc đã hết hạn");
                return null;
            }

        } catch (GeneralSecurityException e) {
            System.err.println("Lỗi bảo mật khi verify Google token: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối với Google servers: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi verify token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Class chứa thông tin user từ Google
     */
    private static class GoogleUserInfo {
        String googleId;
        String email;
        String name;
        String picture;
        boolean emailVerified;
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
            error.addProperty("message", "Invalid JSON format");
            return error;
        }
    }

    public String toJson(Object object) {
        return gson.toJson(object);
    }

    public JsonObject createErrorResponse(String errorMessage) {
        JsonObject error = new JsonObject();
        error.addProperty("message", errorMessage);
        return error;
    }

    public JsonObject createSuccessResponse(String message) {
        JsonObject success = new JsonObject();
        success.addProperty("message", message);
        return success;
    }
}