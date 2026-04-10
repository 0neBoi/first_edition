<template>
  <div class="plaza-page">
    <div class="plaza-wrap">
      <header class="plaza-head">
        <div>
          <h1>{{ plazaPageTitle }}</h1>
          <p class="plaza-sub">
            {{ plazaPageSub }}
          </p>
        </div>
        <div class="plaza-head-actions" v-if="!isAdmin">
          <el-button type="primary" @click="openPublish()">发布</el-button>
        </div>
        <el-alert v-else type="info" show-icon :closable="false" title="管理员账号仅可浏览广场内容。" />
      </header>

      <!-- 私聊功能区 -->
      <section v-if="!isAdmin" class="plaza-section plaza-section-chat">
        <div class="section-head">
          <h2 class="section-title">私信</h2>
          <p class="section-desc">与同学一对一聊天；在帖子详情中也可向作者发起私聊。</p>
        </div>
        <div class="chat-zone-card study-glass-block">
          <el-icon class="chat-zone-icon" :size="40"><ChatDotRound /></el-icon>
          <div class="chat-zone-text">
            <span class="chat-zone-lead">查看会话列表并回复消息</span>
            <span class="chat-zone-hint">侧栏「私信」也可直接进入</span>
          </div>
          <el-button type="primary" size="large" @click="openChatList">打开私信</el-button>
        </div>
      </section>

      <!-- 校园交流 -->
      <section v-if="showDiscussionSection" class="plaza-section">
        <div class="section-head">
          <h2 class="section-title">校园交流</h2>
          <el-button v-if="!isAdmin" type="primary" plain @click="openPublish('DISCUSSION')">发布交流帖</el-button>
        </div>
        <div v-loading="loading" class="plaza-feed">
          <el-empty v-if="!loading && feed.DISCUSSION.posts.length === 0" description="暂无交流帖" />
          <article
            v-for="post in feed.DISCUSSION.posts"
            :key="post.id"
            class="post-card study-glass-block"
            @click="openDetail(post)"
          >
            <div class="post-card-top">
              <el-avatar :size="40" :src="avatarUrl(post.authorAvatar)">{{ (post.authorNickname || '')[0] }}</el-avatar>
              <div class="post-card-meta">
                <span class="post-author">{{ post.authorNickname }}</span>
                <span class="post-time">{{ formatTime(post.createTime) }}</span>
              </div>
              <el-tag size="small" type="info">交流</el-tag>
            </div>
            <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
            <p class="post-excerpt">{{ truncate(post.content, 160) }}</p>
            <div v-if="post.images?.length" class="post-images">
              <el-image
                v-for="(img, i) in post.images.slice(0, 3)"
                :key="i"
                :src="plazaImg(img)"
                fit="cover"
                class="thumb"
              />
            </div>
            <div class="post-stats" @click.stop>
              <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
              <span><el-icon><Pointer /></el-icon> {{ post.likeCount }}</span>
              <span><el-icon><Star /></el-icon> {{ post.favoriteCount }}</span>
            </div>
          </article>
          <div v-if="feed.DISCUSSION.total > feed.DISCUSSION.posts.length" class="plaza-more">
            <el-button
              @click="loadMore('DISCUSSION')"
              :loading="loadingMoreCat === 'DISCUSSION'"
            >
              加载更多
            </el-button>
          </div>
        </div>
      </section>

      <!-- 闲置市集 -->
      <section v-if="showMarketSection" class="plaza-section">
        <div class="section-head">
          <h2 class="section-title">闲置市集</h2>
          <el-button v-if="!isAdmin" type="warning" plain @click="openPublish('MARKETPLACE')">发布闲置</el-button>
        </div>
        <div v-loading="loading" class="plaza-feed">
          <el-empty v-if="!loading && feed.MARKETPLACE.posts.length === 0" description="暂无闲置帖" />
          <article
            v-for="post in feed.MARKETPLACE.posts"
            :key="post.id"
            class="post-card study-glass-block"
            @click="openDetail(post)"
          >
            <div class="post-card-top">
              <el-avatar :size="40" :src="avatarUrl(post.authorAvatar)">{{ (post.authorNickname || '')[0] }}</el-avatar>
              <div class="post-card-meta">
                <span class="post-author">{{ post.authorNickname }}</span>
                <span class="post-time">{{ formatTime(post.createTime) }}</span>
              </div>
              <el-tag size="small" type="warning">闲置</el-tag>
              <el-tag size="small" type="success">{{ tradeLabel(post.tradeStatus) }}</el-tag>
            </div>
            <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
            <p class="post-excerpt">{{ truncate(post.content, 160) }}</p>
            <div v-if="post.images?.length" class="post-images">
              <el-image
                v-for="(img, i) in post.images.slice(0, 3)"
                :key="i"
                :src="plazaImg(img)"
                fit="cover"
                class="thumb"
              />
            </div>
            <div v-if="post.priceDisplay" class="post-price">¥ {{ post.priceDisplay }}</div>
            <div class="post-stats" @click.stop>
              <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
              <span><el-icon><Pointer /></el-icon> {{ post.likeCount }}</span>
              <span><el-icon><Star /></el-icon> {{ post.favoriteCount }}</span>
            </div>
          </article>
          <div v-if="feed.MARKETPLACE.total > feed.MARKETPLACE.posts.length" class="plaza-more">
            <el-button
              @click="loadMore('MARKETPLACE')"
              :loading="loadingMoreCat === 'MARKETPLACE'"
            >
              加载更多
            </el-button>
          </div>
        </div>
      </section>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="detailVisible"
      :title="detail?.title || '帖子详情'"
      size="min(520px, 90vw)"
      class="plaza-drawer plaza-detail-light"
      append-to-body
    >
      <template v-if="detail">
        <div class="detail-head">
          <el-avatar :size="44" :src="avatarUrl(detail.authorAvatar)">{{ (detail.authorNickname || '')[0] }}</el-avatar>
          <div>
            <div class="detail-author">{{ detail.authorNickname }}</div>
            <div class="detail-time">{{ formatTime(detail.createTime) }}</div>
          </div>
        </div>
        <div class="detail-body">
          <p class="detail-content">{{ detail.content }}</p>
          <div v-if="detail.images?.length" class="detail-images">
            <el-image
              v-for="(img, i) in detail.images"
              :key="i"
              :src="plazaImg(img)"
              fit="contain"
              :preview-src-list="detail.images.map(plazaImg)"
              :initial-index="i"
              class="detail-img"
            />
          </div>
          <div v-if="detail.category === 'MARKETPLACE'" class="detail-trade">
            <span class="price">¥ {{ detail.priceDisplay }}</span>
            <el-tag>{{ tradeLabel(detail.tradeStatus) }}</el-tag>
            <el-select
              v-if="isMine(detail) && !isAdmin"
              v-model="tradeStatusEdit"
              size="small"
              style="width: 120px"
              @change="onTradeStatusChange"
            >
              <el-option label="在售" value="ON_SALE" />
              <el-option label="已预订" value="RESERVED" />
              <el-option label="已出" value="SOLD" />
            </el-select>
          </div>
        </div>
        <div class="detail-actions" v-if="!isAdmin">
          <el-button :type="detail.liked ? 'primary' : 'default'" @click="doLike">
            <el-icon><Pointer /></el-icon>
            {{ detail.liked ? '已赞' : '点赞' }} {{ detail.likeCount }}
          </el-button>
          <el-button :type="detail.favorited ? 'primary' : 'default'" plain @click="doFavorite">
            <el-icon><Star /></el-icon>
            {{ detail.favorited ? '已收藏' : '收藏' }} {{ detail.favoriteCount }}
          </el-button>
          <el-button v-if="!isMine(detail)" type="primary" plain @click="openPrivateChat(detail.userId)">私聊</el-button>
          <el-button v-if="isMine(detail)" type="danger" plain @click="deletePost">删除</el-button>
        </div>

        <div class="comments-section">
          <h4>评论</h4>
          <div v-if="!isAdmin" class="comment-input">
            <el-input v-model="commentText" type="textarea" :rows="2" placeholder="写评论…" />
            <el-button type="primary" size="small" style="margin-top: 8px" @click="submitComment()">发送</el-button>
          </div>
          <div v-for="row in flatComments" :key="row.id" class="comment-item" :style="{ paddingLeft: `${12 + row.depth * 16}px` }">
            <div class="comment-line">
              <strong>{{ row.authorNickname }}</strong>
              <span v-if="row.replyToNickname" class="reply-at">回复 @{{ row.replyToNickname }}</span>
              <span class="comment-time">{{ formatTime(row.createTime) }}</span>
            </div>
            <p class="comment-text">{{ row.content }}</p>
            <div v-if="!isAdmin" class="comment-actions">
              <el-button link type="primary" size="small" @click="replyTo(row)">回复</el-button>
              <el-button v-if="isMineId(row.userId)" link type="danger" size="small" @click="removeComment(row.id)">删除</el-button>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- 回复条 -->
    <div v-if="replyTarget && detailVisible" class="reply-bar">
      <span>回复 @{{ replyTarget.authorNickname }}</span>
      <el-button link @click="replyTarget = null">取消</el-button>
    </div>

    <!-- 发布 -->
    <el-dialog
      v-model="publishVisible"
      title="发布"
      width="520px"
      destroy-on-close
      class="plaza-form-light"
      append-to-body
      align-center
      @closed="resetPublish"
    >
      <el-form label-position="top">
        <el-form-item label="类型">
          <el-radio-group v-model="pub.category">
            <el-radio label="DISCUSSION">校园交流</el-radio>
            <el-radio label="MARKETPLACE">闲置出售</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="pub.category === 'MARKETPLACE'" label="标题">
          <el-input v-model="pub.title" placeholder="例如：九成新台灯" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="pub.content" type="textarea" :rows="5" placeholder="描述详情，可附多张图" />
        </el-form-item>
        <el-form-item v-if="pub.category === 'MARKETPLACE'" label="价格（元）">
          <el-input-number v-model="pub.priceYuan" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            multiple
            :http-request="onUploadImage"
            :show-file-list="false"
            accept="image/*"
          >
            <el-button size="small">上传图片</el-button>
          </el-upload>
          <div class="pub-images">
            <div v-for="(p, i) in pub.images" :key="i" class="pub-img-wrap">
              <el-image :src="plazaImg(p)" fit="cover" class="pub-thumb" />
              <el-icon class="pub-img-x" @click="pub.images.splice(i, 1)"><Close /></el-icon>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="submitPublish">发布</el-button>
      </template>
    </el-dialog>

    <!-- 私信：会话列表 + 窗口 -->
    <el-drawer v-model="chatListVisible" title="私信" size="320px" direction="rtl" class="plaza-chat-drawer" append-to-body>
      <div v-if="!isAdmin">
        <el-button type="primary" link @click="chatListVisible = false">关闭</el-button>
        <div v-if="partners.length === 0" class="muted">暂无会话，在帖子详情中点击「私聊」开始。</div>
        <div
          v-for="p in partners"
          :key="p.userId"
          class="partner-row"
          @click="openPrivateChat(p.userId)"
        >
          <el-avatar :size="36" :src="avatarUrl(p.avatar)">{{ (p.nickname || '')[0] }}</el-avatar>
          <div class="partner-info">
            <div class="partner-name">{{ p.nickname }}</div>
            <div class="partner-preview">{{ p.lastPreview }}</div>
          </div>
          <el-badge v-if="p.unreadCount > 0" :value="p.unreadCount" />
        </div>
      </div>
    </el-drawer>

    <el-drawer
      v-model="chatVisible"
      :title="chatTitle"
      size="min(420px, 100%)"
      direction="rtl"
      class="chat-drawer plaza-chat-drawer"
      append-to-body
    >
      <div class="chat-toolbar">
        <el-upload v-if="!isAdmin" :http-request="onChatUpload" :show-file-list="false" accept="image/*">
          <el-button size="small">图片</el-button>
        </el-upload>
      </div>
      <div ref="chatScrollRef" class="chat-messages">
        <div
          v-for="m in chatMessages"
          :key="m.id"
          :class="['chat-bubble', m.mine ? 'mine' : '']"
        >
          <div class="chat-meta">{{ formatTime(m.createTime) }}</div>
          <div v-if="m.content">{{ m.content }}</div>
          <el-image v-if="m.imagePath" :src="plazaImg(m.imagePath)" fit="contain" class="chat-img" />
        </div>
      </div>
      <div v-if="!isAdmin" class="chat-input">
        <el-input v-model="chatInput" type="textarea" :rows="2" placeholder="输入消息…" @keydown.enter.exact.prevent="sendChat" />
        <el-button type="primary" :loading="sendingChat" @click="sendChat">发送</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Pointer, Star, Close } from '@element-plus/icons-vue'
