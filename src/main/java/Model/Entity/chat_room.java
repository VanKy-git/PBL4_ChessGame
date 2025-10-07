package Model.Entity;

import java.time.LocalDateTime;
public class chat_room {
    private int chatroom_id;
    private String room_type;
    private int  ref_id;
    private LocalDateTime created_at;

    public chat_room () {}

    public chat_room (int chatroom_id, String room_type, LocalDateTime created_at) {
        this.chatroom_id = chatroom_id;
        this.room_type = room_type;
        this.created_at = created_at;
    }

    public chat_room (int chatroom_id, String room_type, int ref_id, LocalDateTime created_at) {
        this.chatroom_id = chatroom_id;
        this.room_type = room_type;
        this.ref_id = ref_id;
        this.created_at = created_at;
    }

    // getter - setter
    public int getChatroom_id() { return chatroom_id; }
    public String getRoom_type() { return room_type; }
    public int getRef_id() { return ref_id; }
    public LocalDateTime getCreated_at() { return created_at; }

    public void setChatroom_id(int chatroom_id) { this.chatroom_id = chatroom_id; }
    public void setRoom_type(String room_type) { this.room_type = room_type; }
    public void setRef_id(int ref_id) { this.ref_id = ref_id; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }

    @Override
    public String toString() {
        return "chat_room{" +
                "chatroom_id=" + chatroom_id  +
                ", room_type=" + room_type +
                ", created_at=" + created_at +
                '}';
    }

}
