Page({
  data: {
    list: []
  },
  onLoad() {},
  onShow() {
    this.loadList()
  },
  loadList() {
    const api = require('../../utils/request')
    api.get('/material/list').then(list => {
      this.setData({ list: list || [] })
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/material-detail/material-detail?id=${id}` })
  },
  goAsk() {
    wx.navigateTo({ url: '/pages/ask/ask' })
  }
})
