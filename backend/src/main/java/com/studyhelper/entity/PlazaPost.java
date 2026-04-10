package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plaza_post")
public class PlazaPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** DISCUSSION / MARKETPLACE */
    private String category;
    private String title;
    private String content;
    private String imagesJson;
    private Long priceCent;
    /** NA ON_SALE SOLD RESERVED */
    private String tradeStatus;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer shareCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
