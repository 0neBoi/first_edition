package com.studyhelper.dto.plaza;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlazaChatPartnerVo {
    private Long userId;
    private String nickname;
    private String avatar;
    private String lastPreview;
    private LocalDateTime lastTime;
    private Long unreadCount;
}
