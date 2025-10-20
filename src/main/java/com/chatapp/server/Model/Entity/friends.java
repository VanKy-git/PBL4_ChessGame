package com.chatapp.server.Model.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "friends")
public class friends {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int friendshipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id1", nullable = false)
    private user user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id2", nullable = false)
    private user user2;

    @Column(nullable = false)
    private String status;

    private LocalDateTime createdAt;

    // ===== GETTER & SETTER =====
    public int getFriendshipId() { return friendshipId; }
    public void setFriendshipId(int friendshipId) { this.friendshipId = friendshipId; }

    public user getUser1() { return user1; }
    public void setUser1(user user1) { this.user1 = user1; }

    public user getUser2() { return user2; }
    public void setUser2(user user2) { this.user2 = user2; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
