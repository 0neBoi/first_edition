package com.studyhelper.dto;

import lombok.Data;

@Data
public class LoginResult {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    /** STUDENT / ADMIN */
    private String role;
}
