package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyhelper.dto.KnowledgePointDto;
import com.studyhelper.entity.StudyKnowledgePoint;
import com.studyhelper.entity.StudyMaterial;
import com.studyhelper.mapper.StudyKnowledgePointMapper;
import com.studyhelper.mapper.StudyMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgePointService {

    private final StudyKnowledgePointMapper knowledgePointMapper;
    private final StudyMaterialMapper materialMapper;
    private final QwenService qwenService;

    public List<StudyKnowledgePoint> listByMaterialId(Long materialId) {
        LambdaQueryWrapper<StudyKnowledgePoint> q = new LambdaQueryWrapper<>();
        q.eq(StudyKnowledgePoint::getMaterialId, materialId)
                .orderByAsc(StudyKnowledgePoint::getSortOrder)
                .orderByAsc(StudyKnowledgePoint::getId);
        return knowledgePointMapper.selectList(q);
    }

    /**
     * 使用 AI 从资料中提取知识要点并入库
     */
    @Transactional(rollbackFor = Exception.class)
    public List<StudyKnowledgePoint> extractAndSave(Long materialId, Long userId) {
        StudyMaterial material = materialMapper.selectById(materialId);
        if (material == null || material.getContentText() == null || material.getContentText().isBlank()) {
            throw new IllegalArgumentException("资料不存在或没有可解析的文本内容");
        }
        List<KnowledgePointDto> dtos = qwenService.extractKnowledgePoints(material.getContentText());
        LambdaQueryWrapper<StudyKnowledgePoint> del = new LambdaQueryWrapper<>();
        del.eq(StudyKnowledgePoint::getMaterialId, materialId);
        knowledgePointMapper.delete(del);

        List<StudyKnowledgePoint> entities = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            KnowledgePointDto dto = dtos.get(i);
            StudyKnowledgePoint point = new StudyKnowledgePoint();
            point.setMaterialId(materialId);
            point.setUserId(userId != null ? userId : material.getUserId());
            point.setTitle(dto.getTitle());
            point.setContent(dto.getContent());
            point.setSortOrder(i);
            knowledgePointMapper.insert(point);
            entities.add(point);
        }
        return entities;
    }

    public boolean deleteById(Long id) {
        return knowledgePointMapper.deleteById(id) > 0;
    }

    public boolean deleteByIdAndUser(Long id, Long userId) {
        StudyKnowledgePoint point = knowledgePointMapper.selectById(id);
        if (point == null) return false;
        StudyMaterial material = materialMapper.selectById(point.getMaterialId());
        if (material == null || (userId != null && !userId.equals(material.getUserId()))) {
            return false;
        }
        return knowledgePointMapper.deleteById(id) > 0;
    }
}
