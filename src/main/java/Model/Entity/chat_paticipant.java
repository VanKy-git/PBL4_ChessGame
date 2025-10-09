package Model.Entity;

import java.time.LocalDateTime;

public class chat_paticipant {
    private int chatroom_id;
    private int user_id;
    private LocalDateTime joined_at;

    public chat_paticipant (int chatroom_id, int user_id, LocalDateTime joined_at) {
        this.chatroom_id = chatroom_id;
        this.user_id = user_id;
        this.joined_at = joined_at;
    }

    // getter - setter
    public int getChatroom_id() {return chatroom_id;}
    public int getUser_id() {return user_id;}
    public LocalDateTime getJoined_at() {return joined_at;}

    public void setChatroom_id(int chatroom_id) {this.chatroom_id = chatroom_id;}
    public void setUser_id(int user_id) {this.user_id = user_id;}
    public void setJoined_at(LocalDateTime joined_at) {this.joined_at = joined_at;}

    @Override
    public String toString() {
        return "chat_paticipant{" +
                "chatroom_id=" + chatroom_id +
                ", user_id="  + user_id +
                ", joined_at=" + joined_at +
                '}';
    }
}
