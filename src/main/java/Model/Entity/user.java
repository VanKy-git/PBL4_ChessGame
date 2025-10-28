package Model.Entity;

import java.time.LocalDateTime;

public class user {
    private int user_id;                // Mã định danh duy nhất của người dùng (Primary Key trong DB)
    private String user_name;           // Tên đăng nhập hoặc nickname hiển thị
    private String password;            // Mật khẩu người dùng (nên được mã hóa/hash thay vì plaintext)
    private int elo_rating;             // Hệ số Elo (điểm đánh giá sức mạnh cờ vua)
    private int win_count;              // Số trận thắng
    private int loss_count;             // Số trận thua
    private LocalDateTime create_at;    // Thời điểm tạo tài khoản
    public user () {}

    public user(int user_id, String user_name, String password) {
        this.user_id = user_id;
        this.user_name = user_name;
        this.password = password;
        this.elo_rating = 1200;
        this.win_count = 0;
        this.loss_count = 0;
        this.create_at = LocalDateTime.now();
    }

    public user(int user_id, String user_name, String password, int elo_rating, int win_count, int loss_count, LocalDateTime create_at) {
        this.user_id = user_id;
        this.user_name = user_name;
        this.password = password;
        this.elo_rating = elo_rating;
        this.win_count = win_count;
        this.loss_count = loss_count;
        this.create_at = create_at;
    }
    //getter - setter
    public int getUser_id() { return user_id; }
    public String getUser_name() { return user_name; }
    public String getPassword() { return password; }
    public int getElo_rating() { return elo_rating; }
    public int getWin_count() { return win_count; }
    public int getLoose_count() { return loss_count; }
    public LocalDateTime getCreate_at() { return create_at; }

    public void setUser_id(int user_id) { this.user_id = user_id; }
    public void setUser_name(String user_name) { this.user_name = user_name; }
    public void setPassword(String password) { this.password = password; }
    public void setElo_rating(int elo_rating) { this.elo_rating = elo_rating; }
    public void setWin_count(int win_count) { this.win_count = win_count; }
    public void setLoose_count(int loose_count) { this.loss_count = loose_count; }
    public void setCreate_at(LocalDateTime create_at) { this.create_at = create_at; }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + user_id +
                ", userName='" + user_name + '\'' +
                ", eloRating=" + elo_rating +
                ", winCount=" + win_count +
                ", loseCount=" + loss_count +
                ", createAt=" + create_at +
                '}';
    }
}