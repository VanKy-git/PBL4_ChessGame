package Model.Entity;

import java.time.LocalDateTime;
public class chat_message {
    private int message_id;
    private int chatroom_id;
    private int user_id;
    private String message;
    private LocalDateTime send_at;

    public chat_message() {}

    public chat_message(int message_id, int chatroom_id, int user_id, String message) {
        this.message_id = message_id;
        this.chatroom_id = chatroom_id;
        this.user_id = user_id;
        this.message = message;
        this.send_at = LocalDateTime.now();
    }

    public chat_message(int message_id, int chatroom_id, int user_id, String message, LocalDateTime send_at) {
        this.message_id = message_id;
        this.chatroom_id = chatroom_id;
        this.message = message;
        this.user_id = user_id;
        this.send_at = send_at;

    }

    // getter - setter
    public int getMessage_id() {return message_id;}
    public int getChatroom_id() {return chatroom_id;}
    public int getUser_id() {return user_id;}
    public String getMessage() {return message;}
    public LocalDateTime getSend_at() {return send_at;}

    public void setMessage(int message_id) {this.message_id = message_id;}
    public void setChatroom_id(int chatroom_id) {this.chatroom_id = chatroom_id;}
    public void setUser_id(int user_id) {this.user_id = user_id;}
    public void setMessage(String message) {this.message = message;}
    public void setSend_at(LocalDateTime send_at) {this.send_at = send_at;}

    @Override
    public String toString() {
        return "chat_message [message_id=" + message_id +
                ", chatroom_id=" + chatroom_id  +
                ", user_id=" + user_id  +
                ", message=" + message +
                ", send_at=" + send_at +
                "]";
    }
}
