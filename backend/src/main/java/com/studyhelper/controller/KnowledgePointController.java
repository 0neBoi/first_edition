package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.entity.StudyKnowledgePoint;
import com.studyhelper.service.KnowledgePointService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    @GetMapping("/list")
    public Result<List<StudyKnowledgePoint>> list(@RequestParam Long materialId) {
        return Result.ok(knowledgePointService.listByMaterialId(materialId));
    }

    @PostMapping("/extract")
    public Result<List<StudyKnowledgePoint>> extract(
            @RequestParam Long materialId,
            @AuthenticationPrincipal Long userId) {
        try {
            List<StudyKnowledgePoint> list = knowledgePointService.extractAndSave(materialId, userId);
            return Result.ok(list);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        if (knowledgePointService.deleteByIdAndUser(id, userId)) {
            return Result.ok(null);
        }
        return Result.fail("删除失败或无权操作");
    }
}
