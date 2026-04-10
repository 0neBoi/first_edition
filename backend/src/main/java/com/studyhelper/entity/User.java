package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表（学生）
 */
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    /** 微信 openid，小程序登录用 */
    private String openid;
    private String phone;
    private String school;
    private String major;
    /** 仅接口返回用：ADMIN 虚拟账号或业务角色，非表字段 */
    @TableField(exist = false)
    private String role;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
