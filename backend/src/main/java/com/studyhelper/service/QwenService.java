package com.studyhelper.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.studyhelper.dto.KnowledgePointDto;
import com.studyhelper.dto.OptionItem;
import com.studyhelper.dto.QuestionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于阿里云百炼（通义千问）的 AI 服务：问答、知识要点提取、题目生成
 */
@Service
public class QwenService {

    private static final int MAX_CONTENT_LENGTH = 5000;

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.model:qwen-turbo}")
    private String model;

    private String chat(String systemPrompt, String userMessage) {
        String key = (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("你的") && !apiKey.contains("API-KEY"))
                ? apiKey
                : System.getenv("DASHSCOPE_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("请配置 dashscope.api-key（或在环境变量中设置 DASHSCOPE_API_KEY）");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("用户输入不能为空");
        }
        if (systemPrompt == null) systemPrompt = "";
        Constants.apiKey = key;
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemPrompt)
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userMessage)
                .build();
        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder()
                .apiKey(key)
                .model(model != null && !model.isBlank() ? model : "qwen-turbo")
                .messages(Arrays.asList(systemMsg, userMsg))
                .build();
        try {
            GenerationResult result = gen.call(param);
            if (result == null || result.getOutput() == null) {
                throw new RuntimeException("百炼 API 返回为空");
            }
            var output = result.getOutput();
            String fromChoices = null;
            if (output.getChoices() != null && !output.getChoices().isEmpty()) {
                var msg = output.getChoices().get(0).getMessage();
                if (msg != null && msg.getContent() != null && !msg.getContent().isBlank()) {
                    fromChoices = msg.getContent();
                }
            }
            if (fromChoices != null) {
                return fromChoices;
            }
            String fromText = output.getText();
            if (fromText != null && !fromText.isBlank()) {
                return fromText;
            }
            throw new RuntimeException("百炼 API 返回为空");
        } catch (NoApiKeyException e) {
            throw new IllegalStateException("请配置 dashscope.api-key（或在环境变量中设置 DASHSCOPE_API_KEY）", e);
        } catch (InputRequiredException e) {
            throw new IllegalArgumentException("请求参数不完整: " + e.getMessage(), e);
        }
    }

    public String ask(String question) {
        if (question == null || question.isBlank()) {
            return "请输入问题。";
        }
        String systemPrompt = "你是一名大学生学习助手，请用简洁、准确、友好的方式回答问题，帮助用户理解知识点。";
        return chat(systemPrompt, "用户问题：" + question);
    }

    /**
     * 从资料文本中提取知识要点
     */
    public List<KnowledgePointDto> extractKnowledgePoints(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String truncated = truncate(content, MAX_CONTENT_LENGTH);
        String systemPrompt = "你是一名大学生学习助手。请根据以下学习资料内容，整理出知识要点。"
                + "每条要点必须严格按两行输出：第一行是「标题：」加要点标题，第二行是「内容：」加要点内容。"
                + "不同要点之间用单独一行「---」分隔。只输出要点，不要其他解释。";
        String userMessage = "请整理以下内容的知识要点：\n\n" + truncated;
        String output = chat(systemPrompt, userMessage);
        return parseKnowledgePoints(output);
    }

    /**
     * 根据资料文本生成练习题（题型不限）
     */
    public List<QuestionDto> generateQuestions(String content, int count) {
        return generateQuestions(content, count, List.of());
    }

    /**
     * 根据资料生成练习题；restrictTypes 仅一项时在提示词中强制该题型，提高批量出题成功率。
     * 单次请求最多 30 道；若解析不足会自动再请求一轮补题。
     */
    public List<QuestionDto> generateQuestions(String content, int count, List<String> restrictTypes) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        if (count <= 0) {
            count = 3;
        }
        if (count > 30) {
            count = 30;
        }
        String truncated = truncate(content, MAX_CONTENT_LENGTH);
        String typeConstraint = buildTypeConstraint(restrictTypes, count);
        String formatRules = "每道题必须严格包含以下行（不得缺字段）：\n"
                + "【题型】单选 / 多选 / 填空 / 简答（四选一写清）\n"
                + "【题目】题干（一行或多行均可）\n"
                + "【选项】单选/多选必须写四项：A.xxx B.xxx C.xxx D.xxx；填空与简答写 无\n"
                + "【答案】\n"
                + "【解析】\n"
                + "每道题结束后单独起一行，只写三个等号 === ，再写下一题。共需完整输出 " + count + " 道题。";

        String systemPrompt = "你是一名大学生学习助教。" + typeConstraint
                + "请严格根据资料生成恰好 " + count + " 道互不重复、可独立作答的题目。\n"
                + formatRules
                + "禁止写开场白、目录或结尾总结；直接从第一道题的「【题型】」开始输出。";

        String userMessage = "请根据下列资料生成 " + count + " 道题：\n\n" + truncated;
        String output = chat(systemPrompt, userMessage);
        List<QuestionDto> list = new ArrayList<>(parseQuestions(output));
        dedupeAndTrim(list, count + 5);

        if (list.size() < count && count >= 2) {
            int need = Math.min(count - list.size(), 25);
            String retrySys = "你是一名大学生学习助教。上一输出题目数量不足。请仅再输出 " + need + " 道新题，"
                    + "格式与要求与此前完全一致；题与题之间单独一行 === 分隔；不要开场白，从【题型】开始。"
                    + typeConstraint
                    + formatRules.replace("共需完整输出 " + count + " 道题。", "本段共输出 " + need + " 道题。");
            String retryUser = "仍基于下列资料，再生成 " + need + " 道题：\n\n" + truncated;
            list.addAll(parseQuestions(chat(retrySys, retryUser)));
            dedupeAndTrim(list, count + 5);
        }

        if (list.size() > count) {
            return new ArrayList<>(list.subList(0, count));
        }
        return list;
    }

    private static String buildTypeConstraint(List<String> restrictTypes, int count) {
        if (restrictTypes == null || restrictTypes.isEmpty()) {
            return "题型可包含单选、多选、填空、简答，请合理搭配。\n";
        }
        if (restrictTypes.size() == 1) {
            String k = restrictTypes.get(0).trim().toLowerCase();
            return switch (k) {
                case "single" -> "【硬性要求】这 " + count + " 道题必须全部为单选题；每题必须有 A/B/C/D 四个选项，【选项】行写全四项。\n";
                case "multiple" -> "【硬性要求】这 " + count + " 道题必须全部为多选题；每题四个选项；【答案】可写多个大写字母如 AC。\n";
                case "fill" -> "【硬性要求】这 " + count + " 道题必须全部为填空题；【选项】写 无。\n";
                case "essay" -> "【硬性要求】这 " + count + " 道题必须全部为简答题；【选项】写 无。\n";
                default -> "";
            };
        }
        return "【题型范围】" + String.join("、", restrictTypes) + "。\n";
    }

    /** 去掉空题、按题干前缀粗略去重，并限制列表长度 */
    private static void dedupeAndTrim(List<QuestionDto> list, int maxKeep) {
        List<QuestionDto> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (QuestionDto d : list) {
            if (d.getQuestionText() == null || d.getQuestionText().isBlank()) {
                continue;
            }
            String norm = d.getQuestionText().replaceAll("\\s+", "");
            String key = norm.length() > 48 ? norm.substring(0, 48) : norm;
            if (seen.add(key)) {
                out.add(d);
                if (out.size() >= maxKeep) {
                    break;
                }
            }
        }
        list.clear();
        list.addAll(out);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        s = s.strip();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "\n...(内容已截断)";
    }

    private static List<KnowledgePointDto> parseKnowledgePoints(String output) {
        List<KnowledgePointDto> list = new ArrayList<>();
        if (output == null || output.isBlank()) return list;
        String[] blocks = output.split("\n---\n");
        Pattern titlePat = Pattern.compile("标题[：:]\\s*(.+)");
        Pattern contentPat = Pattern.compile("内容[：:]\\s*(.+)", Pattern.DOTALL);
        for (String block : blocks) {
            block = block.strip();
            if (block.isEmpty()) continue;
            String[] lines = block.split("\n");
            String title = null;
            StringBuilder content = new StringBuilder();
            for (String line : lines) {
                Matcher tm = titlePat.matcher(line.trim());
                Matcher cm = contentPat.matcher(line.trim());
                if (tm.matches()) {
                    title = tm.group(1).strip();
                } else if (cm.matches()) {
                    content.append(cm.group(1).strip());
                } else if (title != null && !line.trim().startsWith("---")) {
                    if (content.length() > 0) content.append("\n");
                    content.append(line);
                }
            }
            if (title != null || content.length() > 0) {
                KnowledgePointDto dto = new KnowledgePointDto();
                dto.setTitle(title != null ? title : "要点");
                dto.setContent(content.toString().strip());
                list.add(dto);
            }
        }
        return list;
    }

    private static List<QuestionDto> parseQuestions(String output) {
        List<QuestionDto> list = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return list;
        }
        String norm = output.replace("\r\n", "\n").replace('\r', '\n').strip();
        String[] blocks = norm.split("(?m)^\\s*===\\s*$");
        if (blocks.length <= 1 && norm.contains("===")) {
            blocks = norm.split("\\n===\\n");
        }
        Pattern typePat = Pattern.compile("【题型】\\s*(.+)");
        Pattern questionPat = Pattern.compile("【题目】\\s*(.+)", Pattern.DOTALL);
        Pattern optionsPat = Pattern.compile("【选项】\\s*(.+)", Pattern.DOTALL);
        Pattern answerPat = Pattern.compile("【答案】\\s*(.+)", Pattern.DOTALL);
        Pattern analysisPat = Pattern.compile("【解析】\\s*(.+)", Pattern.DOTALL);
        for (String block : blocks) {
            block = block.strip();
            if (block.isEmpty() || !block.contains("【题目】")) {
                continue;
            }
            QuestionDto dto = new QuestionDto();
            String type = extractGroup(typePat, block);
            String questionText = extractGroup(questionPat, block);
            String optionsStr = extractGroup(optionsPat, block);
            String answer = extractGroup(answerPat, block);
            String analysis = extractGroup(analysisPat, block);
            dto.setType(normalizeType(type));
            dto.setQuestionText(questionText != null ? questionText.strip() : "");
            dto.setAnswer(answer != null ? answer.strip() : "");
            dto.setAnalysis(analysis != null ? analysis.strip() : "");
            if (optionsStr != null && !optionsStr.isBlank() && !optionsStr.contains("无")) {
                dto.setOptions(parseOptions(optionsStr));
            }
            list.add(dto);
        }
        return list;
    }

    private static String extractGroup(Pattern p, String block) {
        Matcher m = p.matcher(block);
        if (m.find()) {
            int start = m.start(1);
            // 从当前段落后找下一个「【」，只截取到下一段之前，避免把选项/答案/解析混进题目
            int next = block.indexOf("【", start + 1);
            if (next > 0) {
                return block.substring(start, next).strip();
            }
            return block.substring(start).strip();
        }
        return null;
    }

    private static String normalizeType(String type) {
        if (type == null) return "essay";
        type = type.strip().toLowerCase();
        if (type.contains("单")) return "single";
        if (type.contains("多")) return "multiple";
        if (type.contains("填")) return "fill";
        return "essay";
    }

    private static List<OptionItem> parseOptions(String optionsStr) {
        List<OptionItem> list = new ArrayList<>();
        Pattern keyVal = Pattern.compile("([A-D])[.．、]\\s*([^A-D.．、]+)");
        Matcher m = keyVal.matcher(optionsStr);
        while (m.find()) {
            OptionItem item = new OptionItem();
            item.setKey(m.group(1));
            item.setValue(m.group(2).strip());
            list.add(item);
        }
        return list;
    }
}
