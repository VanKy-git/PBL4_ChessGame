package com.database.server.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_participant")
@IdClass(ChatParticipantKey.class)
public class chat_participant {

    @Id
    @Column(name = "chatroom_id")
    private int chatroom_id;

    @Id
    @Column(name = "user_id")
    private int user_id;

    @ManyToOne
    @JoinColumn(name = "chatroom_id", insertable = false, updatable = false)
    private chat_room chatRoom;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private user user;

    @Column(name = "joined_at")
    private LocalDateTime joined_at = LocalDateTime.now();

    public chat_participant() {}

    public chat_participant(int chatroom_id, int user_id, LocalDateTime joined_at) {
        this.chatroom_id = chatroom_id;
        this.user_id = user_id;
        this.joined_at = joined_at;
    }

    // getter - setter
    public int getChatroom_id() { return chatroom_id; }
    public void setChatroom_id(int chatroom_id) { this.chatroom_id = chatroom_id; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public LocalDateTime getJoined_at() { return joined_at; }
    public void setJoined_at(LocalDateTime joined_at) { this.joined_at = joined_at; }

    public chat_room getChatRoom() { return chatRoom; }
    public void setChatRoom(chat_room chatRoom) { this.chatRoom = chatRoom; }

    public user getUser() { return user; }
    public void setUser(user user) { this.user = user; }

    @Override
    public String toString() {
        return "chat_participant{" +
                "chatroom_id=" + chatroom_id +
                ", user_id=" + user_id +
                ", joined_at=" + joined_at +
                '}';
    }
}
