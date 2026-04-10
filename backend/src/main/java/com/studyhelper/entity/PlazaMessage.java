package com.studyhelper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plaza_message")
public class PlazaMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String imagePath;
    private Boolean readFlag;
    private LocalDateTime createTime;
}
