package com.chatapp.server.Model.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity biểu diễn bảng "matches" trong cơ sở dữ liệu.
 * Mỗi bản ghi đại diện cho một trận giữa 2 người chơi.
 */
@Entity
@Table(name = "matches")
public class matches {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private int matchId;

    // ================== QUAN HỆ ==================

    /** 👤 Người chơi 1 */
    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private user player1;

    /** 👤 Người chơi 2 */
    @ManyToOne
    @JoinColumn(name = "player2_id", nullable = false)
    private user player2;

    /** ♟️ Danh sách nước đi trong trận đấu */
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<moves> moveList;

    // ================== CỘT DỮ LIỆU ==================

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "match_status", length = 20, nullable = false)
    private String matchStatus;

    @Column(name = "pgn_notation", columnDefinition = "TEXT")
    private String pgnNotation;

    // ================== CONSTRUCTOR ==================

    public matches() {}

    public matches(user player1, user player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.startTime = LocalDateTime.now();
        this.matchStatus = "Playing";
        this.pgnNotation = "None";
    }

    // ================== GETTER / SETTER ==================

    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public user getPlayer1() { return player1; }
    public void setPlayer1(user player1) { this.player1 = player1; }

    public user getPlayer2() { return player2; }
    public void setPlayer2(user player2) { this.player2 = player2; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    public String getPgnNotation() { return pgnNotation; }
    public void setPgnNotation(String pgnNotation) { this.pgnNotation = pgnNotation; }

    public List<moves> getMoveList() { return moveList; }
    public void setMoveList(List<moves> moveList) { this.moveList = moveList; }

    @Override
    public String toString() {
        return "matches{" +
                "matchId=" + matchId +
                ", player1=" + (player1 != null ? player1.getUserName() : "null") +
                ", player2=" + (player2 != null ? player2.getUserName() : "null") +
                ", status='" + matchStatus + '\'' +
                '}';
    }
}
