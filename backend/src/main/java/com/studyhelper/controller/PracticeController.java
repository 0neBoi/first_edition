package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.dto.PracticeSubmitResult;
import com.studyhelper.entity.StudyQuestion;
import com.studyhelper.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/submit")
    public Result<PracticeSubmitResult> submit(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> body) {
        if (body == null || body.get("questionId") == null) {
            return Result.fail("缺少 questionId");
        }
        long qid = toLong(body.get("questionId"));
        String userAnswer = body.get("userAnswer") != null ? String.valueOf(body.get("userAnswer")) : "";
        Integer timeCost = null;
        if (body.get("timeCostMs") != null) {
            Object t = body.get("timeCostMs");
            if (t instanceof Number n) {
                timeCost = n.intValue();
            } else {
                timeCost = Integer.parseInt(String.valueOf(t).trim());
            }
        }
        try {
            return Result.ok(practiceService.submit(userId, qid, userAnswer, timeCost));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/wrong-book")
    public Result<List<StudyQuestion>> wrongBook(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long materialId) {
        return Result.ok(practiceService.listWrongBook(userId, materialId));
    }

    @GetMapping("/review-today")
    public Result<List<StudyQuestion>> reviewToday(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        int n = Math.min(Math.max(limit, 1), 50);
        return Result.ok(practiceService.reviewToday(userId, n));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@AuthenticationPrincipal Long userId) {
        return Result.ok(practiceService.stats(userId));
    }

    @GetMapping("/mastery")
    public Result<Map<Long, Integer>> mastery(
            @AuthenticationPrincipal Long userId,
            @RequestParam("questionIds") String questionIds) {
        if (questionIds == null || questionIds.isBlank()) {
            return Result.ok(Map.of());
        }
        List<Long> ids = java.util.Arrays.stream(questionIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toList());
        return Result.ok(practiceService.masteryForQuestions(userId, ids));
    }

    private static long toLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v).trim());
    }
}
