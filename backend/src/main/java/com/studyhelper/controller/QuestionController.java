package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.entity.StudyQuestion;
import com.studyhelper.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/list")
    public Result<List<StudyQuestion>> list(@RequestParam Long materialId) {
        return Result.ok(questionService.listByMaterialId(materialId));
    }

    /**
     * @param types   可选，逗号分隔：single,multiple,fill,essay；单值时提示词强制该题型
     * @param replace true=清空本资料旧题再写入（默认）；false=在已有题目后追加（多题型分批生成时必须首轮 true、后续 false）
     */
    @PostMapping("/generate")
    public Result<List<StudyQuestion>> generate(
            @RequestParam Long materialId,
            @RequestParam(value = "count", defaultValue = "5") Integer count,
            @RequestParam(value = "types", required = false) String types,
            @RequestParam(value = "replace", defaultValue = "true") boolean replace,
            @AuthenticationPrincipal Long userId) {
        try {
            List<StudyQuestion> list = questionService.generateAndSave(materialId, userId, count, replace, types);
            return Result.ok(list);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        if (questionService.deleteByIdAndUser(id, userId)) {
            return Result.ok(null);
        }
        return Result.fail("删除失败或无权操作");
    }
}
