package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 飞机大战：每名用户保留一条最佳分
 */
@Data
@TableName("plane_best")
public class PlaneBest {
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private Integer score;
    private LocalDateTime updateTime;
}
