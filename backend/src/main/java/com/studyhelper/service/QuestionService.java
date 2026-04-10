package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyhelper.dto.QuestionDto;
import com.studyhelper.entity.StudyMaterial;
import com.studyhelper.entity.StudyQuestion;
import com.studyhelper.mapper.StudyMaterialMapper;
import com.studyhelper.mapper.StudyQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final StudyQuestionMapper questionMapper;
    private final StudyMaterialMapper materialMapper;
    private final QwenService qwenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 与数据库 answer 列长度一致；若已执行 docs/sql/alter-question-answer.sql 改为 TEXT，可改为 4000 */
    private static final int MAX_ANSWER_LENGTH = 500;
    private static final int MAX_ANALYSIS_LENGTH = 2000;

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }

    public List<StudyQuestion> listByMaterialId(Long materialId) {
        LambdaQueryWrapper<StudyQuestion> q = new LambdaQueryWrapper<>();
        q.eq(StudyQuestion::getMaterialId, materialId)
                .orderByAsc(StudyQuestion::getSortOrder)
                .orderByAsc(StudyQuestion::getId);
        return questionMapper.selectList(q);
    }

    /**
     * 使用 AI 根据资料生成题目并入库。
     *
     * @param replace true 时先删除本资料全部题目再写入；false 时在末尾追加（避免多题型并行请求互相覆盖）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<StudyQuestion> generateAndSave(Long materialId, Long userId, int count, boolean replace, String typesCsv) {
        StudyMaterial material = materialMapper.selectById(materialId);
        if (material == null || material.getContentText() == null || material.getContentText().isBlank()) {
            throw new IllegalArgumentException("资料不存在或没有可解析的文本内容");
        }
        List<String> typeHints = parseTypeHints(typesCsv);
        List<QuestionDto> dtos = qwenService.generateQuestions(material.getContentText(), count, typeHints);

        int baseOrder = 0;
        if (replace) {
            LambdaQueryWrapper<StudyQuestion> del = new LambdaQueryWrapper<>();
            del.eq(StudyQuestion::getMaterialId, materialId);
            questionMapper.delete(del);
        } else {
            LambdaQueryWrapper<StudyQuestion> mq = new LambdaQueryWrapper<>();
            mq.eq(StudyQuestion::getMaterialId, materialId)
                    .orderByDesc(StudyQuestion::getSortOrder)
                    .last("LIMIT 1");
            StudyQuestion last = questionMapper.selectOne(mq);
            if (last != null && last.getSortOrder() != null) {
                baseOrder = last.getSortOrder() + 1;
            }
        }

        List<StudyQuestion> entities = new ArrayList<>();
        int pos = 0;
        for (QuestionDto dto : dtos) {
            if (dto.getQuestionText() == null || dto.getQuestionText().isBlank()) {
                continue;
            }
            StudyQuestion q = new StudyQuestion();
            q.setMaterialId(materialId);
            q.setUserId(userId != null ? userId : material.getUserId());
            String t = dto.getType() != null ? dto.getType() : "essay";
            if (typeHints.size() == 1) {
                t = typeHints.get(0);
            }
            q.setType(t);
            q.setQuestionText(dto.getQuestionText());
            q.setAnswer(truncate(dto.getAnswer(), MAX_ANSWER_LENGTH));
            q.setAnalysis(truncate(dto.getAnalysis(), MAX_ANALYSIS_LENGTH));
            q.setSortOrder(baseOrder + pos);
            pos++;
            if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
                try {
                    q.setOptionsJson(objectMapper.writeValueAsString(dto.getOptions()));
                } catch (JsonProcessingException e) {
                    q.setOptionsJson(null);
                }
            }
            questionMapper.insert(q);
            entities.add(q);
        }
        return entities;
    }

    private static List<String> parseTypeHints(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public boolean deleteById(Long id) {
        return questionMapper.deleteById(id) > 0;
    }

    public boolean deleteByIdAndUser(Long id, Long userId) {
        StudyQuestion question = questionMapper.selectById(id);
        if (question == null) return false;
        StudyMaterial material = materialMapper.selectById(question.getMaterialId());
        if (material == null || (userId != null && !userId.equals(material.getUserId()))) {
            return false;
        }
        return questionMapper.deleteById(id) > 0;
    }
}
