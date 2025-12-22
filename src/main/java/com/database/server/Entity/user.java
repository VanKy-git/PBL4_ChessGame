package com.database.server.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity đại diện cho bảng 'users' trong cơ sở dữ liệu PostgreSQL.
 * Lưu thông tin người dùng trong hệ thống chat/game.
 */
@Entity
@Table(name = "users")
public class user {

    // Khóa chính, tự tăng
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int user_id;

    // Tên đăng nhập duy nhất của người dùng
    @Column(name = "user_name", length = 50, unique = true, nullable = false)
    private String userName;

    // Mật khẩu đã mã hóa
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    // Điểm Elo của người dùng, mặc định 1200
    @Column(name = "elo_rating", nullable = false)
    private int eloRating = 1200;

    // Số trận thắng
    @Column(name = "win_count", nullable = false)
    private int winCount = 0;

    // Số trận thua
    @Column(name = "loss_count", nullable = false)
    private int lossCount = 0;

    // Ngày giờ tạo tài khoản
    @Column(name = "create_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Ảnh đại diện (đường dẫn ảnh)
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    // ID nhà cung cấp (Google, Facebook, v.v.)
    @Column(name = "provider_id", length = 255)
    private String providerId;

    // Trạng thái người dùng: online / offline / in_match
    @Column(name = "status", length = 20)
    private String status = "Offline";

    // Loại tài khoản: local / google / facebook
    @Column(name = "provider", length = 50)
    private String provider = "local";

    // Email đăng ký
    @Column(name = "email", length = 255)
    private String email;

    // ===== Constructors =====
    public user() {}

    public user(String username, String password, String email) {
        this.userName = username;
        this.password = password;
        this.email = email;
    }

    public user(String email, String googleId, String username, String avatarUrl) {
        this.email = email;
        this.providerId = googleId;
        this.userName = username;
        this.avatarUrl = avatarUrl;
        this.provider = "google";
        this.createdAt = LocalDateTime.now();
    }


    // ===== Getters & Setters =====
    public int getUserId() { return user_id; }
    public void setUserId(int id) { this.user_id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String username) { this.userName = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getEloRating() { return eloRating; }
    public void setEloRating(int eloRating) { this.eloRating = eloRating; }

    public int getWinCount() { return winCount; }
    public void setWinCount(int winCount) { this.winCount = winCount; }

    public int getLossCount() { return lossCount; }
    public void setLossCount(int lossCount) { this.lossCount = lossCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // ===== toString() =====
    @Override
    public String toString() {
        return "User{" +
                "id=" + user_id +
                ", username='" + userName + '\'' +
                ", eloRating=" + eloRating +
                ", winCount=" + winCount +
                ", lossCount=" + lossCount +
                ", status='" + status + '\'' +
                ", provider='" + provider + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
