package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.entity.StudyTodo;
import com.studyhelper.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/todo")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping("/list")
    public Result<List<StudyTodo>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) Integer priority) {
        return Result.ok(todoService.list(userId, status, dueFrom, dueTo, priority));
    }

    @GetMapping("/today")
    public Result<List<StudyTodo>> today(@AuthenticationPrincipal Long userId) {
        return Result.ok(todoService.listToday(userId));
    }

    @GetMapping("/{id}")
    public Result<StudyTodo> get(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        StudyTodo t = todoService.getByIdAndUser(id, userId);
        if (t == null) {
            return Result.fail("待办不存在或无权访问");
        }
        return Result.ok(t);
    }

    @PostMapping("/create")
    public Result<StudyTodo> create(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> body) {
        String title = str(body, "title");
        String content = str(body, "content");
        Integer status = intVal(body, "status");
        LocalDate dueDate = dateVal(body, "dueDate");
        Integer priority = intVal(body, "priority");
        return Result.ok(todoService.create(userId, title, content, status, dueDate, priority));
    }

    @PutMapping("/{id}")
    public Result<StudyTodo> update(@PathVariable Long id, @AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> body) {
        String title = str(body, "title");
        String content = str(body, "content");
        Integer status = intVal(body, "status");
        LocalDate dueDate = dateVal(body, "dueDate");
        Integer priority = intVal(body, "priority");
        Boolean clearDue = boolVal(body, "clearDueDate");
        StudyTodo t = todoService.update(id, userId, title, content, status, dueDate, priority, clearDue);
        if (t == null) {
            return Result.fail("更新失败或无权操作");
        }
        return Result.ok(t);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        if (todoService.delete(id, userId)) {
            return Result.ok(null);
        }
        return Result.fail("删除失败或无权操作");
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key)) {
            return null;
        }
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Integer intVal(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return null;
        }
        Object v = body.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean boolVal(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return null;
        }
        Object v = body.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    private static LocalDate dateVal(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return null;
        }
        Object v = body.get(key);
        if (v instanceof LocalDate d) {
            return d;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        return LocalDate.parse(s);
    }
}
