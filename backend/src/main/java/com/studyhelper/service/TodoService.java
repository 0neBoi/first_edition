package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyhelper.entity.StudyTodo;
import com.studyhelper.mapper.StudyTodoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final StudyTodoMapper todoMapper;

    public List<StudyTodo> list(
            Long userId,
            Integer status,
            LocalDate dueFrom,
            LocalDate dueTo,
            Integer priority) {
        LambdaQueryWrapper<StudyTodo> q = new LambdaQueryWrapper<>();
        q.eq(StudyTodo::getUserId, userId);
        if (status != null) {
            q.eq(StudyTodo::getStatus, status);
        }
        if (dueFrom != null) {
            q.ge(StudyTodo::getDueDate, dueFrom);
        }
        if (dueTo != null) {
            q.le(StudyTodo::getDueDate, dueTo);
        }
        if (priority != null) {
            q.eq(StudyTodo::getPriority, priority);
        }
        q.orderByAsc(StudyTodo::getStatus)
                .orderByDesc(StudyTodo::getPriority)
                .orderByAsc(StudyTodo::getDueDate)
                .orderByDesc(StudyTodo::getUpdateTime);
        return todoMapper.selectList(q);
    }

    /** 今日待办：未完成且（未定截止日或截止日不晚于今天） */
    public List<StudyTodo> listToday(Long userId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<StudyTodo> q = new LambdaQueryWrapper<>();
        q.eq(StudyTodo::getUserId, userId)
                .eq(StudyTodo::getStatus, 0)
                .and(w -> w.isNull(StudyTodo::getDueDate).or().le(StudyTodo::getDueDate, today));
        q.orderByAsc(StudyTodo::getDueDate).orderByDesc(StudyTodo::getPriority);
        return todoMapper.selectList(q);
    }

    public StudyTodo getByIdAndUser(Long id, Long userId) {
        StudyTodo t = todoMapper.selectById(id);
        if (t == null || !userId.equals(t.getUserId())) {
            return null;
        }
        return t;
    }

    public StudyTodo create(
            Long userId,
            String title,
            String content,
            Integer status,
            LocalDate dueDate,
            Integer priority) {
        StudyTodo t = new StudyTodo();
        t.setUserId(userId);
        t.setTitle(StringUtils.hasText(title) ? title.trim() : "未命名待办");
        t.setContent(content != null ? content : "");
        t.setStatus(status != null && (status == 0 || status == 1) ? status : 0);
        t.setDueDate(dueDate);
        t.setPriority(priority != null ? priority : 0);
        todoMapper.insert(t);
        return t;
    }

    public StudyTodo update(
            Long id,
            Long userId,
            String title,
            String content,
            Integer status,
            LocalDate dueDate,
            Integer priority,
            Boolean clearDueDate) {
        StudyTodo t = getByIdAndUser(id, userId);
        if (t == null) {
            return null;
        }
        if (title != null) {
            t.setTitle(StringUtils.hasText(title) ? title.trim() : "未命名待办");
        }
        if (content != null) {
            t.setContent(content);
        }
        if (status != null && (status == 0 || status == 1)) {
            t.setStatus(status);
        }
        if (Boolean.TRUE.equals(clearDueDate)) {
            t.setDueDate(null);
        } else if (dueDate != null) {
            t.setDueDate(dueDate);
        }
        if (priority != null) {
            t.setPriority(priority);
        }
        todoMapper.updateById(t);
        return todoMapper.selectById(id);
    }

    public boolean delete(Long id, Long userId) {
        if (getByIdAndUser(id, userId) == null) {
            return false;
        }
        return todoMapper.deleteById(id) > 0;
    }
}
