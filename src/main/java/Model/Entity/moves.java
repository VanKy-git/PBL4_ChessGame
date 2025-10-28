package Model.Entity;

import java.time.LocalDateTime;

public class moves {
    private int move_id;         // ID của nước đi
    private int match_id;        // ID trận đấu
    private int player_id;       // ID người chơi thực hiện nước đi
    private int move_number;     // Thứ tự nước đi trong trận
    private String from_square;  // Ô cờ bắt đầu (vd: "e2")
    private String to_square;    // Ô cờ kết thúc (vd: "e4")
    private String piece;        // Quân cờ di chuyển (vd: "Pawn", "Knight")
    private boolean active;      // Trạng thái còn hiệu lực (true nếu chưa undo)
    private LocalDateTime move_time; // Thời điểm thực hiện nước đi

    public moves() {}

    public moves(int move_id, int match_id, int player_id, int move_number,
                 String from_square, String to_square, String piece,
                 boolean active, LocalDateTime move_time) {
        this.move_id = move_id;
        this.match_id = match_id;
        this.player_id = player_id;
        this.move_number = move_number;
        this.from_square = from_square;
        this.to_square = to_square;
        this.piece = piece;
        this.active = active;
        this.move_time = move_time;
    }

    // Getters / Setters
    public int getMove_id() { return move_id; }
    public void setMove_id(int move_id) { this.move_id = move_id; }

    public int getMatch_id() { return match_id; }
    public void setMatch_id(int match_id) { this.match_id = match_id; }

    public int getPlayer_id() { return player_id; }
    public void setPlayer_id(int player_id) { this.player_id = player_id; }

    public int getMove_number() { return move_number; }
    public void setMove_number(int move_number) { this.move_number = move_number; }

    public String getFrom_square() { return from_square; }
    public void setFrom_square(String from_square) { this.from_square = from_square; }

    public String getTo_square() { return to_square; }
    public void setTo_square(String to_square) { this.to_square = to_square; }

    public String getPiece() { return piece; }
    public void setPiece(String piece) { this.piece = piece; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getMove_time() { return move_time; }
    public void setMove_time(LocalDateTime move_time) { this.move_time = move_time; }

    @Override
    public String toString() {
        return "moves{" +
                "move_id=" + move_id +
                ", match_id=" + match_id +
                ", player_id=" + player_id +
                ", move_number=" + move_number +
                ", from_square='" + from_square + '\'' +
                ", to_square='" + to_square + '\'' +
                ", piece='" + piece + '\'' +
                ", active=" + active +
                ", move_time=" + move_time +
                '}';
    }
}
