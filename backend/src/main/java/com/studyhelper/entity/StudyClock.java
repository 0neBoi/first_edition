package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习打卡记录
 */
@Data
@TableName("study_clock")
public class StudyClock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate clockDate;
    private Integer minutes;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
