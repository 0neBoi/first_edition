package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyhelper.dto.LearningDashboardDto;
import com.studyhelper.entity.QuestionAttempt;
import com.studyhelper.entity.StudyClock;
import com.studyhelper.entity.StudyMaterial;
import com.studyhelper.entity.StudyQuestion;
import com.studyhelper.entity.StudyTodo;
import com.studyhelper.mapper.QuestionAttemptMapper;
import com.studyhelper.mapper.StudyClockMapper;
import com.studyhelper.mapper.StudyMaterialMapper;
import com.studyhelper.mapper.StudyQuestionMapper;
import com.studyhelper.mapper.StudyTodoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningAnalyticsService {

    private final StudyClockMapper clockMapper;
    private final StudyTodoMapper todoMapper;
    private final QuestionAttemptMapper attemptMapper;
    private final StudyQuestionMapper questionMapper;
    private final StudyMaterialMapper materialMapper;
    private final PracticeService practiceService;

    public LearningDashboardDto dashboard(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDateTime weekStartDt = weekStart.atStartOfDay();

        LambdaQueryWrapper<StudyClock> cq = new LambdaQueryWrapper<>();
        cq.eq(StudyClock::getUserId, userId)
                .ge(StudyClock::getClockDate, weekStart)
                .le(StudyClock::getClockDate, today);
        int weekMinutes = clockMapper.selectList(cq).stream()
                .mapToInt(c -> c.getMinutes() != null ? c.getMinutes() : 0)
                .sum();

        LambdaQueryWrapper<StudyTodo> tq = new LambdaQueryWrapper<>();
        tq.eq(StudyTodo::getUserId, userId)
                .eq(StudyTodo::getStatus, 1)
                .ge(StudyTodo::getUpdateTime, weekStartDt);
        long doneCount = todoMapper.selectCount(tq);
        int weekTodosDone = (int) Math.min(doneCount, Integer.MAX_VALUE);

        LambdaQueryWrapper<QuestionAttempt> aq = new LambdaQueryWrapper<>();
        aq.eq(QuestionAttempt::getUserId, userId);
        List<QuestionAttempt> attempts = attemptMapper.selectList(aq);
        long total = attempts.size();
        long correct = attempts.stream().filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1).count();
        double acc = total == 0 ? 0.0 : Math.round(1000.0 * correct / total) / 10.0;

        List<Map<String, Object>> weak = weakMaterials(userId);

        LearningDashboardDto dto = new LearningDashboardDto();
        dto.setWeekStudyMinutes(weekMinutes);
        dto.setWeekTodosDone(weekTodosDone);
        dto.setPracticeTotalAttempts(total);
        dto.setPracticeCorrectAttempts(correct);
        dto.setPracticeAccuracy(acc);
        dto.setWeakMaterials(weak);
        dto.setReviewQueueSize(practiceService.reviewToday(userId, 30).size());
        return dto;
    }

    private List<Map<String, Object>> weakMaterials(Long userId) {
        LambdaQueryWrapper<QuestionAttempt> aq = new LambdaQueryWrapper<>();
        aq.eq(QuestionAttempt::getUserId, userId);
        List<QuestionAttempt> attempts = attemptMapper.selectList(aq);
        Map<Long, int[]> byMaterial = new HashMap<>();
        for (QuestionAttempt a : attempts) {
            StudyQuestion q = questionMapper.selectById(a.getQuestionId());
            if (q == null) {
                continue;
            }
            StudyMaterial m = materialMapper.selectById(q.getMaterialId());
            if (m == null || !userId.equals(m.getUserId())) {
                continue;
            }
            long mid = m.getId();
            int[] arr = byMaterial.computeIfAbsent(mid, k -> new int[]{0, 0});
            arr[1]++;
            if (a.getIsCorrect() != null && a.getIsCorrect() == 0) {
                arr[0]++;
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, int[]> e : byMaterial.entrySet()) {
            StudyMaterial m = materialMapper.selectById(e.getKey());
            if (m == null) {
                continue;
            }
            int wrong = e.getValue()[0];
            int tot = e.getValue()[1];
            if (tot == 0) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("materialId", m.getId());
            row.put("title", m.getTitle() != null ? m.getTitle() : m.getFileName());
            row.put("wrongAttempts", wrong);
            row.put("totalAttempts", tot);
            row.put("wrongRate", Math.round(1000.0 * wrong / tot) / 10.0);
            rows.add(row);
        }
        rows.sort(Comparator.comparingDouble((Map<String, Object> r) -> ((Number) r.get("wrongRate")).doubleValue()).reversed());
        return rows.stream().limit(8).collect(Collectors.toList());
    }
}
