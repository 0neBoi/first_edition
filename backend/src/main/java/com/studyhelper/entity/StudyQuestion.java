package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 根据资料/知识点生成的题目
 */
@Data
@TableName("study_question")
public class StudyQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private Long userId;
    /** single=单选 multiple=多选 fill=填空 essay=简答 */
    private String type;
    private String questionText;
    /** 选择题选项 JSON */
    private String optionsJson;
    private String answer;
    private String analysis;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
