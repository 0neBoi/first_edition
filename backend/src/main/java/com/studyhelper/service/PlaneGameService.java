package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyhelper.dto.DinoLeaderboardItem;
import com.studyhelper.entity.PlaneBest;
import com.studyhelper.entity.User;
import com.studyhelper.mapper.PlaneBestMapper;
import com.studyhelper.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaneGameService {

    private static final long ADMIN_USER_ID = 0L;

    private final PlaneBestMapper planeBestMapper;
    private final UserMapper userMapper;

    @Transactional
    public void submitBest(Long userId, int score) {
        if (userId == null || userId.equals(ADMIN_USER_ID)) {
            throw new IllegalArgumentException("管理员不参与排行");
        }
        if (score < 0 || score > 9_999_999) {
            throw new IllegalArgumentException("分数不合法");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        PlaneBest existing = planeBestMapper.selectById(userId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            PlaneBest row = new PlaneBest();
            row.setUserId(userId);
            row.setScore(score);
            row.setUpdateTime(now);
            planeBestMapper.insert(row);
        } else if (score > existing.getScore()) {
            existing.setScore(score);
            existing.setUpdateTime(now);
            planeBestMapper.updateById(existing);
        }
    }

    public Integer getMyBest(Long userId) {
        if (userId == null || userId.equals(ADMIN_USER_ID)) {
            return null;
        }
        PlaneBest d = planeBestMapper.selectById(userId);
        return d == null ? null : d.getScore();
    }

    public List<DinoLeaderboardItem> leaderboard(int size) {
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
        LambdaQueryWrapper<PlaneBest> q = new LambdaQueryWrapper<>();
        q.orderByDesc(PlaneBest::getScore).orderByDesc(PlaneBest::getUpdateTime);
        q.last("LIMIT " + size);
        List<PlaneBest> rows = planeBestMapper.selectList(q);
        List<DinoLeaderboardItem> out = new ArrayList<>();
        int rank = 1;
        for (PlaneBest row : rows) {
            DinoLeaderboardItem item = new DinoLeaderboardItem();
            item.setRank(rank++);
            item.setUserId(row.getUserId());
            item.setScore(row.getScore());
            item.setUpdateTime(row.getUpdateTime());
            User u = userMapper.selectById(row.getUserId());
            if (u != null) {
                String nick = u.getNickname();
                item.setDisplayName(nick != null && !nick.isBlank() ? nick : u.getUsername());
            } else {
                item.setDisplayName("用户" + row.getUserId());
            }
            out.add(item);
        }
        return out;
    }

    /**
     * 当前用户在全局排行中的名次与成绩（未上榜过则为 null）。
     * 排序规则与 {@link #leaderboard(int)} 一致。
     */
    public DinoLeaderboardItem getMyLeaderboardEntry(Long userId) {
        if (userId == null || userId.equals(ADMIN_USER_ID)) {
            return null;
        }
        PlaneBest mine = planeBestMapper.selectById(userId);
        if (mine == null) {
            return null;
        }
        LambdaQueryWrapper<PlaneBest> q = new LambdaQueryWrapper<>();
        q.and(w -> w.gt(PlaneBest::getScore, mine.getScore())
                .or(w2 -> w2.eq(PlaneBest::getScore, mine.getScore())
                        .gt(PlaneBest::getUpdateTime, mine.getUpdateTime())));
        long ahead = planeBestMapper.selectCount(q);
        int rank = (int) Math.min(Integer.MAX_VALUE, ahead + 1);

        DinoLeaderboardItem item = new DinoLeaderboardItem();
        item.setRank(rank);
        item.setUserId(mine.getUserId());
        item.setScore(mine.getScore());
        item.setUpdateTime(mine.getUpdateTime());
        User u = userMapper.selectById(userId);
        if (u != null) {
            String nick = u.getNickname();
            item.setDisplayName(nick != null && !nick.isBlank() ? nick : u.getUsername());
        } else {
            item.setDisplayName("用户" + userId);
        }
        return item;
    }
}
