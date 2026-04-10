package com.studyhelper.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DinoLeaderboardItem {
    private int rank;
    private Long userId;
    /** 展示用：昵称优先，否则用户名 */
    private String displayName;
    private Integer score;
    private LocalDateTime updateTime;
}
