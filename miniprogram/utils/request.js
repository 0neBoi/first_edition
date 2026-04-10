const app = getApp()
function request(options) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      success(res) {
        const d = res.data
        if (d && d.code === 200) {
          resolve(d.data)
        } else {
          reject(new Error(d && d.msg ? d.msg : '请求失败'))
        }
      },
      fail: reject
    })
  })
}

module.exports = {
  get: (url, data) => request({ url, method: 'GET', data }),
  post: (url, data) => request({ url, method: 'POST', data }),
  del: (url) => request({ url, method: 'DELETE' })
}
