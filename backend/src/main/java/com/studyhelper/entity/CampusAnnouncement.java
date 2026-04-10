package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("campus_announcement")
public class CampusAnnouncement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String noticeType;
    private String publisher;
    private Integer pinned;
    private LocalDate publishDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
