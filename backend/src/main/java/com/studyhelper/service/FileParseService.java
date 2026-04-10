package com.studyhelper.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 从上传文件中解析出纯文本（TXT / PDF / DOCX）
 */
@Service
public class FileParseService {

    /**
     * 根据文件名后缀解析文件内容为纯文本
     */
    public String parseToText(String fileName, InputStream input) throws IOException {
        if (fileName == null || input == null) return "";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".txt")) return parseTxt(input);
        if (lower.endsWith(".pdf")) return parsePdf(input);
        if (lower.endsWith(".docx")) return parseDocx(input);
        return "";
    }

    public String parseTxt(InputStream input) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }

    public String parsePdf(InputStream input) throws IOException {
        try (PDDocument doc = Loader.loadPDF(input.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    public String parseDocx(InputStream input) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(input)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    public boolean supported(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".pdf") || lower.endsWith(".docx");
    }
}
