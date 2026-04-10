package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.entity.CampusAnnouncement;
import com.studyhelper.service.CampusAnnouncementService;
import com.studyhelper.util.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/campus-announcement")
@RequiredArgsConstructor
public class CampusAnnouncementController {

    private final CampusAnnouncementService announcementService;
    private final JwtUtil jwtUtil;

    @GetMapping("/public/list")
    public Result<List<CampusAnnouncement>> publicList() {
        return Result.ok(announcementService.listPublic());
    }

    @PostMapping
    public Result<CampusAnnouncement> create(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody AnnouncementBody body) {
        if (!isAdmin(auth)) {
            return Result.fail(403, "需要管理员权限");
        }
        try {
            return Result.ok(announcementService.create(
                    body.getTitle(),
                    body.getContent(),
                    body.getNoticeType(),
                    body.getPublisher(),
                    body.getPinned(),
                    body.getPublishDate()));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<CampusAnnouncement> update(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody AnnouncementBody body) {
        if (!isAdmin(auth)) {
            return Result.fail(403, "需要管理员权限");
        }
        try {
            return Result.ok(announcementService.update(
                    id,
                    body.getTitle(),
                    body.getContent(),
                    body.getNoticeType(),
                    body.getPublisher(),
                    body.getPinned(),
                    body.getPublishDate()));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        if (!isAdmin(auth)) {
            return Result.fail(403, "需要管理员权限");
        }
        try {
            announcementService.delete(id);
            return Result.ok(null);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    private boolean isAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return false;
        }
        return "ADMIN".equals(jwtUtil.getRoleFromToken(token));
    }

    @Data
    public static class AnnouncementBody {
        private String title;
        private String content;
        private String noticeType;
        private String publisher;
        private Integer pinned;
        private LocalDate publishDate;
    }
}
