package com.database.server.Controller;

import com.database.server.Entity.matches;
import com.database.server.DAO.matchesDAO;
import com.database.server.Service.matchesService;
import com.database.server.Entity.user;
import com.database.server.Utils.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * matchesController - Xử lý các request liên quan đến trận đấu (Match)
 */
public class matchesController {

    private final matchesService matchService;
    private final Gson gson;

    public matchesController(EntityManagerFactory emf ) {
        // emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        this.matchService = new matchesService(emf);
        this.gson = GsonUtils.gson;
    }

    // ========================= HELPER =========================

    private String successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return gson.toJson(response);
    }

    private String errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return gson.toJson(response);
    }

    // ========================= CRUD =========================

    /** Lấy tất cả các trận */
    public String getAllMatches() {
        try {
            List<matches> list = matchService.getAllMatches();
            return successResponse(list);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Lỗi lấy danh sách trận đấu: " + e.getMessage());
        }
    }

    /** Lấy thông tin trận theo ID */
    public String getMatchById(String requestJson) {
        try {
            Map<String, Object> req = gson.fromJson(requestJson, Map.class);
            if (!req.containsKey("matchId")) {
                return errorResponse("Thiếu tham số matchId!");
            }

            int matchId = ((Number) req.get("matchId")).intValue();
            matches m = matchService.getMatchById(matchId);

            if (m == null) return errorResponse("Không tìm thấy trận đấu!");
            return successResponse(m);
        } catch (JsonSyntaxException e) {
            return errorResponse("Dữ liệu JSON không hợp lệ!");
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Lỗi xử lý getMatchById: " + e.getMessage());
        }
    }

    /** Xóa trận đấu */
    public String deleteMatch(String requestJson) {
        try {
            Map<String, Object> req = gson.fromJson(requestJson, Map.class);
            if (!req.containsKey("matchId")) {
                return errorResponse("Thiếu tham số matchId!");
            }

            int matchId = ((Number) req.get("matchId")).intValue();
            boolean result = matchService.deleteMatch(matchId);

            return result
                    ? successResponse("Xóa trận đấu thành công!")
                    : errorResponse("Xóa thất bại hoặc không tìm thấy trận đấu!");
        } catch (JsonSyntaxException e) {
            return errorResponse("Dữ liệu JSON không hợp lệ!");
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Lỗi xử lý deleteMatch: " + e.getMessage());
        }
    }

    /** ✅ Tạo trận đấu mới giữa 2 user (đối tượng user, không phải int) */
    public String createMatch(String requestJson) {
        try {
            Map<String, Object> req = gson.fromJson(requestJson, Map.class);

            if (!req.containsKey("player1") || !req.containsKey("player2")) {
                return errorResponse("Thiếu thông tin người chơi!");
            }

            // Parse 2 user từ JSON
            String player1Json = gson.toJson(req.get("player1"));
            String player2Json = gson.toJson(req.get("player2"));
            user player1 = gson.fromJson(player1Json, user.class);
            user player2 = gson.fromJson(player2Json, user.class);

            // Gọi service
            matches m = matchService.createMatch(player1, player2);
            return successResponse(m);

        } catch (JsonSyntaxException e) {
            return errorResponse("Dữ liệu JSON không hợp lệ!");
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Lỗi tạo trận đấu: " + e.getMessage());
        }
    }

    // ========================= MỞ RỘNG: MATCH + NGƯỜI CHƠI =========================

    /** Lấy thông tin chi tiết 1 trận (bao gồm người chơi) */
    public String getMatchWithPlayers(String requestJson) {
        try {
            Map<String, Object> req = gson.fromJson(requestJson, Map.class);
            int matchId = ((Number) req.get("matchId")).intValue();

            matchesDAO.match_player data = matchService.getMatchWithPlayers(matchId);
            if (data == null) return errorResponse("Không tìm thấy trận đấu!");
            return successResponse(data);
        } catch (JsonSyntaxException e) {
            return errorResponse("Dữ liệu JSON không hợp lệ!");
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Lỗi getMatchWithPlayers: " + e.getMessage());
        }
    }

    /** Lấy danh sách tất cả trận + người chơi */
    public String getAllMatchesWithPlayers() {
        try {
            List<matchesDAO.match_player> list = matchService.getAllMatchesWithPlayers();
            return successResponse(list);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Lỗi lấy danh sách trận đấu + người chơi: " + e.getMessage());
        }
    }

// TRONG matchesController.java (SỬA ĐỔI)
public String getHistoryByUserId(String userIdStr) { 
    try {
        // 1. Phân tích cú pháp chuỗi thành số nguyên trực tiếp
        int id = Integer.parseInt(userIdStr); 
        
        // 2. Gọi Service như bình thường
        List<matches> list = matchService.getMatchesWithPlayersByUser(id); 
        System.out.println("Lịch sử trận đấu cho user ID " + id + ": " + list);
        // 3. Xây dựng JSON response
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("history", list);
        
        return gson.toJson(res);
    } 
    catch (NumberFormatException e) {
        return errorResponse("ID người dùng không hợp lệ.");
    } 
    catch(IllegalArgumentException e)
    {
        System.out.println("Lỗi: " + e.getMessage());
        return errorResponse("Lỗi: " + e.getMessage());
    } 
    catch (Exception e) {
        e.printStackTrace();
        return errorResponse("Lỗi xử lý lịch sử: " + e.getMessage());
    }
}

    // ========================= ROUTER =========================

    public String handleRequest(String action, String requestJson) {
        try {
            switch (action) {
                case "getAllMatches":
                    return getAllMatches();
                case "getMatchById":
                    return getMatchById(requestJson);
                case "createMatch":
                    return createMatch(requestJson);
                case "deleteMatch":
                    return deleteMatch(requestJson);
                case "getMatchWithPlayers":
                    return getMatchWithPlayers(requestJson);
                case "getAllMatchesWithPlayers":
                    return getAllMatchesWithPlayers();
                case "getHistoryByUserId":
                    return getHistoryByUserId(requestJson);
                
                default:
                    return errorResponse("Action không hợp lệ: " + action);
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse("Lỗi xử lý request: " + e.getMessage());
        }
    }
}
