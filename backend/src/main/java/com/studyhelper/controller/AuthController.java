package com.studyhelper.controller;

import com.studyhelper.common.Result;
import com.studyhelper.dto.LoginResult;
import com.studyhelper.entity.User;
import com.studyhelper.service.UserService;
import com.studyhelper.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final long ADMIN_USER_ID = 0L;

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Value("${admin.username:root}")
    private String adminUsername;

    @Value("${admin.password:123456}")
    private String adminPassword;

    @PostMapping("/register")
    public Result<LoginResult> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String nickname) {
        User user = userService.register(username, password, nickname);
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), "STUDENT");
        LoginResult res = new LoginResult();
        res.setToken(token);
        res.setUserId(user.getId());
        res.setUsername(user.getUsername());
        res.setNickname(user.getNickname());
        res.setAvatar(user.getAvatar());
        res.setRole("STUDENT");
        return Result.ok(res);
    }

    @PostMapping("/login")
    public Result<LoginResult> login(
            @RequestParam String username,
            @RequestParam String password) {
        if (username != null && adminUsername != null
                && username.trim().equalsIgnoreCase(adminUsername.trim())
                && adminPassword != null
                && adminPassword.equals(password)) {
            String token = jwtUtil.createToken(ADMIN_USER_ID, username.trim(), "ADMIN");
            LoginResult res = new LoginResult();
            res.setToken(token);
            res.setUserId(ADMIN_USER_ID);
            res.setUsername(username.trim());
            res.setNickname("系统管理员");
            res.setAvatar(null);
            res.setRole("ADMIN");
            return Result.ok(res);
        }
        User user = userService.login(username, password);
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), "STUDENT");
        LoginResult res = new LoginResult();
        res.setToken(token);
        res.setUserId(user.getId());
        res.setUsername(user.getUsername());
        res.setNickname(user.getNickname());
        res.setAvatar(user.getAvatar());
        res.setRole("STUDENT");
        return Result.ok(res);
    }

    @GetMapping("/me")
    public Result<User> me(@AuthenticationPrincipal Long userId) {
        if (userId != null && userId.equals(ADMIN_USER_ID)) {
            User u = new User();
            u.setId(ADMIN_USER_ID);
            u.setUsername(adminUsername);
            u.setNickname("系统管理员");
            u.setRole("ADMIN");
            return Result.ok(u);
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setPassword(null);
        user.setRole("STUDENT");
        return Result.ok(user);
    }

    /**
     * 更新个人资料（昵称、头像、手机、学校、专业）
     */
    @PutMapping("/profile")
    public Result<User> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        if (userId != null && userId.equals(ADMIN_USER_ID)) {
            return Result.fail("管理员资料为系统固定，无需修改");
        }
        try {
            User user = userService.updateProfile(
                    userId,
                    body.get("nickname"),
                    body.get("avatar"),
                    body.get("phone"),
                    body.get("school"),
                    body.get("major"));
            user.setRole("STUDENT");
            return Result.ok(user);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        if (userId != null && userId.equals(ADMIN_USER_ID)) {
            return Result.fail("管理员密码请在服务器配置中通过 ADMIN_PASSWORD 环境变量修改");
        }
        try {
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            if (oldPassword == null || newPassword == null) {
                return Result.fail("请填写原密码和新密码");
            }
            userService.updatePassword(userId, oldPassword, newPassword);
            return Result.ok(null);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 上传头像，返回相对路径（如 avatar/xxx.jpg）
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {
        if (userId != null && userId.equals(ADMIN_USER_ID)) {
            return Result.fail("管理员不使用头像上传");
        }
        try {
            String path = userService.uploadAvatar(file);
            return Result.ok(path);
        } catch (IOException e) {
            return Result.fail("上传失败：" + e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }
}
