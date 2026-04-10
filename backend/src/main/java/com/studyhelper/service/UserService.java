package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyhelper.entity.User;
import com.studyhelper.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    public User getByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, username);
        return userMapper.selectOne(q);
    }

    public User register(String username, String password, String nickname) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if ("root".equalsIgnoreCase(username.trim())) {
            throw new IllegalArgumentException("该用户名为系统保留");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (getByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname.trim() : username.trim());
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    public User login(String username, String password) {
        User user = getByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 更新个人资料（昵称、头像、手机、学校、专业），仅更新非空字段
     */
    public User updateProfile(Long userId, String nickname, String avatar, String phone, String school, String major) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname.trim());
        }
        if (avatar != null) {
            user.setAvatar(avatar.isBlank() ? null : avatar.trim());
        }
        if (phone != null) {
            user.setPhone(phone.isBlank() ? null : phone.trim());
        }
        if (school != null) {
            user.setSchool(school.isBlank() ? null : school.trim());
        }
        if (major != null) {
            user.setMajor(major.isBlank() ? null : major.trim());
        }
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    /**
     * 上传头像，返回相对路径（如 avatar/xxx.jpg），供前端拼接为完整 URL
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("请选择图片");
        }
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".jpg";
        if (!Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("仅支持 jpg/png/gif/webp 图片");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("头像大小不能超过 2MB");
        }
        Path dir = Paths.get(uploadDir, "avatar").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(storedName);
        file.transferTo(target.toFile());
        return "avatar/" + storedName;
    }

    /**
     * 修改密码：需验证旧密码
     */
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    /** 管理员分页查询注册用户（不含密码） */
    public Page<User> pageUsersForAdmin(int pageNum, int pageSize) {
        Page<User> p = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.orderByDesc(User::getCreateTime);
        userMapper.selectPage(p, q);
        p.getRecords().forEach(u -> {
            u.setPassword(null);
            u.setRole("STUDENT");
        });
        return p;
    }

    /** 管理员重置用户登录密码 */
    public void adminResetPassword(Long targetUserId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    /** 管理员禁用账号（逻辑删除） */
    public void adminDeleteUser(Long targetUserId) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在或已删除");
        }
        userMapper.deleteById(targetUserId);
    }
}
