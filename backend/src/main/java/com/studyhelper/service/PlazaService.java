package com.studyhelper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyhelper.dto.plaza.*;
import com.studyhelper.entity.*;
import com.studyhelper.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlazaService {

    private static final long ADMIN_ID = 0L;
    public static final String CAT_DISCUSSION = "DISCUSSION";
    public static final String CAT_MARKETPLACE = "MARKETPLACE";

    private final PlazaPostMapper postMapper;
    private final PlazaCommentMapper commentMapper;
    private final PlazaPostLikeMapper likeMapper;
    private final PlazaPostFavoriteMapper favoriteMapper;
    private final PlazaMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private void rejectAdmin(Long userId) {
        if (userId != null && userId.equals(ADMIN_ID)) {
            throw new IllegalArgumentException("管理员账号不参与交流广场");
        }
    }

    @Transactional
    public PlazaPostVo createPost(Long userId, Map<String, Object> body) {
        rejectAdmin(userId);
        String category = Objects.toString(body.get("category"), "").trim();
        if (!CAT_DISCUSSION.equals(category) && !CAT_MARKETPLACE.equals(category)) {
            throw new IllegalArgumentException("请选择帖子类型");
        }
        String content = Objects.toString(body.get("content"), "").trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("内容不能为空");
        }
        String title = body.get("title") != null ? Objects.toString(body.get("title"), "").trim() : "";
        if (CAT_MARKETPLACE.equals(category) && title.isEmpty()) {
            throw new IllegalArgumentException("闲置商品请填写标题");
        }
        PlazaPost p = new PlazaPost();
        p.setUserId(userId);
        p.setCategory(category);
        p.setTitle(title.isEmpty() ? null : title);
        p.setContent(content);
        Object imgObj = body.get("images");
        if (imgObj instanceof List<?> raw && !raw.isEmpty()) {
            List<String> imgs = new ArrayList<>();
            for (Object o : raw) {
                imgs.add(Objects.toString(o));
            }
            try {
                p.setImagesJson(objectMapper.writeValueAsString(imgs));
            } catch (Exception e) {
                throw new IllegalArgumentException("图片格式错误");
            }
        }
        if (CAT_MARKETPLACE.equals(category)) {
            Object pc = body.get("priceYuan");
            if (pc == null) {
                pc = body.get("priceCent");
            }
            if (pc == null) {
                throw new IllegalArgumentException("请设置价格（元）");
            }
            long cents = parsePriceCent(pc);
            if (cents <= 0) {
                throw new IllegalArgumentException("价格需大于 0");
            }
            p.setPriceCent(cents);
            p.setTradeStatus("ON_SALE");
        } else {
            p.setPriceCent(null);
            p.setTradeStatus("NA");
        }
        p.setLikeCount(0);
        p.setCommentCount(0);
        p.setFavoriteCount(0);
        p.setShareCount(0);
        postMapper.insert(p);
        return toVo(p, userId, true);
    }

    private long parsePriceCent(Object priceObj) {
        if (priceObj instanceof Number n) {
            return Math.round(n.doubleValue() * 100);
        }
        String s = Objects.toString(priceObj, "").trim();
        if (s.isEmpty()) throw new IllegalArgumentException("请填写价格");
        BigDecimal yuan = new BigDecimal(s);
        return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public Page<PlazaPostVo> pagePosts(Long currentUserId, String category, int pageNum, int pageSize) {
        Page<PlazaPost> p = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PlazaPost> q = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            q.eq(PlazaPost::getCategory, category.toUpperCase());
        }
        q.orderByDesc(PlazaPost::getCreateTime);
        postMapper.selectPage(p, q);
        List<PlazaPostVo> list = p.getRecords().stream()
                .map(post -> toVo(post, currentUserId, false))
                .collect(Collectors.toList());
        Page<PlazaPostVo> out = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        out.setRecords(list);
        return out;
    }

    public PlazaPostVo getPost(Long id, Long currentUserId) {
        PlazaPost post = postMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        return toVo(post, currentUserId, true);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        rejectAdmin(userId);
        PlazaPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己的帖子");
        }
        postMapper.deleteById(postId);
    }

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        rejectAdmin(userId);
        ensurePost(postId);
        LambdaQueryWrapper<PlazaPostLike> q = new LambdaQueryWrapper<>();
        q.eq(PlazaPostLike::getPostId, postId).eq(PlazaPostLike::getUserId, userId);
        PlazaPostLike exist = likeMapper.selectOne(q);
        if (exist != null) {
            likeMapper.deleteById(exist.getId());
            decCount(postId, "like_count");
            return false;
        }
        PlazaPostLike l = new PlazaPostLike();
        l.setPostId(postId);
        l.setUserId(userId);
        l.setCreateTime(LocalDateTime.now());
        likeMapper.insert(l);
        incCount(postId, "like_count");
        return true;
    }

    @Transactional
    public boolean toggleFavorite(Long postId, Long userId) {
        rejectAdmin(userId);
        ensurePost(postId);
        LambdaQueryWrapper<PlazaPostFavorite> q = new LambdaQueryWrapper<>();
        q.eq(PlazaPostFavorite::getPostId, postId).eq(PlazaPostFavorite::getUserId, userId);
        PlazaPostFavorite exist = favoriteMapper.selectOne(q);
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            decCount(postId, "favorite_count");
            return false;
        }
        PlazaPostFavorite f = new PlazaPostFavorite();
        f.setPostId(postId);
        f.setUserId(userId);
        f.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(f);
        incCount(postId, "favorite_count");
        return true;
    }

    @Transactional
    public void sharePost(Long postId) {
        ensurePost(postId);
        incCount(postId, "share_count");
    }

    @Transactional
    public PlazaCommentVo addComment(Long postId, Long userId, Map<String, Object> body) {
        rejectAdmin(userId);
        ensurePost(postId);
        String content = Objects.toString(body.get("content"), "").trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("评论不能为空");
        }
        Long parentId = null;
        Long replyToUserId = null;
        if (body.get("parentId") != null) {
            parentId = Long.parseLong(Objects.toString(body.get("parentId")));
            PlazaComment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getPostId().equals(postId)) {
                throw new IllegalArgumentException("回复的评论不存在");
            }
            if (body.get("replyToUserId") != null) {
                replyToUserId = Long.parseLong(Objects.toString(body.get("replyToUserId")));
            }
        }
        PlazaComment c = new PlazaComment();
        c.setPostId(postId);
        c.setUserId(userId);
        c.setParentId(parentId);
        c.setReplyToUserId(replyToUserId);
        c.setContent(content);
        commentMapper.insert(c);
        incCount(postId, "comment_count");
        return toCommentVo(c);
    }

    public List<PlazaCommentVo> listComments(Long postId) {
        ensurePost(postId);
        LambdaQueryWrapper<PlazaComment> q = new LambdaQueryWrapper<>();
        q.eq(PlazaComment::getPostId, postId).orderByAsc(PlazaComment::getCreateTime);
        List<PlazaComment> all = commentMapper.selectList(q);
        return buildCommentTree(all);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        rejectAdmin(userId);
        PlazaComment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        if (!c.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己的评论");
        }
        Long postId = c.getPostId();
        LambdaQueryWrapper<PlazaComment> childQ = new LambdaQueryWrapper<>();
        childQ.eq(PlazaComment::getParentId, commentId);
        long child = commentMapper.selectCount(childQ);
        if (child > 0) {
            commentMapper.delete(childQ);
        }
        commentMapper.deleteById(commentId);
        decCount(postId, "comment_count");
        for (int i = 0; i < child; i++) {
            decCount(postId, "comment_count");
        }
    }

    /**
     * 更新闲置状态（仅作者）
     */
    @Transactional
    public PlazaPostVo updateTradeStatus(Long postId, Long userId, String tradeStatus) {
        rejectAdmin(userId);
        PlazaPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!CAT_MARKETPLACE.equals(post.getCategory())) {
            throw new IllegalArgumentException("仅闲置帖可更新交易状态");
        }
        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能修改自己的商品");
        }
        if (!Set.of("ON_SALE", "SOLD", "RESERVED").contains(tradeStatus)) {
            throw new IllegalArgumentException("无效状态");
        }
        post.setTradeStatus(tradeStatus);
        postMapper.updateById(post);
        return toVo(post, userId, true);
    }

    public String uploadPlazaImage(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("请选择图片");
        }
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".jpg";
        if (!Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("仅支持 jpg/png/gif/webp 图片");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("单张图片不能超过 5MB");
        }
        Path dir = Paths.get(uploadDir, "plaza").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(storedName);
        file.transferTo(target.toFile());
        return "plaza/" + storedName;
    }

    public Page<PlazaMessageVo> listMessages(Long me, Long withUserId, int pageNum, int pageSize) {
        rejectAdmin(me);
        if (withUserId.equals(me)) {
            throw new IllegalArgumentException("不能与自己对话");
        }
        Page<PlazaMessage> p = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PlazaMessage> q = new LambdaQueryWrapper<>();
        q.and(w -> w.and(w1 -> w1.eq(PlazaMessage::getFromUserId, me).eq(PlazaMessage::getToUserId, withUserId))
                .or(w2 -> w2.eq(PlazaMessage::getFromUserId, withUserId).eq(PlazaMessage::getToUserId, me)));
        q.orderByDesc(PlazaMessage::getCreateTime);
        messageMapper.selectPage(p, q);
        List<PlazaMessageVo> vos = new ArrayList<>();
        for (PlazaMessage m : p.getRecords()) {
            vos.add(toMessageVo(m, me));
        }
        Collections.reverse(vos);
        Page<PlazaMessageVo> out = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        out.setRecords(vos);
        markRead(me, withUserId);
        return out;
    }

    @Transactional
    public PlazaMessageVo sendMessage(Long fromUserId, Map<String, Object> body) {
        rejectAdmin(fromUserId);
        Long toUserId = Long.parseLong(Objects.toString(body.get("toUserId")));
        if (toUserId.equals(fromUserId)) {
            throw new IllegalArgumentException("不能给自己发消息");
        }
        if (toUserId.equals(ADMIN_ID)) {
            throw new IllegalArgumentException("无法给管理员发私信");
        }
        User to = userMapper.selectById(toUserId);
        if (to == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String content = Objects.toString(body.get("content"), "").trim();
        String imagePath = body.get("imagePath") != null ? Objects.toString(body.get("imagePath")).trim() : null;
        if (content.isEmpty() && (imagePath == null || imagePath.isEmpty())) {
            throw new IllegalArgumentException("请输入内容或图片");
        }
        PlazaMessage m = new PlazaMessage();
        m.setFromUserId(fromUserId);
        m.setToUserId(toUserId);
        m.setContent(content.isEmpty() ? null : content);
        m.setImagePath(imagePath);
        m.setReadFlag(false);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
        return toMessageVo(m, fromUserId);
    }

    public List<PlazaChatPartnerVo> chatPartners(Long userId) {
        rejectAdmin(userId);
        LambdaQueryWrapper<PlazaMessage> q = new LambdaQueryWrapper<>();
        q.and(w -> w.eq(PlazaMessage::getFromUserId, userId).or().eq(PlazaMessage::getToUserId, userId));
        q.orderByDesc(PlazaMessage::getCreateTime);
        List<PlazaMessage> all = messageMapper.selectList(q);
        Map<Long, PlazaMessage> lastByPartner = new LinkedHashMap<>();
        for (PlazaMessage m : all) {
            long partner = m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId();
            lastByPartner.putIfAbsent(partner, m);
        }
        List<PlazaChatPartnerVo> out = new ArrayList<>();
        for (Map.Entry<Long, PlazaMessage> e : lastByPartner.entrySet()) {
            Long pid = e.getKey();
            PlazaMessage last = e.getValue();
            User u = userMapper.selectById(pid);
            if (u == null) continue;
            PlazaChatPartnerVo vo = new PlazaChatPartnerVo();
            vo.setUserId(pid);
            vo.setNickname(u.getNickname() != null ? u.getNickname() : u.getUsername());
            vo.setAvatar(u.getAvatar());
            vo.setLastTime(last.getCreateTime());
            String preview = last.getContent() != null ? last.getContent() : "[图片]";
            if (preview.length() > 40) preview = preview.substring(0, 40) + "…";
            vo.setLastPreview(preview);
            LambdaQueryWrapper<PlazaMessage> uq = new LambdaQueryWrapper<>();
            uq.eq(PlazaMessage::getFromUserId, pid).eq(PlazaMessage::getToUserId, userId).eq(PlazaMessage::getReadFlag, false);
            vo.setUnreadCount(messageMapper.selectCount(uq));
            out.add(vo);
        }
        return out;
    }

    private void markRead(Long me, Long partnerId) {
        LambdaUpdateWrapper<PlazaMessage> u = new LambdaUpdateWrapper<>();
        u.eq(PlazaMessage::getFromUserId, partnerId).eq(PlazaMessage::getToUserId, me).eq(PlazaMessage::getReadFlag, false);
        PlazaMessage patch = new PlazaMessage();
        patch.setReadFlag(true);
        messageMapper.update(patch, u);
    }

    private void ensurePost(Long postId) {
        if (postMapper.selectById(postId) == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
    }

    private void incCount(Long postId, String field) {
        LambdaUpdateWrapper<PlazaPost> u = new LambdaUpdateWrapper<>();
        u.eq(PlazaPost::getId, postId).setSql(field + " = " + field + " + 1");
        postMapper.update(null, u);
    }

    private void decCount(Long postId, String field) {
        LambdaUpdateWrapper<PlazaPost> u = new LambdaUpdateWrapper<>();
        u.eq(PlazaPost::getId, postId).setSql(field + " = GREATEST(" + field + " - 1, 0)");
        postMapper.update(null, u);
    }

    private PlazaPostVo toVo(PlazaPost post, Long currentUserId, boolean refresh) {
        if (refresh) {
            post = postMapper.selectById(post.getId());
        }
        PlazaPostVo vo = new PlazaPostVo();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        User author = userMapper.selectById(post.getUserId());
        if (author != null) {
            vo.setAuthorNickname(author.getNickname() != null ? author.getNickname() : author.getUsername());
            vo.setAuthorAvatar(author.getAvatar());
        }
        vo.setCategory(post.getCategory());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        if (post.getImagesJson() != null && !post.getImagesJson().isBlank()) {
            try {
                vo.setImages(objectMapper.readValue(post.getImagesJson(), new TypeReference<List<String>>() {}));
            } catch (Exception e) {
                vo.setImages(List.of());
            }
        } else {
            vo.setImages(List.of());
        }
        vo.setPriceCent(post.getPriceCent());
        if (post.getPriceCent() != null) {
            BigDecimal yuan = BigDecimal.valueOf(post.getPriceCent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            vo.setPriceDisplay(yuan.toPlainString());
        }
        vo.setTradeStatus(post.getTradeStatus());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setFavoriteCount(post.getFavoriteCount());
        vo.setShareCount(post.getShareCount());
        vo.setCreateTime(post.getCreateTime());
        if (currentUserId != null && !currentUserId.equals(ADMIN_ID)) {
            vo.setLiked(hasLike(post.getId(), currentUserId));
            vo.setFavorited(hasFavorite(post.getId(), currentUserId));
        } else {
            vo.setLiked(false);
            vo.setFavorited(false);
        }
        return vo;
    }

    private boolean hasLike(Long postId, Long userId) {
        LambdaQueryWrapper<PlazaPostLike> q = new LambdaQueryWrapper<>();
        q.eq(PlazaPostLike::getPostId, postId).eq(PlazaPostLike::getUserId, userId);
        return likeMapper.selectCount(q) > 0;
    }

    private boolean hasFavorite(Long postId, Long userId) {
        LambdaQueryWrapper<PlazaPostFavorite> q = new LambdaQueryWrapper<>();
        q.eq(PlazaPostFavorite::getPostId, postId).eq(PlazaPostFavorite::getUserId, userId);
        return favoriteMapper.selectCount(q) > 0;
    }

    private PlazaCommentVo toCommentVo(PlazaComment c) {
        PlazaCommentVo vo = new PlazaCommentVo();
        vo.setId(c.getId());
        vo.setPostId(c.getPostId());
        vo.setUserId(c.getUserId());
        User u = userMapper.selectById(c.getUserId());
        if (u != null) {
            vo.setAuthorNickname(u.getNickname() != null ? u.getNickname() : u.getUsername());
            vo.setAuthorAvatar(u.getAvatar());
        }
        vo.setParentId(c.getParentId());
        vo.setReplyToUserId(c.getReplyToUserId());
        if (c.getReplyToUserId() != null) {
            User ru = userMapper.selectById(c.getReplyToUserId());
            if (ru != null) {
                vo.setReplyToNickname(ru.getNickname() != null ? ru.getNickname() : ru.getUsername());
            }
        }
        vo.setContent(c.getContent());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private List<PlazaCommentVo> buildCommentTree(List<PlazaComment> all) {
        Map<Long, PlazaCommentVo> map = new HashMap<>();
        for (PlazaComment c : all) {
            PlazaCommentVo vo = toCommentVo(c);
            vo.setChildren(new ArrayList<>());
            map.put(c.getId(), vo);
        }
        List<PlazaCommentVo> roots = new ArrayList<>();
        for (PlazaComment c : all) {
            PlazaCommentVo vo = map.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(vo);
            } else {
                PlazaCommentVo parent = map.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    private PlazaMessageVo toMessageVo(PlazaMessage m, Long me) {
        PlazaMessageVo vo = new PlazaMessageVo();
        vo.setId(m.getId());
        vo.setFromUserId(m.getFromUserId());
        vo.setToUserId(m.getToUserId());
        vo.setContent(m.getContent());
        vo.setImagePath(m.getImagePath());
        vo.setReadFlag(m.getReadFlag());
        vo.setMine(m.getFromUserId().equals(me));
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}
