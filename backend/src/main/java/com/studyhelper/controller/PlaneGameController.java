package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.dto.DinoLeaderboardItem;
import com.studyhelper.service.PlaneGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/game/plane")
@RequiredArgsConstructor
public class PlaneGameController {

    private final PlaneGameService planeGameService;

    @GetMapping("/leaderboard")
    public Result<List<DinoLeaderboardItem>> leaderboard(@RequestParam(defaultValue = "20") int size) {
        return Result.ok(planeGameService.leaderboard(size));
    }

    @GetMapping("/my-best")
    public Result<Integer> myBest(@AuthenticationPrincipal Long userId) {
        return Result.ok(planeGameService.getMyBest(userId));
    }

    @GetMapping("/my-entry")
    public Result<DinoLeaderboardItem> myEntry(@AuthenticationPrincipal Long userId) {
        return Result.ok(planeGameService.getMyLeaderboardEntry(userId));
    }

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
            planeGameService.submitBest(userId, score);
            return Result.ok(null);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
