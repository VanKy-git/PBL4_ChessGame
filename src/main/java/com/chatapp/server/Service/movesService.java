package com.chatapp.server.Service;

import com.chatapp.server.Model.DAO.movesDAO;
import com.chatapp.server.Model.Entity.moves;
import com.chatapp.server.Model.Entity.matches;
import java.util.List;

/**
 * Service xử lý logic nghiệp vụ cho moves (nước đi trong trận).
 */
public class movesService {

    private final movesDAO moveDAO = new movesDAO();

    /**
     * 🟢 Lưu nước đi mới.
     */
    public boolean saveMove(moves move) {
        return movesDAO.addMove(move);
    }

    /**
     * 🟡 Lấy danh sách nước đi theo trận.
     */
    public List<moves> getMovesByMatch(matches match) {
        return moveDAO.getMovesByMatch(match);
    }

    /**
     * 🔵 Lấy toàn bộ nước đi (debug).
     */
    public List<moves> getAllMoves() {
        return moveDAO.getAllMoves();
    }

    /**
     * 🔴 Xóa nước đi theo ID.
     */
    public boolean deleteMove(int moveId) {
        return moveDAO.deleteMove(moveId);
    }

    /**
     * 🧩 Cập nhật nước đi.
     */
    public boolean updateMove(moves move) {
        return moveDAO.updateMove(move);
    }

    /**
     * 🔍 Lấy move theo ID.
     */
    public moves getMoveById(int id) {
        return moveDAO.getMoveById(id);
    }
}
