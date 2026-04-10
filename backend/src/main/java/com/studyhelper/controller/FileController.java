package com.studyhelper.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 提供头像等静态文件访问（如 /files/avatar/xxx.jpg）
 */
@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @GetMapping("/plaza/{filename}")
    public ResponseEntity<Resource> getPlazaImage(@PathVariable String filename) {
        if (filename == null || filename.contains("..")) {
            return ResponseEntity.notFound().build();
        }
        Path base = Paths.get(uploadDir).resolve("plaza").toAbsolutePath().normalize();
        Path path = base.resolve(filename).normalize();
        if (!path.startsWith(base)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new PathResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        String contentType = getContentType(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
    }

    @GetMapping("/avatar/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        if (filename == null || filename.contains("..")) {
            return ResponseEntity.notFound().build();
        }
        Path base = Paths.get(uploadDir).resolve("avatar").toAbsolutePath().normalize();
        Path path = base.resolve(filename).normalize();
        if (!path.startsWith(base)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new PathResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        String contentType = getContentType(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
    }

    private static String getContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
