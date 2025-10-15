package Model.Entity;

import java.time.LocalDateTime;

public class user {
    private int user_id;                 // PK: Mã định danh người dùng
    private String user_name;            // Tên hiển thị (có thể là tên người dùng)
    private String email;                // Email (Google hoặc đăng ký)
    private String password;             // Mật khẩu (null nếu đăng nhập bằng Google)
    private int elo_rating;              // Điểm Elo
    private int win_count;               // Số trận thắng
    private int loss_count;              // Số trận thua
    private LocalDateTime create_at;     // Ngày tạo tài khoản
    private String avatar_url;           // URL ảnh đại diện
    private String provider;             // "local" hoặc "google"
    private String provider_id;          // ID duy nhất của tài khoản Google
    private String status;               // "Online" hoặc "Offline"

    // ─────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────

    public user() {}

    // Dùng khi lấy dữ liệu từ DB (đầy đủ mọi trường)
    public user(int user_id, String user_name, String email, String password,
                int elo_rating, int win_count, int loss_count,
                LocalDateTime create_at, String avatar_url,
                String provider, String provider_id, String status) {
        this.user_id = user_id;
        this.user_name = user_name;
        this.email = email;
        this.password = password;
        this.elo_rating = elo_rating;
        this.win_count = win_count;
        this.loss_count = loss_count;
        this.create_at = create_at;
        this.avatar_url = avatar_url;
        this.provider = provider;
        this.provider_id = provider_id;
        this.status = status;
    }

    // Dùng khi tạo tài khoản mới (mặc định)
    public user(String user_name, String email, String password, String provider, String provider_id) {
        this.user_name = user_name;
        this.email = email;
        this.password = password;
        this.provider = provider;
        this.provider_id = provider_id;
        this.elo_rating = 1200;
        this.win_count = 0;
        this.loss_count = 0;
        this.create_at = LocalDateTime.now();
        this.avatar_url = "/images/default_avatar.png";
        this.status = "Offline";
    }

    // ─────────────────────────────────────────────────────────────
    // Getter & Setter
    // ─────────────────────────────────────────────────────────────

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public String getUser_name() { return user_name; }
    public void setUser_name(String user_name) { this.user_name = user_name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getElo_rating() { return elo_rating; }
    public void setElo_rating(int elo_rating) { this.elo_rating = elo_rating; }

    public int getWin_count() { return win_count; }
    public void setWin_count(int win_count) { this.win_count = win_count; }

    public int getLoss_count() { return loss_count; }
    public void setLoss_count(int loss_count) { this.loss_count = loss_count; }

    public LocalDateTime getCreate_at() { return create_at; }
    public void setCreate_at(LocalDateTime create_at) { this.create_at = create_at; }

    public String getAvatar_url() { return avatar_url; }
    public void setAvatar_url(String avatar_url) { this.avatar_url = avatar_url; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProvider_id() { return provider_id; }
    public void setProvider_id(String provider_id) { this.provider_id = provider_id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // ─────────────────────────────────────────────────────────────
    // toString
    // ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "User{" +
                "user_id=" + user_id +
                ", user_name='" + user_name + '\'' +
                ", email='" + email + '\'' +
                ", provider='" + provider + '\'' +
                ", elo_rating=" + elo_rating +
                ", win_count=" + win_count +
                ", loss_count=" + loss_count +
                ", create_at=" + create_at +
                ", status='" + status + '\'' +
                '}';
    }
}
