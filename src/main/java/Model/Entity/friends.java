package Model.Entity;

import java.time.LocalDateTime;
public class friends {
    private int friendship_id;
    private int user1_id;
    private int user2_id;
    private String status;
    private LocalDateTime created_at;

    public friends() {}

    public friends(int friendship_id, int user1_id, int user2_id, String status, LocalDateTime created_at) {
        this.friendship_id = friendship_id;
        this.user1_id = user1_id;
        this.user2_id = user2_id;
        this.status = status;
        this.created_at = created_at;
    }

    public friends(int friendship_id, int user1_id, int user2_id) {
        this.friendship_id = friendship_id;
        this.user1_id = user1_id;
        this.user2_id = user2_id;
        this.status = "PENDING";
        this.created_at = LocalDateTime.now();
    }

    // getter - setter
    public int getFriendship_id() { return friendship_id; }
    public int getUser1_id() { return user1_id; }
    public int getUser2_id() { return user2_id; }
    public String getStatus() { return status; }
    public LocalDateTime getCreated_at() { return created_at; }

    public void setFriendship_id(int friendship_id) { this.friendship_id = friendship_id; }
    public void setUser1_id(int user1_id) { this.user1_id = user1_id; }
    public void setUser2_id(int user2_id) { this.user2_id = user2_id; }
    public void setStatus(String status) { this.status = status; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }

    @Override
    public String toString() {
        return "friends{" +
                "friendship_id=" + friendship_id  +
                ", user1_id=" + user1_id +
                ", user2_id=" + user2_id   +
                ", status=" + status +
                ", created_at=" + created_at +
                '}';
    }
}
