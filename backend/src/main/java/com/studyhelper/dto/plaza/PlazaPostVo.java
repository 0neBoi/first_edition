package com.studyhelper.dto.plaza;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlazaPostVo {
    private Long id;
    private Long userId;
    private String authorNickname;
    private String authorAvatar;
    private String category;
    private String title;
    private String content;
    private List<String> images;
    private Long priceCent;
    private String priceDisplay;
    private String tradeStatus;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer shareCount;
    private Boolean liked;
    private Boolean favorited;
    private LocalDateTime createTime;
}
