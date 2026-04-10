package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyhelper.entity.CampusAnnouncement;
import com.studyhelper.mapper.CampusAnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampusAnnouncementService {

    private final CampusAnnouncementMapper mapper;

    public List<CampusAnnouncement> listPublic() {
        LambdaQueryWrapper<CampusAnnouncement> q = new LambdaQueryWrapper<>();
        q.orderByDesc(CampusAnnouncement::getPinned)
                .orderByDesc(CampusAnnouncement::getPublishDate)
                .orderByDesc(CampusAnnouncement::getId);
        return mapper.selectList(q);
    }

    public List<CampusAnnouncement> listAll() {
        return listPublic();
    }

    public CampusAnnouncement getById(Long id) {
        return mapper.selectById(id);
    }

    public CampusAnnouncement create(String title, String content, String noticeType,
                                     String publisher, Integer pinned, LocalDate publishDate) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }
        CampusAnnouncement a = new CampusAnnouncement();
        a.setTitle(title.trim());
        a.setContent(content);
        a.setNoticeType(noticeType != null && !noticeType.isBlank() ? noticeType.trim() : "teaching");
        a.setPublisher(publisher != null && !publisher.isBlank() ? publisher.trim() : "教务处");
        a.setPinned(pinned != null && pinned != 0 ? 1 : 0);
        a.setPublishDate(publishDate != null ? publishDate : LocalDate.now());
        mapper.insert(a);
        return a;
    }

    public CampusAnnouncement update(Long id, String title, String content, String noticeType,
                                     String publisher, Integer pinned, LocalDate publishDate) {
        CampusAnnouncement a = mapper.selectById(id);
        if (a == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        if (title != null && !title.isBlank()) {
            a.setTitle(title.trim());
        }
        if (content != null && !content.isBlank()) {
            a.setContent(content);
        }
        if (noticeType != null && !noticeType.isBlank()) {
            a.setNoticeType(noticeType.trim());
        }
        if (publisher != null && !publisher.isBlank()) {
            a.setPublisher(publisher.trim());
        }
        if (pinned != null) {
            a.setPinned(pinned != 0 ? 1 : 0);
        }
        if (publishDate != null) {
            a.setPublishDate(publishDate);
        }
        mapper.updateById(a);
        return mapper.selectById(id);
    }

    public void delete(Long id) {
        CampusAnnouncement a = mapper.selectById(id);
        if (a == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        mapper.deleteById(id);
    }
}
