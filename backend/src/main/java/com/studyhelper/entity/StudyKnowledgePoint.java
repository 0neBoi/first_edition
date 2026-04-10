package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 从资料中提取的知识要点
 */
@Data
@TableName("study_knowledge_point")
public class StudyKnowledgePoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private Long userId;
    private String title;
    private String content;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
