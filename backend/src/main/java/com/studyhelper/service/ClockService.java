package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyhelper.entity.StudyClock;
import com.studyhelper.mapper.StudyClockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClockService {

    private final StudyClockMapper clockMapper;

    public StudyClock upsertDay(Long userId, LocalDate clockDate, int minutes, String remark) {
        if (clockDate == null) {
            clockDate = LocalDate.now();
        }
        if (minutes < 0) {
            minutes = 0;
        }
        LambdaQueryWrapper<StudyClock> q = new LambdaQueryWrapper<>();
        q.eq(StudyClock::getUserId, userId).eq(StudyClock::getClockDate, clockDate);
        StudyClock existing = clockMapper.selectOne(q);
        if (existing != null) {
            existing.setMinutes(minutes);
            existing.setRemark(StringUtils.hasText(remark) ? remark.trim() : existing.getRemark());
            clockMapper.updateById(existing);
            return clockMapper.selectById(existing.getId());
        }
        StudyClock c = new StudyClock();
        c.setUserId(userId);
        c.setClockDate(clockDate);
        c.setMinutes(minutes);
        c.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        clockMapper.insert(c);
        return c;
    }

    public List<StudyClock> listRange(Long userId, LocalDate from, LocalDate to) {
        LambdaQueryWrapper<StudyClock> q = new LambdaQueryWrapper<>();
        q.eq(StudyClock::getUserId, userId);
        if (from != null) {
            q.ge(StudyClock::getClockDate, from);
        }
        if (to != null) {
            q.le(StudyClock::getClockDate, to);
        }
        q.orderByAsc(StudyClock::getClockDate);
        return clockMapper.selectList(q);
    }

    public Map<String, Object> monthSummary(Long userId, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        List<StudyClock> list = listRange(userId, start, end);
        int totalMinutes = list.stream().mapToInt(c -> c.getMinutes() != null ? c.getMinutes() : 0).sum();
        Map<String, Integer> byDay = list.stream()
                .collect(Collectors.toMap(c -> c.getClockDate().toString(), c -> c.getMinutes() != null ? c.getMinutes() : 0, Integer::sum));
        Map<String, Object> out = new HashMap<>();
        out.put("yearMonth", yearMonth.toString());
        out.put("totalMinutes", totalMinutes);
        out.put("days", byDay);
        out.put("entries", list);
        return out;
    }

    /**
     * 连续学习天数：若今日有记录则从今天往前；否则若昨日有记录则从昨日往前；否则 0。
     */
    public int streakDays(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate anchor = null;
        StudyClock todayRow = find(userId, today);
        if (todayRow != null && nz(todayRow.getMinutes()) > 0) {
            anchor = today;
        } else {
            StudyClock y = find(userId, today.minusDays(1));
            if (y != null && nz(y.getMinutes()) > 0) {
                anchor = today.minusDays(1);
            }
        }
        if (anchor == null) {
            return 0;
        }
        int streak = 0;
        LocalDate d = anchor;
        while (true) {
            StudyClock c = find(userId, d);
            if (c == null || nz(c.getMinutes()) <= 0) {
                break;
            }
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    private StudyClock find(Long userId, LocalDate date) {
        LambdaQueryWrapper<StudyClock> q = new LambdaQueryWrapper<>();
        q.eq(StudyClock::getUserId, userId).eq(StudyClock::getClockDate, date);
        return clockMapper.selectOne(q);
    }

    private static int nz(Integer m) {
        return m != null ? m : 0;
    }
}
