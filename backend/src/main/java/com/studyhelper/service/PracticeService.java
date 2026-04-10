package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyhelper.dto.PracticeSubmitResult;
import com.studyhelper.entity.QuestionAttempt;
import com.studyhelper.entity.StudyMaterial;
import com.studyhelper.entity.StudyQuestion;
import com.studyhelper.mapper.QuestionAttemptMapper;
import com.studyhelper.mapper.StudyMaterialMapper;
import com.studyhelper.mapper.StudyQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeService {

    private final QuestionAttemptMapper attemptMapper;
    private final StudyQuestionMapper questionMapper;
    private final StudyMaterialMapper materialMapper;

    private static final Pattern LETTER_PAT = Pattern.compile("[A-Za-z]");

    public PracticeSubmitResult submit(Long userId, Long questionId, String userAnswer, Integer timeCostMs) {
        StudyQuestion q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        StudyMaterial m = materialMapper.selectById(q.getMaterialId());
        if (m == null || !userId.equals(m.getUserId())) {
            throw new IllegalArgumentException("无权练习该题目");
        }
        boolean correct = matchAnswer(q.getType(), q.getAnswer(), userAnswer);
        QuestionAttempt a = new QuestionAttempt();
        a.setUserId(userId);
        a.setQuestionId(questionId);
        a.setIsCorrect(correct ? 1 : 0);
        a.setUserAnswer(userAnswer != null ? userAnswer : "");
        a.setTimeCostMs(timeCostMs);
        a.setAttemptTime(LocalDateTime.now());
        attemptMapper.insert(a);

        PracticeSubmitResult r = new PracticeSubmitResult();
        r.setQuestionId(questionId);
        r.setCorrect(correct);
        r.setStandardAnswer(q.getAnswer());
        r.setAnalysis(q.getAnalysis());
        r.setMasteryScore(masteryForQuestion(userId, questionId));
        return r;
    }

    public int masteryForQuestion(Long userId, Long questionId) {
        LambdaQueryWrapper<QuestionAttempt> q = new LambdaQueryWrapper<>();
        q.eq(QuestionAttempt::getUserId, userId)
                .eq(QuestionAttempt::getQuestionId, questionId)
                .orderByDesc(QuestionAttempt::getAttemptTime)
                .last("LIMIT 10");
        List<QuestionAttempt> list = attemptMapper.selectList(q);
        if (list.isEmpty()) {
            return 0;
        }
        long correct = list.stream().filter(x -> x.getIsCorrect() != null && x.getIsCorrect() == 1).count();
        return (int) Math.round(5.0 * correct / list.size());
    }

    public Map<Long, Integer> masteryForQuestions(Long userId, Collection<Long> questionIds) {
        Map<Long, Integer> out = new HashMap<>();
        for (Long qid : questionIds) {
            StudyQuestion q = questionMapper.selectById(qid);
            if (q == null || !isOwned(userId, q)) {
                continue;
            }
            out.put(qid, masteryForQuestion(userId, qid));
        }
        return out;
    }

    /** 最近一次作答记录（每题一条） */
    public Map<Long, QuestionAttempt> latestByQuestion(Long userId) {
        LambdaQueryWrapper<QuestionAttempt> q = new LambdaQueryWrapper<>();
        q.eq(QuestionAttempt::getUserId, userId).orderByDesc(QuestionAttempt::getAttemptTime);
        List<QuestionAttempt> all = attemptMapper.selectList(q);
        Map<Long, QuestionAttempt> map = new LinkedHashMap<>();
        for (QuestionAttempt a : all) {
            map.putIfAbsent(a.getQuestionId(), a);
        }
        return map;
    }

    public List<StudyQuestion> listWrongBook(Long userId, Long materialId) {
        Map<Long, QuestionAttempt> latest = latestByQuestion(userId);
        List<Long> wrongIds = latest.entrySet().stream()
                .filter(e -> e.getValue().getIsCorrect() != null && e.getValue().getIsCorrect() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (wrongIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<StudyQuestion> qq = new LambdaQueryWrapper<>();
        qq.in(StudyQuestion::getId, wrongIds);
        List<StudyQuestion> questions = questionMapper.selectList(qq);
        if (materialId != null) {
            questions = questions.stream().filter(x -> materialId.equals(x.getMaterialId())).collect(Collectors.toList());
        }
        questions = filterOwned(userId, questions);
        Map<Long, LocalDateTime> lastWrong = new HashMap<>();
        for (QuestionAttempt a : attemptMapper.selectList(new LambdaQueryWrapper<QuestionAttempt>()
                .eq(QuestionAttempt::getUserId, userId)
                .eq(QuestionAttempt::getIsCorrect, 0)
                .orderByDesc(QuestionAttempt::getAttemptTime))) {
            lastWrong.putIfAbsent(a.getQuestionId(), a.getAttemptTime());
        }
        questions.sort(Comparator.comparing((StudyQuestion x) -> lastWrong.getOrDefault(x.getId(), LocalDateTime.MIN)).reversed());
        return questions;
    }

    public List<StudyQuestion> reviewToday(Long userId, int maxSize) {
        Map<Long, QuestionAttempt> latest = latestByQuestion(userId);
        List<StudyQuestion> out = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        List<Long> wrongLatest = latest.entrySet().stream()
                .filter(e -> e.getValue().getIsCorrect() != null && e.getValue().getIsCorrect() == 0)
                .sorted(Comparator.comparing(e -> e.getValue().getAttemptTime(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        for (Long qid : wrongLatest) {
            if (out.size() >= maxSize) {
                break;
            }
            StudyQuestion q = questionMapper.selectById(qid);
            if (q != null && isOwned(userId, q)) {
                out.add(q);
                used.add(qid);
            }
        }

        if (out.size() < maxSize) {
            LambdaQueryWrapper<StudyMaterial> mq = new LambdaQueryWrapper<>();
            mq.eq(StudyMaterial::getUserId, userId).orderByDesc(StudyMaterial::getCreateTime);
            List<StudyMaterial> materials = materialMapper.selectList(mq);
            List<StudyQuestion> pool = new ArrayList<>();
            for (StudyMaterial m : materials) {
                LambdaQueryWrapper<StudyQuestion> qq = new LambdaQueryWrapper<>();
                qq.eq(StudyQuestion::getMaterialId, m.getId()).orderByAsc(StudyQuestion::getSortOrder);
                pool.addAll(questionMapper.selectList(qq));
            }
            Collections.shuffle(pool);
            for (StudyQuestion q : pool) {
                if (out.size() >= maxSize) {
                    break;
                }
                if (used.contains(q.getId())) {
                    continue;
                }
                int m = masteryForQuestion(userId, q.getId());
                if (m <= 2 || !latest.containsKey(q.getId())) {
                    out.add(q);
                    used.add(q.getId());
                }
            }
        }
        return out;
    }

    public Map<String, Object> stats(Long userId) {
        LambdaQueryWrapper<QuestionAttempt> q = new LambdaQueryWrapper<>();
        q.eq(QuestionAttempt::getUserId, userId);
        List<QuestionAttempt> all = attemptMapper.selectList(q);
        long total = all.size();
        long correct = all.stream().filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1).count();
        Map<String, Object> map = new HashMap<>();
        map.put("totalAttempts", total);
        map.put("correctAttempts", correct);
        map.put("accuracy", total == 0 ? 0.0 : Math.round(1000.0 * correct / total) / 10.0);
        map.put("wrongBookSize", listWrongBook(userId, null).size());
        return map;
    }

    private boolean isOwned(Long userId, StudyQuestion q) {
        StudyMaterial m = materialMapper.selectById(q.getMaterialId());
        return m != null && userId.equals(m.getUserId());
    }

    private List<StudyQuestion> filterOwned(Long userId, List<StudyQuestion> list) {
        return list.stream().filter(q -> isOwned(userId, q)).collect(Collectors.toList());
    }

    static boolean matchAnswer(String type, String standard, String userRaw) {
        String std = standard == null ? "" : standard.strip();
        String user = userRaw == null ? "" : userRaw.strip();
        String t = type != null ? type : "essay";
        return switch (t) {
            case "single" -> matchSingle(std, user);
            case "multiple" -> matchMultiple(std, user);
            case "fill" -> normText(std).equals(normText(user));
            default -> matchEssay(std, user);
        };
    }

    private static boolean matchSingle(String std, String user) {
        String lettersStd = choiceLetters(std);
        String lettersUser = choiceLetters(user);
        if (lettersStd.length() == 1 && lettersUser.length() >= 1) {
            return lettersStd.charAt(0) == lettersUser.charAt(0);
        }
        return normText(std).equals(normText(user));
    }

    private static boolean matchMultiple(String std, String user) {
        Set<Character> a = letterSet(std);
        Set<Character> b = letterSet(user);
        if (!a.isEmpty() && !b.isEmpty()) {
            return a.equals(b);
        }
        return normText(std).equals(normText(user));
    }

    private static boolean matchEssay(String std, String user) {
        String ns = normText(std);
        String nu = normText(user);
        if (ns.isEmpty() && nu.isEmpty()) {
            return true;
        }
        if (ns.equals(nu)) {
            return true;
        }
        if (ns.length() >= 12 && nu.length() >= 8 && (ns.contains(nu) || nu.contains(ns))) {
            return true;
        }
        return false;
    }

    private static String choiceLetters(String s) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Matcher m = LETTER_PAT.matcher(s);
        while (m.find()) {
            char c = Character.toUpperCase(m.group().charAt(0));
            if (c >= 'A' && c <= 'Z') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Set<Character> letterSet(String s) {
        return choiceLetters(s).chars().mapToObj(c -> (char) c).collect(Collectors.toCollection(TreeSet::new));
    }

    private static String normText(String s) {
        if (s == null) {
            return "";
        }
        return s.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
