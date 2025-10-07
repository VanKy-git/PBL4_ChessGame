package Model.Entity;

import java.time.LocalDateTime;
public class matches {
    private int match_id;
    private int player1_id;
    private int player2_id;
    private LocalDateTime start_time;
    private LocalDateTime end_time;
    private String match_status;
    private String pgn_notation;

    public matches(){}

    public matches (int match_id, int player1_id, int player2_id, LocalDateTime start_time, LocalDateTime end_time){
        this.match_id = match_id;
        this.player1_id = player1_id;
        this.player2_id = player2_id;
        this.start_time = start_time;
        this.end_time = end_time;
        this.match_status = "Playing";
        this.pgn_notation = "None";
    }

    public matches (int match_id, int player1_id, int player2_id, LocalDateTime start_time, LocalDateTime end_time, String match_status, String pgn_nonation ){
        this.match_id = match_id;
        this.player1_id = player1_id;
        this.player2_id = player2_id;
        this.start_time = start_time;
        this.end_time = end_time;
        this.match_status = match_status;
        this.pgn_notation = pgn_nonation;
    }

    // getter - setter
    public int getMatch_id() { return match_id;}
    public int getPlayer1_id() { return player1_id;}
    public int getPlayer2_id() { return player2_id;}
    public LocalDateTime getStart_time() { return start_time;}
    public LocalDateTime getEnd_time() { return end_time;}
    public String getMatch_status() { return match_status;}
    public String getPgn_notation() { return pgn_notation;}

    public void setMatch_id(int match_id) {this.match_id = match_id;}
    public void setPlayer1_id(int player1_id) {this.player1_id = player1_id;}
    public void setPlayer2_id(int player2_id) {this.player2_id = player2_id;}
    public void setStart_time(LocalDateTime start_time){this.start_time = start_time;}
    public void setEnd_time(LocalDateTime end_time){this.end_time = end_time;}
    public void setMatch_status(String match_status) {this.match_status = match_status;}

    @Override
    public String toString() {
        return "Match{" +
                "match_id=" + match_id +
                ", player1_id=" + player1_id +
                ", player2_id=" + player2_id +
                ", start_time=" + start_time +
                ", end_time=" + end_time +
                ", match_status='" + match_status + '\'' +
                ", pgn_notation='" + pgn_notation + '\'' +
                '}';
    }
}
