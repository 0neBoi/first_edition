package com.studyhelper.dto;

import lombok.Data;

@Data
public class PracticeSubmitResult {
    private Long questionId;
    private boolean correct;
    private String standardAnswer;
    private String analysis;
    private Integer masteryScore;
}
