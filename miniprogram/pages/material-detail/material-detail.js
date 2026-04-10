Page({
  data: {
    id: null,
    material: null,
    knowledgeList: [],
    questionList: [],
    extractLoading: false,
    genLoading: false,
    questionCount: 5
  },
  onLoad(opts) {
    const id = opts && opts.id
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      return
    }
    this.setData({ id })
    this.loadMaterial()
    this.loadKnowledge()
    this.loadQuestions()
  },
  loadMaterial() {
    const api = require('../../utils/request')
    api.get('/material/' + this.data.id).then(material => {
      this.setData({ material })
    }).catch(() => {})
  },
  loadKnowledge() {
    const api = require('../../utils/request')
    api.get('/knowledge/list', { materialId: this.data.id }).then(list => {
      this.setData({ knowledgeList: list || [] })
    }).catch(() => {})
  },
  loadQuestions() {
    const api = require('../../utils/request')
    api.get('/question/list', { materialId: this.data.id }).then(list => {
      this.setData({ questionList: list || [] })
    }).catch(() => {})
  },
  extractKnowledge() {
    this.setData({ extractLoading: true })
    const api = require('../../utils/request')
    const id = this.data.id
    api.post('/knowledge/extract?materialId=' + id).then(list => {
      this.setData({ knowledgeList: list || [], extractLoading: false })
      wx.showToast({ title: '已生成' })
    }).catch(() => {
      this.setData({ extractLoading: false })
      wx.showToast({ title: '失败', icon: 'none' })
    })
  },
  onCountChange(e) {
    const idx = parseInt(e.detail.value, 10)
    const count = [3, 4, 5, 6, 7, 8, 9, 10][idx] || 5
    this.setData({ questionCount: count })
  },
  generateQuestions() {
    this.setData({ genLoading: true })
    const api = require('../../utils/request')
    const id = this.data.id
    const count = this.data.questionCount
    api.post('/question/generate?materialId=' + id + '&count=' + count).then(list => {
      this.setData({ questionList: list || [], genLoading: false })
      wx.showToast({ title: '已生成' })
    }).catch(() => {
      this.setData({ genLoading: false })
      wx.showToast({ title: '失败', icon: 'none' })
    })
  },
  typeLabel(type) {
    const m = { single: '单选', multiple: '多选', fill: '填空', essay: '简答' }
    return m[type] || type
  }
})
