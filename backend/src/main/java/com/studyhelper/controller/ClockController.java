package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.entity.StudyClock;
import com.studyhelper.service.ClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clock")
@RequiredArgsConstructor
public class ClockController {

    private final ClockService clockService;

    @PostMapping("/day")
    public Result<StudyClock> upsertDay(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> body) {
        LocalDate date = null;
        if (body != null && body.get("clockDate") != null) {
            Object v = body.get("clockDate");
            if (v instanceof LocalDate d) {
                date = d;
            } else {
                date = LocalDate.parse(String.valueOf(v).trim());
            }
        }
        int minutes = 0;
        if (body != null && body.get("minutes") != null) {
            Object m = body.get("minutes");
            if (m instanceof Number n) {
                minutes = n.intValue();
            } else {
                minutes = Integer.parseInt(String.valueOf(m).trim());
            }
        }
        String remark = body != null && body.get("remark") != null ? String.valueOf(body.get("remark")) : null;
        return Result.ok(clockService.upsertDay(userId, date, minutes, remark));
    }

    @GetMapping("/range")
    public Result<List<StudyClock>> range(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(clockService.listRange(userId, from, to));
    }

    @GetMapping("/month")
    public Result<Map<String, Object>> month(
            @AuthenticationPrincipal Long userId,
            @RequestParam String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth.trim());
        return Result.ok(clockService.monthSummary(userId, ym));
    }

    @GetMapping("/streak")
    public Result<Map<String, Integer>> streak(@AuthenticationPrincipal Long userId) {
        return Result.ok(Map.of("streakDays", clockService.streakDays(userId)));
    }
}
