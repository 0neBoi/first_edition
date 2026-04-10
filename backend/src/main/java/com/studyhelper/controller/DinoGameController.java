package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.dto.DinoLeaderboardItem;
import com.studyhelper.service.DinoGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/game/dino")
@RequiredArgsConstructor
public class DinoGameController {

    private final DinoGameService dinoGameService;

    /**
     * 排行榜（按用户历史最佳分降序）
     */
    @GetMapping("/leaderboard")
    public Result<List<DinoLeaderboardItem>> leaderboard(
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(dinoGameService.leaderboard(size));
    }

    /** 当前登录用户在服务器上的最佳分（未上榜过则为 null） */
    @GetMapping("/my-best")
    public Result<Integer> myBest(@AuthenticationPrincipal Long userId) {
        return Result.ok(dinoGameService.getMyBest(userId));
    }

    /** 当前用户完整排行信息（含未进入前 N 名时的真实名次） */
    @GetMapping("/my-entry")
    public Result<DinoLeaderboardItem> myEntry(@AuthenticationPrincipal Long userId) {
        return Result.ok(dinoGameService.getMyLeaderboardEntry(userId));
    }

    /**
     * 提交本局得分（仅登录学生；若破个人纪录则更新）
     */
    @PostMapping("/best")
    public Result<Void> submitBest(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        int score = 0;
        if (body != null && body.get("score") != null) {
            Object s = body.get("score");
            if (s instanceof Number n) {
                score = n.intValue();
            } else {
                score = (int) Double.parseDouble(String.valueOf(s).trim());
            }
        }
        try {
            dinoGameService.submitBest(userId, score);
            return Result.ok(null);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
