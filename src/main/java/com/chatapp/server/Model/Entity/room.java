package com.chatapp.server.Model.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Bảng room thể hiện một phòng chat (1-1 hoặc tạo bởi người dùng).
 * - Mỗi phòng có 1 host (người tạo) và 1 guest (người tham gia).
 * - Một phòng có thể chứa nhiều tin nhắn (quan hệ 1-N với messages).
 */

@Entity
@Table (name = "room")
public class room {

    /** ID phòng (PRIMARY KEY) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "room_id")
    private int room_id;

    /** ID người tạo phòng (FK → users.user_id) */
    @Column(name = "host_id", nullable = false)
    private int host_id;

    /** ID người tham gia phòng (FK → users.user_id), có thể bằng null nếu chưa có ai tham gia */
    @Column (name = "guest_id", nullable = true)
    private Integer guest_id;

    /** Trạng thái phòng: "Waiting", "Active", "Closed" */
    @Column (name = "room_status",  length = 50, nullable = true)
    private String room_status;

    /** Thời điểm tạo phòng */
    @Column (name = "create_at", nullable = false)
    private LocalDateTime create_at = LocalDateTime.now();

    // ===== CONSTRUCTORS =====

    /** Constructor mặc định */
    public room() {}

    /**
     * Tạo phòng mới với host, mặc định trạng thái "Waiting"
     */
    public room(int room_id, int host_id) {
        this.room_id = room_id;
        this.host_id = host_id;
        this.guest_id = 0;                // Chưa có người tham gia
        this.room_status = "waiting";
        this.create_at = LocalDateTime.now();
    }

    /**
     * Constructor đầy đủ (khi đã có dữ liệu từ DB)
     */
    public room(int room_id, int host_id, int guest_id, String room_status, LocalDateTime create_at) {
        this.room_id = room_id;
        this.host_id = host_id;
        this.guest_id = guest_id;
        this.room_status = room_status;
        this.create_at = create_at;
    }

    // ===== GETTER - SETTER =====

    public int getRoom_id() { return room_id; }
    public int getHost_id() { return host_id; }
    public Integer getGuest_id() { return guest_id; }
    public String getRoom_status() { return room_status; }
    public LocalDateTime getCreate_at() { return create_at; }

    public void setRoom_id(int room_id) { this.room_id = room_id; }
    public void setHost_id(int host_id) { this.host_id = host_id; }
    public void setGuest_id(Integer guest_id) { this.guest_id = guest_id; }
    public void setRoom_status(String room_status) { this.room_status = room_status; }
    public void setCreate_at(LocalDateTime create_at) { this.create_at = create_at; }

    // ===== RELATIONSHIPS =====
    // (Chỉ chú thích, không triển khai đối tượng phức tạp để giữ code đơn giản)
    // 1. host_id → users.user_id (1 người dùng có thể tạo nhiều phòng)
    // 2. guest_id → users.user_id (1 người dùng có thể tham gia nhiều phòng)
    // 3. room_id → messages.room_id (1 phòng có thể có nhiều tin nhắn)

    // ===== TO STRING =====
    @Override
    public String toString() {
        return "Room{" +
                "room_id=" + room_id +
                ", host_id=" + host_id +
                ", guest_id=" + guest_id +
                ", room_status='" + room_status + '\'' +
                ", create_at=" + create_at +
                '}';
    }
}
