package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导入的学习资料（上传文件元信息 + 解析出的文本）
 */
@Data
@TableName("study_material")
public class StudyMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    /** 解析出的纯文本，供 AI 提取知识点与出题 */
    private String contentText;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
