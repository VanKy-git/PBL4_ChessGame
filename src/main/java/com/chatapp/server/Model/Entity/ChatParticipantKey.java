package com.chatapp.server.Model.Entity;

import java.io.Serializable;
import java.util.Objects;

public class ChatParticipantKey implements Serializable {
    private int chatroom_id;
    private int user_id;

    public ChatParticipantKey() {}

    public ChatParticipantKey(int chatroom_id, int user_id) {
        this.chatroom_id = chatroom_id;
        this.user_id = user_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatParticipantKey)) return false;
        ChatParticipantKey that = (ChatParticipantKey) o;
        return chatroom_id == that.chatroom_id && user_id == that.user_id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatroom_id, user_id);
    }
}
