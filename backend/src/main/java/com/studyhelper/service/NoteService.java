package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyhelper.entity.StudyNote;
import com.studyhelper.mapper.StudyNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final StudyNoteMapper noteMapper;

    public List<StudyNote> list(Long userId, String tag, String keyword) {
        LambdaQueryWrapper<StudyNote> q = new LambdaQueryWrapper<>();
        q.eq(StudyNote::getUserId, userId);
        if (StringUtils.hasText(tag)) {
            q.and(w -> w.like(StudyNote::getTags, tag.trim()));
        }
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            q.and(w -> w.like(StudyNote::getTitle, k).or().like(StudyNote::getContent, k));
        }
        q.orderByDesc(StudyNote::getUpdateTime);
        return noteMapper.selectList(q);
    }

    public StudyNote getByIdAndUser(Long id, Long userId) {
        StudyNote n = noteMapper.selectById(id);
        if (n == null || !userId.equals(n.getUserId())) {
            return null;
        }
        return n;
    }

    public StudyNote create(Long userId, String title, String content, String tags) {
        StudyNote n = new StudyNote();
        n.setUserId(userId);
        n.setTitle(StringUtils.hasText(title) ? title.trim() : "未命名笔记");
        n.setContent(content != null ? content : "");
        n.setTags(tags != null ? tags.trim() : null);
        noteMapper.insert(n);
        return n;
    }

    public StudyNote update(Long id, Long userId, String title, String content, String tags) {
        StudyNote n = getByIdAndUser(id, userId);
        if (n == null) {
            return null;
        }
        if (title != null) {
            n.setTitle(StringUtils.hasText(title) ? title.trim() : "未命名笔记");
        }
        if (content != null) {
            n.setContent(content);
        }
        if (tags != null) {
            n.setTags(StringUtils.hasText(tags) ? tags.trim() : null);
        }
        noteMapper.updateById(n);
        return noteMapper.selectById(id);
    }

    public boolean delete(Long id, Long userId) {
        StudyNote n = getByIdAndUser(id, userId);
        if (n == null) {
            return false;
        }
        return noteMapper.deleteById(id) > 0;
    }
}
