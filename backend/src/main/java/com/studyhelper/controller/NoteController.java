package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.entity.StudyNote;
import com.studyhelper.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping("/list")
    public Result<List<StudyNote>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword) {
        return Result.ok(noteService.list(userId, tag, keyword));
    }

    @GetMapping("/{id}")
    public Result<StudyNote> get(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        StudyNote n = noteService.getByIdAndUser(id, userId);
        if (n == null) {
            return Result.fail("笔记不存在或无权访问");
        }
        return Result.ok(n);
    }

    /** 使用显式子路径，避免部分环境下对 POST /note（空方法级 path）未注册导致 NoResourceFoundException */
    @PostMapping("/create")
    public Result<StudyNote> create(@AuthenticationPrincipal Long userId, @RequestBody Map<String, String> body) {
        String title = body != null ? body.get("title") : null;
        String content = body != null ? body.get("content") : null;
        String tags = body != null ? body.get("tags") : null;
        return Result.ok(noteService.create(userId, title, content, tags));
    }

    @PutMapping("/{id}")
    public Result<StudyNote> update(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        String title = body != null ? body.get("title") : null;
        String content = body != null ? body.get("content") : null;
        String tags = body != null ? body.get("tags") : null;
        StudyNote n = noteService.update(id, userId, title, content, tags);
        if (n == null) {
            return Result.fail("更新失败或无权操作");
        }
        return Result.ok(n);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        if (noteService.delete(id, userId)) {
            return Result.ok(null);
        }
        return Result.fail("删除失败或无权操作");
    }
}
