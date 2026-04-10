package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.service.QwenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 问答接口（Qwen）
 */
@RestController
@RequestMapping("/qwen")
@RequiredArgsConstructor
public class QwenController {

    private final QwenService qwenService;

    @PostMapping("/ask")
    public Result<String> ask(@RequestParam String question) {
        String answer = qwenService.ask(question);
        return Result.ok(answer);
    }
}
