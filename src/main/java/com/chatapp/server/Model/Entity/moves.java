package com.chatapp.server.Model.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity biểu diễn bảng "moves" - lưu từng nước đi trong một trận đấu.
 */
@Entity
@Table(name = "moves")
public class moves {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "move_id")
    private int moveId;

    /** 🔗 Trận đấu chứa nước đi này */
    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private matches match;

    /** 👤 Người chơi thực hiện nước đi */
    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private user player;

    @Column(name = "move_number")
    private int moveNumber;

    @Column(name = "from_square", length = 5)
    private String fromSquare;

    @Column(name = "to_square", length = 5)
    private String toSquare;

    @Column(name = "piece", length = 20)
    private String piece;

    @Column(name = "active")
    private boolean active;

    @Column(name = "move_time")
    private LocalDateTime moveTime;

    public moves() {}

    public moves(matches match, user player, int moveNumber,
                 String fromSquare, String toSquare, String piece) {
        this.match = match;
        this.player = player;
        this.moveNumber = moveNumber;
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.piece = piece;
        this.active = true;
        this.moveTime = LocalDateTime.now();
    }

    // Getters / Setters
    public int getMoveId() { return moveId; }
    public void setMoveId(int moveId) { this.moveId = moveId; }

    public matches getMatch() { return match; }
    public void setMatch(matches match) { this.match = match; }

    public user getPlayer() { return player; }
    public void setPlayer(user player) { this.player = player; }

    public int getMoveNumber() { return moveNumber; }
    public void setMoveNumber(int moveNumber) { this.moveNumber = moveNumber; }

    public String getFromSquare() { return fromSquare; }
    public void setFromSquare(String fromSquare) { this.fromSquare = fromSquare; }

    public String getToSquare() { return toSquare; }
    public void setToSquare(String toSquare) { this.toSquare = toSquare; }

    public String getPiece() { return piece; }
    public void setPiece(String piece) { this.piece = piece; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getMoveTime() { return moveTime; }
    public void setMoveTime(LocalDateTime moveTime) { this.moveTime = moveTime; }

    @Override
    public String toString() {
        return "moves{" +
                "id=" + moveId +
                ", match=" + (match != null ? match.getMatchId() : "null") +
                ", player=" + (player != null ? player.getUserName() : "null") +
                ", move=" + fromSquare + "→" + toSquare +
                '}';
    }
}
