package com.database.server.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chat_room")
public class chat_room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_id")
    private int chatroom_id;

    @Column(name = "room_type", length = 20)
    private String room_type;

    @Column(name = "ref_id")
    private int ref_id;

    @Column(name = "created_at")
    private LocalDateTime created_at = LocalDateTime.now();

    // Quan hệ 1-n với chat_message
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true,  fetch = FetchType.EAGER)
    private List<chat_message> messages;

    // Quan hệ 1-n với chat_participant
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<chat_participant> participants;

    public chat_room() {}

    public chat_room(int chatroom_id, String room_type, LocalDateTime created_at) {
        this.chatroom_id = chatroom_id;
        this.room_type = room_type;
        this.created_at = created_at;
    }

    public chat_room(int chatroom_id, String room_type, int ref_id, LocalDateTime created_at) {
        this.chatroom_id = chatroom_id;
        this.room_type = room_type;
        this.ref_id = ref_id;
        this.created_at = created_at;
    }

    // getter - setter
    public int getChatroom_id() { return chatroom_id; }
    public void setChatroom_id(int chatroom_id) { this.chatroom_id = chatroom_id; }

    public String getRoom_type() { return room_type; }
    public void setRoom_type(String room_type) { this.room_type = room_type; }

    public int getRef_id() { return ref_id; }
    public void setRef_id(int ref_id) { this.ref_id = ref_id; }

    public LocalDateTime getCreated_at() { return created_at; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }

    public List<chat_message> getMessages() { return messages; }
    public void setMessages(List<chat_message> messages) { this.messages = messages; }

    public List<chat_participant> getParticipants() { return participants; }
    public void setParticipants(List<chat_participant> participants) { this.participants = participants; }

    @Override
    public String toString() {
        return "chat_room{" +
                "chatroom_id=" + chatroom_id +
                ", room_type=" + room_type +
                ", created_at=" + created_at +
                '}';
    }
}
