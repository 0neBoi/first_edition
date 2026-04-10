package com.studyhelper.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LearningDashboardDto {
    private int weekStudyMinutes;
    private int weekTodosDone;
    private long practiceTotalAttempts;
    private long practiceCorrectAttempts;
    private double practiceAccuracy;
    private List<Map<String, Object>> weakMaterials;
    private int reviewQueueSize;
}