import { plazaApi, getAvatarUrl, getPlazaImageUrl } from '../api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadingMoreCat = ref(null)

const feed = reactive({
  DISCUSSION: { posts: [], total: 0, page: 1 },
  MARKETPLACE: { posts: [], total: 0, page: 1 },
})

const plazaMode = computed(() => route.meta.plazaMode || 'all')

const plazaPageTitle = computed(() => {
  if (plazaMode.value === 'discussion') return '帖子交流'
  if (plazaMode.value === 'market') return '闲置购物'
  return '交流广场'
})

const plazaPageSub = computed(() => {
  if (plazaMode.value === 'discussion') {
    return '浏览与发布校园讨论帖；评论、点赞与收藏。侧栏「闲置购物」可查看二手；「私信」可查看会话。'
  }
  if (plazaMode.value === 'market') {
    return '浏览与发布闲置物品，支持标价与交易状态；可与买家私聊。侧栏「帖子交流」可参与讨论。'
  }
  return '校园交流、闲置标价与交易状态、评论回复、点赞收藏；可与同学发起私信。'
})

const showDiscussionSection = computed(() => plazaMode.value !== 'market')
const showMarketSection = computed(() => plazaMode.value !== 'discussion')

const isAdmin = computed(() => {
  try {
    const u = JSON.parse(localStorage.getItem('user') || '{}')
    return u.role === 'ADMIN'
  } catch {
    return false
  }
})

