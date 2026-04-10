package com.studyhelper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyhelper.common.Result;
import com.studyhelper.dto.plaza.*;
import com.studyhelper.service.PlazaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plaza")
@RequiredArgsConstructor
public class PlazaController {

    private final PlazaService plazaService;

    @PostMapping("/post")
    public Result<PlazaPostVo> createPost(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> body) {
        try {
            return Result.ok(plazaService.createPost(userId, body));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/post/page")
    public Result<Page<PlazaPostVo>> pagePosts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category) {
        return Result.ok(plazaService.pagePosts(userId, category, page, size));
    }

    @GetMapping("/post/{id}")
    public Result<PlazaPostVo> getPost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        try {
            return Result.ok(plazaService.getPost(id, userId));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/post/{id}")
    public Result<Void> deletePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        try {
            plazaService.deletePost(id, userId);
            return Result.ok(null);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/post/{id}/like")
    public Result<Map<String, Object>> toggleLike(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        try {
            boolean on = plazaService.toggleLike(id, userId);
            return Result.ok(Map.of("liked", on));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/post/{id}/favorite")
    public Result<Map<String, Object>> toggleFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        try {
            boolean on = plazaService.toggleFavorite(id, userId);
            return Result.ok(Map.of("favorited", on));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/post/{id}/share")
    public Result<Void> share(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        try {
            plazaService.sharePost(id);
            return Result.ok(null);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/post/{id}/trade")
    public Result<PlazaPostVo> updateTrade(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("tradeStatus");
            return Result.ok(plazaService.updateTradeStatus(id, userId, status));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/post/{id}/comments")
    public Result<List<PlazaCommentVo>> comments(@PathVariable Long id) {
        try {
            return Result.ok(plazaService.listComments(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/post/{id}/comment")
    public Result<PlazaCommentVo> addComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            return Result.ok(plazaService.addComment(id, userId, body));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        try {
            plazaService.deleteComment(id, userId);
            return Result.ok(null);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public Result<String> upload(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (userId != null && userId.equals(0L)) {
            return Result.fail("管理员不支持上传");
        }
        try {
            return Result.ok(plazaService.uploadPlazaImage(file));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/message/page")
    public Result<Page<PlazaMessageVo>> messages(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long withUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            return Result.ok(plazaService.listMessages(userId, withUserId, page, size));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/message")
    public Result<PlazaMessageVo> sendMessage(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> body) {
        try {
            return Result.ok(plazaService.sendMessage(userId, body));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/chat/partners")
    public Result<List<PlazaChatPartnerVo>> partners(@AuthenticationPrincipal Long userId) {
        try {
            return Result.ok(plazaService.chatPartners(userId));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
