package com.database.server.DAO;

import com.database.server.Entity.user;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

public class userDAO {

    private final EntityManager em;

    public userDAO(EntityManager em) {
        this.em = em;
    }

    // ========================= ĐĂNG NHẬP / ĐĂNG KÝ =========================

    public user findByUsername(String username) {
        try {
            TypedQuery<user> query = em.createQuery(
                    "SELECT u FROM user u WHERE u.userName = :username",
                    user.class
            );
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

//    public user createUser(String username, String hashedPassword) {
//        user newUser = new user();
//        newUser.setUserName(username);
//        newUser.setPassword(hashedPassword);
//        newUser.setEloRating(1200);
//        newUser.setWinCount(0);
//        newUser.setLossCount(0);
//        newUser.setStatus("Offline");
//        newUser.setCreatedAt(LocalDateTime.now());
//        em.persist(newUser);
//        return newUser;
//    }
    public user createUser(String username, String hashedPassword) {

        user newUser = new user();
        newUser.setUserName(username);
        newUser.setPassword(hashedPassword);
        newUser.setEloRating(1200);
        newUser.setWinCount(0);
        newUser.setLossCount(0);
        newUser.setStatus("Offline");
        newUser.setCreatedAt(LocalDateTime.now());

        try {
            em.persist(newUser); // Lưu đối tượng mới vào DB
            return newUser; // Trả về user vừa được tạo
        } catch (Exception e) {
            // Nếu có lỗi (ví dụ: username vi phạm ràng buộc UNIQUE), rollback
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            // Ném lỗi để Service xử lý
            throw new RuntimeException("Không thể tạo user: " + e.getMessage(), e);
        }
    }

    public boolean isUsernameExists(String username) {
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM user u WHERE u.userName = :username", Long.class
                ).setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    public boolean isEmailExists(String email) {
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM user u WHERE u.email = :email", Long.class
                ).setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    // ========================= GOOGLE OAUTH =========================

    public user getUserByGoogleId(String googleId) {
        try {
            TypedQuery<user> query = em.createQuery(
                    "SELECT u FROM user u WHERE u.providerId = :providerId AND u.provider = 'google'",
                    user.class
            );
            query.setParameter("providerId", googleId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void insertUser(user u) {
        em.persist(u);
    }

    public user getUserByEmail(String email) {
        try {
            TypedQuery<user> query = em.createQuery(
                    "SELECT u FROM user u WHERE u.email = :email", user.class
            );
            query.setParameter("email", email);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public user createUserWithGoogle(String email, String googleId, String displayName, String avatarUrl) {
        // KIỂM TRA DỮ LIỆU ĐẦU VÀO
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email không được rỗng!");
        }
        if (googleId == null || googleId.isEmpty()) {
            throw new IllegalArgumentException("Google ID không được rỗng!");
        }
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("Tên hiển thị không được rỗng!");
        }

        // ✅ DÙNG DISPLAY NAME TỪ GOOGLE LÀM USERNAME
        // Loại bỏ ký tự đặc biệt, chỉ giữ chữ cái, số và khoảng trắng
        String username = displayName.trim();

        // Nếu username bị trùng, thêm số vào cuối
        String finalUsername = username;
        int counter = 1;
        while (isUsernameExists(finalUsername)) {
            finalUsername = username + counter++;
        }

        System.out.println("🔧 [DEBUG] Creating Google user with:");
        System.out.println("   Display Name (from Google): " + displayName);
        System.out.println("   Username (saved to DB): " + finalUsername);
        System.out.println("   Email: " + email);
        System.out.println("   Google ID: " + googleId);
        System.out.println("   Avatar URL: " + avatarUrl);

        // TẠO USER MỚI
        user newUser = new user();
        newUser.setUserName(finalUsername);      // ✅ Dùng name từ Google
        newUser.setEmail(email);                 // ✅ Lưu email vào trường email
        newUser.setProvider("google");
        newUser.setProviderId(googleId);         // ✅ Lưu Google ID vào provider_id
        newUser.setAvatarUrl(avatarUrl);
        newUser.setPassword("oauth_default");    // Không cần password thật
        newUser.setEloRating(1200);
        newUser.setWinCount(0);
        newUser.setLossCount(0);
        newUser.setStatus("Offline");
        newUser.setCreatedAt(LocalDateTime.now());

        // LƯU VÀO DATABASE
        em.persist(newUser);

        System.out.println("✅ [DEBUG] Google user created successfully!");
        System.out.println("   User ID: " + newUser.getUserId());
        System.out.println("   Username: " + newUser.getUserName());
        System.out.println("   Email: " + newUser.getEmail());

        return newUser;
    }

    public boolean linkGoogleAccount(int userId, String googleId, String email, String avatarUrl) {
        user u = em.find(user.class, userId);
        if (u == null) return false;
        u.setProvider("google");
        u.setProviderId(googleId);
        if (u.getEmail() == null || u.getEmail().isEmpty()) {
            u.setEmail(email);
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            u.setAvatarUrl(avatarUrl);
        }
        em.merge(u);
        return true;
    }

    public boolean unlinkGoogleAccount(int userId) {
        user u = em.find(user.class, userId);
        if (u == null) return false;
        u.setProvider("local");
        u.setProviderId(null);
        em.merge(u);
        return true;
    }

    private String generateUsernameFromEmail(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String username = base;
        int counter = 1;
        while (isUsernameExists(username)) {
            username = base + counter++;
        }
        return username;
    }

    public boolean hasPassword(int userId) {
        user u = em.find(user.class, userId);
        return u != null && u.getPassword() != null && !u.getPassword().isEmpty() && !"oauth_default".equals(u.getPassword());
    }

    // ========================= CRUD CƠ BẢN =========================

    public List<user> getAllUsers() {
        return em.createQuery("SELECT u FROM user u", user.class).getResultList();
    }

    public user getUserByUsername(String username) {
        try {
            TypedQuery<user> query = em.createQuery(
                    "SELECT u FROM user u WHERE u.userName = :username", user.class
            );
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public user getUserById(int id) {
        return em.find(user.class, id);
    }

    public boolean updateUser(user u) {
        em.merge(u);
        return true;
    }

    public boolean updateStatus(int userId, String status) {
        System.out.println("🔍 [DAO] updateStatus - User ID: " + userId + ", Status: " + status);

        try {
            user u = em.find(user.class, userId);

            if (u == null) {
                System.err.println("❌ [DAO] User not found: " + userId);
                return false;
            }

            System.out.println("✅ [DAO] Found user: " + u.getUserName() + " (current: " + u.getStatus() + ")");

            u.setStatus(status);
            em.merge(u);

            System.out.println("✅ [DAO] Updated to: " + status);
            return true;

        } catch (Exception e) {
            System.err.println("❌ [DAO] Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean updateEloRating(int userId, int newElo) {
        user u = em.find(user.class, userId);
        if (u != null) {
            u.setEloRating(newElo);
            em.merge(u);
            return true;
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        user u = em.find(user.class, userId);
        if (u != null) {
            em.remove(u);
            return true;
        }
        return false;
    }

    // ========================= TRUY VẤN & THỐNG KÊ =========================

    public List<user> getUsersOrderByElo() {
        return em.createQuery("SELECT u FROM user u ORDER BY u.eloRating DESC", user.class).getResultList();
    }

    public List<user> getTopUsersByElo(int limit) {
        return em.createQuery("SELECT u FROM user u ORDER BY u.eloRating DESC", user.class)
                .setMaxResults(limit)
                .getResultList();
    }

    public int getTotalUserCount() {
        return em.createQuery("SELECT COUNT(u) FROM user u", Long.class)
                .getSingleResult().intValue();
    }

    public List<user> searchUsers(String keyword) {
        return em.createQuery("SELECT u FROM user u WHERE LOWER(u.userName) LIKE LOWER(:keyword)", user.class)
        .setParameter("keyword", "%" + keyword + "%")
        .setMaxResults(20)
        .getResultList();
    }
    // ========================= THỐNG KÊ NGƯỜI DÙNG =========================

    public UserStatistics getUserStatistics(int userId) {
        user u = getUserById(userId);
        if (u == null) return null;
        int total = u.getWinCount() + u.getLossCount();
        double rate = total > 0 ? (double) u.getWinCount() / total * 100 : 0;
        return new UserStatistics(
                u.getUserId(), u.getUserName(), u.getEloRating(),
                u.getWinCount(), u.getLossCount(), total, rate
        );
    }

    public static class UserStatistics {
        public int userId;
        public String username;
        public int eloRating;
        public int wins;
        public int losses;
        public int totalMatches;
        public double winRate;

        public UserStatistics(int userId, String username, int eloRating,
                              int wins, int losses, int totalMatches, double winRate) {
            this.userId = userId;
            this.username = username;
            this.eloRating = eloRating;
            this.wins = wins;
            this.losses = losses;
            this.totalMatches = totalMatches;
            this.winRate = winRate;
        }
    }
}
