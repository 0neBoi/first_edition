package com.studyhelper.dto.plaza;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlazaMessageVo {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String imagePath;
    private Boolean readFlag;
    private Boolean mine;
    private LocalDateTime createTime;
}
