package com.studyhelper.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionDto {
    private String type;
    private String questionText;
    private List<OptionItem> options;  // 选择题时有值
    private String answer;
    private String analysis;
}
