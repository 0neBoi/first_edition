package com.studyhelper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyhelper.common.Result;
import com.studyhelper.entity.User;
import com.studyhelper.service.UserService;
import com.studyhelper.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员：查看与管理注册用户（不含虚拟 root 会话对应的库内记录）。
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final long ADMIN_USER_ID = 0L;

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<Page<User>> page(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!isAdmin(auth)) {
            return Result.fail(403, "需要管理员权限");
        }
        if (size > 100) {
            size = 100;
        }
        if (page < 1) {
            page = 1;
        }
        return Result.ok(userService.pageUsersForAdmin(page, size));
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (!isAdmin(auth)) {
            return Result.fail(403, "需要管理员权限");
        }
        if (id != null && id.equals(ADMIN_USER_ID)) {
            return Result.fail("无法修改虚拟管理员");
        }
        try {
            userService.adminResetPassword(id, body != null ? body.get("newPassword") : null);
            return Result.ok(null);
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
        if (id != null && id.equals(ADMIN_USER_ID)) {
            return Result.fail("无法删除虚拟管理员");
        }
        try {
            userService.adminDeleteUser(id);
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
}
