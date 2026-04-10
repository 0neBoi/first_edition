package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.dto.LearningDashboardDto;
import com.studyhelper.service.LearningAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final LearningAnalyticsService learningAnalyticsService;

    @GetMapping("/dashboard")
    public Result<LearningDashboardDto> dashboard(@AuthenticationPrincipal Long userId) {
        return Result.ok(learningAnalyticsService.dashboard(userId));
    }
}
