package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.entity.StudyMaterial;
import com.studyhelper.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping("/upload")
    public Result<StudyMaterial> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) {
        try {
            StudyMaterial material = materialService.upload(userId, file);
            return Result.ok(material);
        } catch (IOException e) {
            return Result.fail("文件解析失败：" + e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<StudyMaterial>> list(@AuthenticationPrincipal Long userId) {
        return Result.ok(materialService.list(userId));
    }

    @GetMapping("/{id}")
    public Result<StudyMaterial> getById(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        StudyMaterial material = materialService.getByIdAndUser(id, userId);
        if (material == null) {
            return Result.fail("资料不存在或无权访问");
        }
        return Result.ok(material);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        if (materialService.deleteByIdAndUser(id, userId)) {
            return Result.ok(null);
        }
        return Result.fail("删除失败或无权操作");
    }
}
