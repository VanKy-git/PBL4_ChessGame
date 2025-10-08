package Model.Entity;

import java.time.LocalDateTime;
public class room {
    private int room_id;
    private int host_id;
    private int guest_id;
    private String room_status;
    private LocalDateTime create_at;

    public room () {}

    public room (int room_id, int host_id) {
        this.room_id = room_id;
        this.host_id = host_id;
        this.guest_id = 0;
        this.room_status = "Waiting";
        this.create_at = LocalDateTime.now();
    }
    public room (int room_id, int host_id, int guest_id, String room_status, LocalDateTime create_at) {
        this.room_id = room_id;
        this.host_id = host_id;
        this.guest_id = guest_id;
        this.room_status = room_status;
        this.create_at = create_at;
    }

    // getter - setter
    public int getRoom_id() { return room_id; }
    public int getHost_id() { return host_id; }
    public int getGuest_id() { return guest_id; }
    public String getRoom_status() { return room_status; }
    public LocalDateTime getCreate_at() { return create_at; }

    public void setRoom_id(int room_id) { this.room_id = room_id; }
    public void setHost_id(int host_id) { this.host_id = host_id; }
    public void setGuest_id(int guest_id) { this.guest_id = guest_id; }
    public void setRoom_status(String room_status) { this.room_status = room_status; }
    public void setCreate_at(LocalDateTime create_at) { this.create_at = create_at; }

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
