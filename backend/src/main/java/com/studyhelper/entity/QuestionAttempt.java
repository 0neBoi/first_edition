package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_attempt")
public class QuestionAttempt {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    /** 1 正确 0 错误 */
    private Integer isCorrect;
    private String userAnswer;
    private Integer timeCostMs;
    private LocalDateTime attemptTime;
    @TableLogic
    private Integer deleted;
}
