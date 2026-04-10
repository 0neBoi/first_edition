Page({
  data: {
    question: '',
    answer: '',
    loading: false
  },
  onInput(e) {
    this.setData({ question: e.detail.value })
  },
  ask() {
    const q = (this.data.question || '').trim()
    if (!q) {
      wx.showToast({ title: '请输入问题', icon: 'none' })
      return
    }
    this.setData({ loading: true, answer: '' })
    const api = require('../../utils/request')
    api.post('/qwen/ask?question=' + encodeURIComponent(q)).then(answer => {
      this.setData({ answer: answer || '', loading: false })
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '请求失败', icon: 'none' })
    })
  }
})
