//package com.chatapp.server.Test;
//
//import com.chatapp.server.Controller.movesController;
//import com.chatapp.server.Model.Entity.moves;
//import com.chatapp.server.Model.Entity.matches;
//import com.chatapp.server.Model.Entity.user;
//
//public class movesTest {
//
//    public static void main(String[] args) {
//        movesController controller = new movesController();
//
//        // Tạo dữ liệu giả lập
//        matches match = new matches();
//        match.setMatchId(1);
//
//        user player = new user();
//        player.setUserId(2);
//
//        moves move = new moves(match, player, 1, "e2", "e4", "Pawn");
//
//        // Thêm move
//        System.out.println(controller.addMove(move));
//
//        // Lấy danh sách move theo match
//        System.out.println(controller.getMovesByMatch(match));
//
//        // Lấy toàn bộ move
//        System.out.println(controller.getAllMoves());
//
//        // Xóa move (ví dụ id=1)
//        System.out.println(controller.deleteMove(1));
//    }
//}
