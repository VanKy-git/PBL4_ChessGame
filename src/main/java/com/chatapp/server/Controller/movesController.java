package com.chatapp.server.Controller;

import com.chatapp.server.Model.Entity.moves;
import com.chatapp.server.Model.Entity.matches;
import com.chatapp.server.Service.movesService;
import com.chatapp.server.Utils.GsonUtils;
import com.google.gson.JsonObject;

import java.util.List;

public class movesController {

    private final movesService moveService = new movesService();

    public String addMove(moves move) {
        JsonObject json = new JsonObject();
        if (moveService.saveMove(move)) {
            json.addProperty("success", true);
            json.addProperty("message", "Thêm nước đi thành công");
        } else {
            json.addProperty("success", false);
            json.addProperty("message", "Thêm nước đi thất bại");
        }
        return GsonUtils.gson.toJson(json);
    }

    public String getAllMoves() {
        List<moves> list = moveService.getAllMoves();
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.add("moves", GsonUtils.gson.toJsonTree(list));
        return GsonUtils.gson.toJson(json);
    }

    public String getMovesByMatch(matches match) {
        List<moves> list = moveService.getMovesByMatch(match);
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.add("moves", GsonUtils.gson.toJsonTree(list));
        return GsonUtils.gson.toJson(json);
    }

    public String deleteMove(int id) {
        JsonObject json = new JsonObject();
        if (moveService.deleteMove(id)) {
            json.addProperty("success", true);
            json.addProperty("message", "Xóa nước đi thành công");
        } else {
            json.addProperty("success", false);
            json.addProperty("message", "Xóa nước đi thất bại");
        }
        return GsonUtils.gson.toJson(json);
    }
}
