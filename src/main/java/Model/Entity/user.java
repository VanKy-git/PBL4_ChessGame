package Model.Entity;

import java.time.LocalDateTime;

public class user {
    private int user_id;
    private String user_name;
    private String password;
    private int elo_rating;
    private int win_count;
    private int loss_count;
    private LocalDateTime create_at;
    private String avatar_url;
    private String email;
    private String status;
    private String auth_provider;
    private String full_name;

    public user () {}

    // Constructor cũ, có thể vẫn hữu ích ở đâu đó
    public user(int user_id, String user_name, String password, int elo_rating, int win_count, int loss_count, LocalDateTime create_at) {
        this.user_id = user_id;
        this.user_name = user_name;
        this.password = password;
        this.elo_rating = elo_rating;
        this.win_count = win_count;
        this.loss_count = loss_count;
        this.create_at = create_at;
    }

    // CONSTRUCTOR ĐẦY ĐỦ MỚI - Dùng cho userDAO
    public user(int user_id, String user_name, String password, int elo_rating, int win_count, int loss_count, LocalDateTime create_at, String avatar_url, String email, String status, String auth_provider, String full_name) {
        this.user_id = user_id;
        this.user_name = user_name;
        this.password = password;
        this.elo_rating = elo_rating;
        this.win_count = win_count;
        this.loss_count = loss_count;
        this.create_at = create_at;
        this.avatar_url = avatar_url;
        this.email = email;
        this.status = status;
        this.auth_provider = auth_provider;
        this.full_name = full_name;
    }

    // --- Getters ---
    public int getUser_id() { return user_id; }
    public String getUser_name() { return user_name; }
    public String getPassword() { return password; }
    public int getElo_rating() { return elo_rating; }
    public int getWin_count() { return win_count; }
    public int getLoss_count() { return loss_count; } // Sửa tên phương thức cho đúng
    public LocalDateTime getCreate_at() { return create_at; }
    public String getAvatar_url() { return avatar_url; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public String getAuth_provider() { return auth_provider; }
    public String getFull_name() { return full_name; }


    // --- Setters ---
    public void setUser_id(int user_id) { this.user_id = user_id; }
    public void setUser_name(String user_name) { this.user_name = user_name; }
    public void setPassword(String password) { this.password = password; }
    public void setElo_rating(int elo_rating) { this.elo_rating = elo_rating; }
    public void setWin_count(int win_count) { this.win_count = win_count; }
    public void setLoss_count(int loss_count) { this.loss_count = loss_count; } // Sửa tên phương thức cho đúng
    public void setCreate_at(LocalDateTime create_at) { this.create_at = create_at; }
    public void setAvatar_url(String avatar_url) { this.avatar_url = avatar_url; }
    public void setEmail(String email) { this.email = email; }
    public void setStatus(String status) { this.status = status; }
    public void setAuth_provider(String auth_provider) { this.auth_provider = auth_provider; }
    public void setFull_name(String full_name) { this.full_name = full_name; }


    @Override
    public String toString() {
        return "user{" +
                "user_id=" + user_id +
                ", user_name='" + user_name + '\'' +
                ", elo_rating=" + elo_rating +
                ", win_count=" + win_count +
                ", loss_count=" + loss_count +
                ", create_at=" + create_at +
                ", full_name='" + full_name + '\'' +
                '}';
    }
}
