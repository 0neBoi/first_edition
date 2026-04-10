package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小恐龙跑酷：每名用户保留一条最佳分（用于排行榜）
 */
@Data
@TableName("dino_best")
public class DinoBest {
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private Integer score;
    private LocalDateTime updateTime;
}
