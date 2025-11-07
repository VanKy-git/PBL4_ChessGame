package com.database.server.Controller;

import com.database.server.Entity.moves;
import com.database.server.Entity.matches;
import com.database.server.Entity.user;
import com.database.server.Service.movesService;
import com.database.server.Service.matchesService;
import com.database.server.Service.userService;
import com.database.server.DAO.movesDAO;
import com.database.server.Utils.GsonUtils;
import com.google.gson.Gson;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * movesController - Xử lý các request liên quan đến Moves
 * - Chỉ nhận request, parse JSON, gọi Service, và trả về response
 * - KHÔNG xử lý logic nghiệp vụ (logic thuộc về Service)
 */
public class movesController {

    private final movesService movesService;
    private final matchesService matchesService;
    private final userService userService;
    private final Gson gson;

    public movesController(EntityManagerFactory emf ) {
        emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        this.movesService = new movesService(emf);
        this.matchesService = new matchesService(emf);
        this.userService = new userService(emf);
        this.gson = GsonUtils.gson;
    }

    // ========================= HELPER METHODS =========================

    /**
     * Tạo response thành công với data
     */
    private String successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return gson.toJson(response);
    }

    /**
     * Tạo response thành công với message
     */
    private String successResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return gson.toJson(response);
    }

    /**
     * Tạo response lỗi
     */
    private String errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return gson.toJson(response);
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Tạo nước đi mới
     * Request: { "matchId": 1, "playerId": 1, "moveNumber": 1, "fromSquare": "e2", "toSquare": "e4", "piece": "Pawn" }
     */
    public String createMove(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int matchId = ((Number) request.get("matchId")).intValue();
            int playerId = ((Number) request.get("playerId")).intValue();
            int moveNumber = ((Number) request.get("moveNumber")).intValue();
            String fromSquare = (String) request.get("fromSquare");
            String toSquare = (String) request.get("toSquare");
            String piece = (String) request.get("piece");

            matches match = matchesService.getMatchById(matchId);
            user player = userService.getUserById(playerId);

            if (match == null) {
                return errorResponse("Trận đấu không tồn tại");
            }
            if (player == null) {
                return errorResponse("Người chơi không tồn tại");
            }

            moves newMove = movesService.createMove(match, player, moveNumber, fromSquare, toSquare, piece);
            return successResponse(newMove);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin nước đi theo ID
     * Request: { "moveId": 1 }
     */
    public String getMoveById(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int moveId = ((Number) request.get("moveId")).intValue();

            moves move = movesService.getMoveById(moveId);
            if (move != null) {
                return successResponse(move);
            } else {
                return errorResponse("Không tìm thấy nước đi");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả nước đi
     * Request: {}
     */
    public String getAllMoves() {
        try {
            List<moves> movesList = movesService.getAllMoves();
            return successResponse(movesList);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Cập nhật nước đi
     * Request: { "moveId": 1, "fromSquare": "e2", "toSquare": "e4", "piece": "Pawn", "active": true }
     */
    public String updateMove(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int moveId = ((Number) request.get("moveId")).intValue();

            moves move = movesService.getMoveById(moveId);
            if (move == null) {
                return errorResponse("Nước đi không tồn tại");
            }

            if (request.containsKey("fromSquare")) {
                move.setFromSquare((String) request.get("fromSquare"));
            }
            if (request.containsKey("toSquare")) {
                move.setToSquare((String) request.get("toSquare"));
            }
            if (request.containsKey("piece")) {
                move.setPiece((String) request.get("piece"));
            }
            if (request.containsKey("active")) {
                move.setActive((Boolean) request.get("active"));
            }

            boolean success = movesService.updateMove(move);
            if (success) {
                return successResponse("Cập nhật nước đi thành công");
            } else {
                return errorResponse("Cập nhật nước đi thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa nước đi
     * Request: { "moveId": 1 }
     */
    public String deleteMove(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int moveId = ((Number) request.get("moveId")).intValue();

            boolean success = movesService.deleteMove(moveId);
            if (success) {
                return successResponse("Xóa nước đi thành công");
            } else {
                return errorResponse("Xóa nước đi thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Vô hiệu hóa nước đi
     * Request: { "moveId": 1 }
     */
    public String deactivateMove(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int moveId = ((Number) request.get("moveId")).intValue();

            boolean success = movesService.deactivateMove(moveId);
            if (success) {
                return successResponse("Vô hiệu hóa nước đi thành công");
            } else {
                return errorResponse("Vô hiệu hóa nước đi thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= TRUY VẤN THEO MATCH =========================

    /**
     * Lấy tất cả nước đi của trận đấu
     * Request: { "matchId": 1 }
     */
    public String getMovesByMatch(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int matchId = ((Number) request.get("matchId")).intValue();

            List<moves> movesList = movesService.getMovesByMatch(matchId);
            return successResponse(movesList);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy các nước đi đang hoạt động của trận đấu
     * Request: { "matchId": 1 }
     */
    public String getActiveMovesByMatch(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int matchId = ((Number) request.get("matchId")).intValue();

            List<moves> movesList = movesService.getActiveMovesByMatch(matchId);
            return successResponse(movesList);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy nước đi mới nhất của trận đấu
     * Request: { "matchId": 1 }
     */
    public String getLastMoveOfMatch(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int matchId = ((Number) request.get("matchId")).intValue();

            moves lastMove = movesService.getLastMoveOfMatch(matchId);
            if (lastMove != null) {
                return successResponse(lastMove);
            } else {
                return errorResponse("Chưa có nước đi nào trong trận này");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Đếm số nước đi trong trận đấu
     * Request: { "matchId": 1 }
     */
    public String getMoveCountByMatch(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int matchId = ((Number) request.get("matchId")).intValue();

            int count = movesService.getMoveCountByMatch(matchId);

            Map<String, Object> result = new HashMap<>();
            result.put("matchId", matchId);
            result.put("moveCount", count);

            return successResponse(result);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= TRUY VẤN THEO PLAYER =========================

    /**
     * Lấy tất cả nước đi của người chơi
     * Request: { "playerId": 1 }
     */
    public String getMovesByPlayer(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int playerId = ((Number) request.get("playerId")).intValue();

            List<moves> movesList = movesService.getMovesByPlayer(playerId);
            return successResponse(movesList);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy nước đi của người chơi trong trận đấu cụ thể
     * Request: { "playerId": 1, "matchId": 1 }
     */
    public String getMovesByPlayerInMatch(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int playerId = ((Number) request.get("playerId")).intValue();
            int matchId = ((Number) request.get("matchId")).intValue();

            List<moves> movesList = movesService.getMovesByPlayerInMatch(playerId, matchId);
            return successResponse(movesList);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Đếm tổng số nước đi của người chơi
     * Request: { "playerId": 1 }
     */
    public String getMoveCountByPlayer(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int playerId = ((Number) request.get("playerId")).intValue();

            int count = movesService.getMoveCountByPlayer(playerId);

            Map<String, Object> result = new HashMap<>();
            result.put("playerId", playerId);
            result.put("moveCount", count);

            return successResponse(result);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= TRUY VẤN NÂNG CAO =========================

    /**
     * Lấy nước đi theo loại quân cờ
     * Request: { "piece": "Pawn" }
     */
    public String getMovesByPiece(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            String piece = (String) request.get("piece");

            List<moves> movesList = movesService.getMovesByPiece(piece);
            return successResponse(movesList);
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa tất cả nước đi của trận đấu
     * Request: { "matchId": 1 }
     */
    public String deleteAllMovesByMatch(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int matchId = ((Number) request.get("matchId")).intValue();

            boolean success = movesService.deleteAllMovesByMatch(matchId);
            if (success) {
                return successResponse("Đã xóa tất cả nước đi của trận đấu");
            } else {
                return errorResponse("Xóa nước đi thất bại");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Lấy thống kê nước đi của trận đấu
     * Request: { "matchId": 1 }
     */
    public String getMoveStatistics(String requestJson) {
        try {
            Map<String, Object> request = gson.fromJson(requestJson, Map.class);
            int matchId = ((Number) request.get("matchId")).intValue();

            movesDAO.MoveStatistics stats = movesService.getMoveStatistics(matchId);
            if (stats != null) {
                return successResponse(stats);
            } else {
                return errorResponse("Không có nước đi nào trong trận này");
            }
        } catch (Exception e) {
            return errorResponse("Lỗi: " + e.getMessage());
        }
    }

    // ========================= ROUTE HANDLER =========================

    /**
     * Xử lý request dựa trên action
     * @param action Hành động cần thực hiện
     * @param requestJson JSON request data
     * @return JSON response
     */
    public String handleRequest(String action, String requestJson) {
        try {
            switch (action) {
                // CRUD cơ bản
                case "createMove":
                    return createMove(requestJson);
                case "getMoveById":
                    return getMoveById(requestJson);
                case "getAllMoves":
                    return getAllMoves();
                case "updateMove":
                    return updateMove(requestJson);
                case "deleteMove":
                    return deleteMove(requestJson);
                case "deactivateMove":
                    return deactivateMove(requestJson);

                // Truy vấn theo Match
                case "getMovesByMatch":
                    return getMovesByMatch(requestJson);
                case "getActiveMovesByMatch":
                    return getActiveMovesByMatch(requestJson);
                case "getLastMoveOfMatch":
                    return getLastMoveOfMatch(requestJson);
                case "getMoveCountByMatch":
                    return getMoveCountByMatch(requestJson);

                // Truy vấn theo Player
                case "getMovesByPlayer":
                    return getMovesByPlayer(requestJson);
                case "getMovesByPlayerInMatch":
                    return getMovesByPlayerInMatch(requestJson);
                case "getMoveCountByPlayer":
                    return getMoveCountByPlayer(requestJson);

                // Truy vấn nâng cao
                case "getMovesByPiece":
                    return getMovesByPiece(requestJson);
                case "deleteAllMovesByMatch":
                    return deleteAllMovesByMatch(requestJson);

                // Thống kê
                case "getMoveStatistics":
                    return getMoveStatistics(requestJson);

                default:
                    return errorResponse("Action không hợp lệ: " + action);
            }
        } catch (Exception e) {
            return errorResponse("Lỗi xử lý request: " + e.getMessage());
        }
    }
}