const currentUserId = computed(() => {
  try {
    const u = JSON.parse(localStorage.getItem('user') || '{}')
    return u.userId
  } catch {
    return null
  }
})

function avatarUrl(path) {
  return getAvatarUrl(path)
}
function plazaImg(path) {
  return getPlazaImageUrl(path)
}

function truncate(s, n) {
  if (!s) return ''
  return s.length > n ? s.slice(0, n) + '…' : s
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function tradeLabel(s) {
  const m = { NA: '—', ON_SALE: '在售', SOLD: '已出', RESERVED: '已预订' }
  return m[s] || s
}

async function fetchCategory(cat, reset) {
  const f = feed[cat]
  if (reset) {
    f.page = 1
    f.posts = []
  }
  const data = await plazaApi.postPage({
    page: f.page,
    size: 10,
    category: cat,
  })
  f.total = data.total || 0
  const records = data.records || []
  if (reset) f.posts = records
  else f.posts = [...f.posts, ...records]
}

async function loadInitial() {
  loading.value = true
  try {
    const m = plazaMode.value
    if (m === 'discussion') {
      await fetchCategory('DISCUSSION', true)
    } else if (m === 'market') {
      await fetchCategory('MARKETPLACE', true)
    } else {
      await Promise.all([fetchCategory('DISCUSSION', true), fetchCategory('MARKETPLACE', true)])
    }
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore(cat) {
  const f = feed[cat]
  if (f.posts.length >= f.total) return
  f.page += 1
  loadingMoreCat.value = cat
  try {
    await fetchCategory(cat, false)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
    f.page -= 1
  } finally {
    loadingMoreCat.value = null
  }
}

const detailVisible = ref(false)
const detail = ref(null)
const commentText = ref('')
const comments = ref([])
const replyTarget = ref(null)
const tradeStatusEdit = ref('ON_SALE')

const flatComments = computed(() => {
  function flatten(nodes, depth = 0) {
    const out = []
    for (const n of nodes || []) {
      out.push({ ...n, depth })
      if (n.children?.length) out.push(...flatten(n.children, depth + 1))
    }
    return out
  }
  return flatten(comments.value)
})

async function openDetail(post) {
  detailVisible.value = true
  try {
    detail.value = await plazaApi.postGet(post.id)
    tradeStatusEdit.value = detail.value.tradeStatus
    comments.value = await plazaApi.comments(post.id)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

async function doLike() {
  if (isAdmin.value) return
  try {
    await plazaApi.toggleLike(detail.value.id)
    detail.value = await plazaApi.postGet(detail.value.id)
    syncPostInList(detail.value)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function doFavorite() {
  if (isAdmin.value) return
  try {
    await plazaApi.toggleFavorite(detail.value.id)
    detail.value = await plazaApi.postGet(detail.value.id)
    syncPostInList(detail.value)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

function syncPostInList(p) {
  for (const cat of ['DISCUSSION', 'MARKETPLACE']) {
    const arr = feed[cat].posts
    const i = arr.findIndex((x) => x.id === p.id)
    if (i >= 0) arr[i] = { ...arr[i], ...p }
  }
}

function isMine(p) {
  return currentUserId.value != null && p.userId === currentUserId.value
}

function isMineId(uid) {
  return currentUserId.value != null && uid === currentUserId.value
}

async function deletePost() {
  try {
    await ElMessageBox.confirm('确定删除该帖？', '提示', { type: 'warning' })
    await plazaApi.postDelete(detail.value.id)
    ElMessage.success('已删除')
    detailVisible.value = false
    loadInitial()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

async function onTradeStatusChange() {
  try {
    detail.value = await plazaApi.updateTrade(detail.value.id, tradeStatusEdit.value)
    syncPostInList(detail.value)
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  }
}

function replyTo(row) {
  replyTarget.value = row
  commentText.value = ''
}

async function submitComment() {
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  const body = { content: commentText.value.trim() }
  if (replyTarget.value) {
    body.parentId = replyTarget.value.id
    body.replyToUserId = replyTarget.value.userId
  }
  try {
    await plazaApi.addComment(detail.value.id, body)
    commentText.value = ''
    replyTarget.value = null
    comments.value = await plazaApi.comments(detail.value.id)
    detail.value = await plazaApi.postGet(detail.value.id)
    syncPostInList(detail.value)
  } catch (e) {
    ElMessage.error(e.message || '评论失败')
  }
}

async function removeComment(id) {
  try {
    await plazaApi.deleteComment(id)
    comments.value = await plazaApi.comments(detail.value.id)
    detail.value = await plazaApi.postGet(detail.value.id)
    syncPostInList(detail.value)
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

const publishVisible = ref(false)
const publishing = ref(false)
const pub = reactive({
  category: 'DISCUSSION',
  title: '',
  content: '',
  priceYuan: 1,
  images: [],
})

function openPublish(cat) {
  if (isAdmin.value) return
  pub.category = cat || 'DISCUSSION'
  publishVisible.value = true
}

function resetPublish() {
  pub.category = 'DISCUSSION'
  pub.title = ''
  pub.content = ''
  pub.priceYuan = 1
  pub.images = []
}

async function onUploadImage({ file }) {
  try {
    const path = await plazaApi.upload(file)
    pub.images.push(path)
    ElMessage.success('已上传')
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  }
}

async function submitPublish() {
  if (!pub.content.trim()) {
    ElMessage.warning('请填写正文')
    return
  }
  if (pub.category === 'MARKETPLACE' && !pub.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  publishing.value = true
  try {
    const body = {
      category: pub.category,
      title: pub.title.trim(),
      content: pub.content.trim(),
      images: pub.images,
    }
    if (pub.category === 'MARKETPLACE') {
      body.priceYuan = pub.priceYuan
    }
    await plazaApi.postCreate(body)
    ElMessage.success('发布成功')
    publishVisible.value = false
    loadInitial()
  } catch (e) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

const chatListVisible = ref(false)
const chatVisible = ref(false)
const partners = ref([])
const chatTargetId = ref(null)
const chatTitle = ref('私信')
const chatMessages = ref([])
const chatInput = ref('')
const sendingChat = ref(false)
const chatScrollRef = ref(null)

async function openChatList() {
  chatListVisible.value = true
  try {
    partners.value = await plazaApi.chatPartners()
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

async function openPrivateChat(userId) {
  if (isAdmin.value) return
  chatTargetId.value = userId
  const u = partners.value.find((p) => p.userId === userId)
  chatTitle.value = u ? `与 ${u.nickname} 对话` : '私信'
  chatVisible.value = true
  chatListVisible.value = false
  await loadChat()
}

async function loadChat() {
  if (!chatTargetId.value) return
  try {
    const data = await plazaApi.messages({ withUserId: chatTargetId.value, page: 1, size: 50 })
    chatMessages.value = data.records || []
    await nextTick()
    scrollChatBottom()
  } catch (e) {
    ElMessage.error(e.message || '加载消息失败')
  }
}

function scrollChatBottom() {
  const el = chatScrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function onChatUpload({ file }) {
  try {
    const path = await plazaApi.upload(file)
    await plazaApi.sendMessage({ toUserId: chatTargetId.value, imagePath: path })
    await loadChat()
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  }
}

async function sendChat() {
  if (!chatInput.value.trim()) return
  sendingChat.value = true
  try {
    await plazaApi.sendMessage({ toUserId: chatTargetId.value, content: chatInput.value.trim() })
    chatInput.value = ''
    await loadChat()
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    sendingChat.value = false
  }
}

function maybeOpenChatFromRoute() {
  if (route.query.openChat === '1' && !isAdmin.value) {
    openChatList()
    router.replace({ path: route.path, query: {} })
  }
}

watch(
  () => route.query.openChat,
  () => {
    if (route.query.openChat === '1') maybeOpenChatFromRoute()
  },
)

watch(
  () => route.meta.plazaMode,
  () => {
    loadInitial()
  },
)

onMounted(() => {
  loadInitial()
  maybeOpenChatFromRoute()
})
</script>

<style scoped>
.plaza-page {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 20px clamp(16px, 3vw, 40px) 80px;
}

.plaza-wrap {
  max-width: 720px;
  margin: 0 auto;
}

.plaza-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.plaza-head h1 {
  margin: 0 0 8px;
  font-size: 1.5rem;
  color: #f8fafc;
}

.plaza-sub {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: rgba(203, 213, 225, 0.92);
  max-width: 52em;
}

.plaza-head-actions {
  display: flex;
  gap: 8px;
}

.plaza-section {
  margin-bottom: 28px;
}

.plaza-section-chat {
  margin-bottom: 24px;
}

.section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px 16px;
  margin-bottom: 12px;
}

.section-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 600;
  color: #f8fafc;
}

.section-desc {
  margin: 4px 0 0;
  width: 100%;
  font-size: 13px;
  color: rgba(148, 163, 184, 0.95);
  line-height: 1.5;
}

.chat-zone-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px 18px;
  padding: 18px 20px;
  border-radius: 16px;
}

.chat-zone-icon {
  color: #38bdf8;
  flex-shrink: 0;
}

.chat-zone-text {
  flex: 1;
  min-width: 200px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-zone-lead {
  font-size: 15px;
  font-weight: 600;
  color: #f1f5f9;
}

.chat-zone-hint {
  font-size: 12px;
  color: #94a3b8;
}

.plaza-feed {
  min-height: 200px;
}

.post-card {
  padding: 16px;
  margin-bottom: 14px;
  border-radius: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.post-card:hover {
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.35);
}

.post-card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.post-card-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.post-author {
  font-weight: 600;
  color: #f1f5f9;
}

.post-time {
  font-size: 12px;
  color: #94a3b8;
}

.post-title {
  margin: 0 0 8px;
  font-size: 16px;
  color: #e2e8f0;
}

.post-excerpt {
  margin: 0 0 8px;
  font-size: 14px;
  color: #cbd5e1;
  line-height: 1.5;
  white-space: pre-wrap;
}

.post-images {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.thumb {
  width: 72px;
  height: 72px;
  border-radius: 8px;
}

.post-price {
  font-size: 18px;
  font-weight: 600;
  color: #fbbf24;
  margin-bottom: 8px;
}

.post-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
  color: #94a3b8;
}

.post-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.plaza-more {
  text-align: center;
  padding: 16px;
}

.detail-head {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-author {
  font-weight: 600;
  color: #f1f5f9;
}

.detail-time {
  font-size: 12px;
  color: #94a3b8;
}

.detail-content {
  white-space: pre-wrap;
  color: #e2e8f0;
  line-height: 1.6;
}

.detail-images {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.detail-img {
  max-height: 260px;
  border-radius: 8px;
}

.detail-trade {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-trade .price {
  font-size: 22px;
  font-weight: 700;
  color: #fbbf24;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;
}

.comments-section h4 {
  margin: 16px 0 8px;
  color: #e2e8f0;
}

.comment-item {
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.comment-line {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.reply-at {
  margin-left: 4px;
  color: #38bdf8;
}

.comment-text {
  margin: 0;
  color: #e5e7eb;
  font-size: 14px;
  white-space: pre-wrap;
}

.comment-actions {
  margin-top: 4px;
}

.reply-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  background: var(--glass-95);
  padding: 8px 16px;
  border-radius: 8px;
  z-index: 3000;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #e5e7eb;
}

.pub-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.pub-img-wrap {
  position: relative;
}

.pub-thumb {
  width: 80px;
  height: 80px;
  border-radius: 8px;
}

.pub-img-x {
  position: absolute;
  top: -4px;
  right: -4px;
  cursor: pointer;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
}

.partner-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 6px;
}

.partner-row:hover {
  background: rgba(255, 255, 255, 0.06);
}

.partner-info {
  flex: 1;
  min-width: 0;
}

.partner-name {
  font-weight: 500;
  color: #f1f5f9;
}

.partner-preview {
  font-size: 12px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted {
  color: #94a3b8;
  font-size: 13px;
}

.chat-drawer .chat-messages {
  height: calc(100vh - 220px);
  overflow-y: auto;
  padding: 8px 0;
}

.chat-bubble {
  max-width: 85%;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  margin-bottom: 10px;
  color: #e5e7eb;
  font-size: 14px;
}

.chat-bubble.mine {
  margin-left: auto;
  background: rgba(56, 189, 248, 0.2);
  border: 1px solid rgba(56, 189, 248, 0.35);
}

.chat-meta {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.chat-img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  margin-top: 6px;
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.detail-body {
  margin-bottom: 12px;
}
</style>

<style>
/* 发布弹窗：浅色底 + 深色字（与 body 上学习区全局样式解耦，弹层挂到 body） */
.plaza-form-light.el-dialog {
  --el-dialog-bg-color: #ffffff;
}
.plaza-form-light .el-dialog__header {
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}
.plaza-form-light .el-dialog__title {
  color: var(--neon-bg-deep) !important;
}
.plaza-form-light .el-form-item__label {
  color: #334155 !important;
}
.plaza-form-light .el-input__wrapper,
.plaza-form-light .el-textarea__inner {
  background: #ffffff !important;
  box-shadow: 0 0 0 1px #cbd5e1 inset !important;
}
.plaza-form-light .el-input__inner,
.plaza-form-light .el-textarea__inner {
  color: var(--neon-bg-deep) !important;
}
.plaza-form-light .el-radio__label {
  color: #334155 !important;
}
.plaza-form-light .el-input-number {
  width: 100%;
}

/* 帖子详情抽屉：深色内容区，保证正文与标签对比度 */
.plaza-detail-light.el-drawer .el-drawer__header {
  background: #0f172b;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 0;
}
.plaza-detail-light.el-drawer .el-drawer__title {
  color: #f1f5f9 !important;
}
.plaza-detail-light.el-drawer .el-drawer__body {
  background: #0f172b;
  color: #e2e8f0;
}

/* 私信抽屉 */
.plaza-chat-drawer.el-drawer .el-drawer__header {
  background: #0f172b;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.plaza-chat-drawer.el-drawer .el-drawer__title {
  color: #f1f5f9 !important;
}
.plaza-chat-drawer.el-drawer .el-drawer__body {
  background: #0f172b;
  color: #e5e7eb;
}
</style>
