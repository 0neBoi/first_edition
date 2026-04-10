package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyhelper.entity.StudyMaterial;
import com.studyhelper.mapper.StudyMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final StudyMaterialMapper materialMapper;
    private final FileParseService fileParseService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Transactional(rollbackFor = Exception.class)
    public StudyMaterial upload(Long userId, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (!fileParseService.supported(originalName)) {
            throw new IllegalArgumentException("仅支持 .txt / .pdf / .docx 格式");
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(storedName);
        file.transferTo(target.toFile());

        String contentText;
        try (var is = Files.newInputStream(target)) {
            contentText = fileParseService.parseToText(originalName, is);
        }

        StudyMaterial material = new StudyMaterial();
        material.setUserId(userId);
        material.setTitle(originalName);
        material.setFileName(originalName);
        material.setFilePath(storedName);
        material.setFileType(ext.replace(".", ""));
        material.setFileSize(file.getSize());
        material.setContentText(contentText != null ? contentText : "");
        materialMapper.insert(material);
        return material;
    }

    public StudyMaterial getById(Long id) {
        return materialMapper.selectById(id);
    }

    public StudyMaterial getByIdAndUser(Long id, Long userId) {
        StudyMaterial m = materialMapper.selectById(id);
        if (m == null || (userId != null && !userId.equals(m.getUserId()))) {
            return null;
        }
        return m;
    }

    public List<StudyMaterial> list(Long userId) {
        LambdaQueryWrapper<StudyMaterial> q = new LambdaQueryWrapper<>();
        if (userId != null) {
            q.eq(StudyMaterial::getUserId, userId);
        }
        q.orderByDesc(StudyMaterial::getCreateTime);
        return materialMapper.selectList(q);
    }

    public boolean deleteById(Long id) {
        return materialMapper.deleteById(id) > 0;
    }

    public boolean deleteByIdAndUser(Long id, Long userId) {
        StudyMaterial m = materialMapper.selectById(id);
        if (m == null || (userId != null && !userId.equals(m.getUserId()))) {
            return false;
        }
        return materialMapper.deleteById(id) > 0;
    }
}
