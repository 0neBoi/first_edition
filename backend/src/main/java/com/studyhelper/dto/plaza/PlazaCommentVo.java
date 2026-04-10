package com.studyhelper.dto.plaza;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlazaCommentVo {
    private Long id;
    private Long postId;
    private Long userId;
    private String authorNickname;
    private String authorAvatar;
    private Long parentId;
    private Long replyToUserId;
    private String replyToNickname;
    private String content;
    private LocalDateTime createTime;
    private List<PlazaCommentVo> children;
}
