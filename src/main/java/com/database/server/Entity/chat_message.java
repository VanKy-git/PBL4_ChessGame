package com.database.server.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
public class chat_message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private int message_id;

    @ManyToOne
    @JoinColumn(name = "chatroom_id", nullable = false)
    private chat_room chatRoom;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private user user;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "sent_at")
    private LocalDateTime send_at = LocalDateTime.now();

    public chat_message() {}

    public chat_message(int message_id, int chatroom_id, int user_id, String message) {
        this.message_id = message_id;
        this.chatRoom = new chat_room();
        this.chatRoom.setChatroom_id(chatroom_id);
        this.user = new user();
        this.user.setUserId(user_id);
        this.message = message;
        this.send_at = LocalDateTime.now();
    }

    public chat_message(int message_id, int chatroom_id, int user_id, String message, LocalDateTime send_at) {
        this.message_id = message_id;
        this.chatRoom = new chat_room();
        this.chatRoom.setChatroom_id(chatroom_id);
        this.user = new user();
        this.user.setUserId(user_id);
        this.message = message;
        this.send_at = send_at;
    }

    // getter - setter
    public int getMessage_id() { return message_id; }
    public void setMessage(int message_id) { this.message_id = message_id; }

    public chat_room getChatRoom() { return chatRoom; }
    public void setChatRoom(chat_room chatRoom) { this.chatRoom = chatRoom; }

    public user getUser() { return user; }
    public void setUser(user user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getSend_at() { return send_at; }
    public void setSend_at(LocalDateTime send_at) { this.send_at = send_at; }

    @Override
    public String toString() {
        return "chat_message [message_id=" + message_id +
                ", chatroom_id=" + (chatRoom != null ? chatRoom.getChatroom_id() : null) +
                ", user_id=" + (user != null ? user.getUserId() : null) +
                ", message=" + message +
                ", send_at=" + send_at +
                "]";
    }
}
