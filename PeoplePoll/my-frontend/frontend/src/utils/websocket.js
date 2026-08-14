// 作为未来的优化方向
import { ref, onUnmounted } from 'vue'

// 全局一份数据，所有组件共用
const socket = ref(null)
const message = ref(null)   // 存放后端推过来的最新数据
const isConnect = ref(false)

// 初始化连接
export function useWebSocket(url) {
  // 避免重复连接
  if (socket.value && socket.value.readyState === WebSocket.OPEN) {
    return { message, isConnect }
  }

  // 建立 ws 连接
  socket.value = new WebSocket(url)

  // 连接成功
  socket.value.onopen = () => {
    isConnect.value = true
    console.log('WebSocket 连接成功')
  }

  // 接收后端推送消息
  socket.value.onmessage = (e) => {
    // 把后端数据存起来
    message.value = JSON.parse(e.data)
  }

  // 连接关闭
  socket.value.onclose = () => {
    isConnect.value = false
    console.log('WebSocket 断开，准备重连...')
    // 简单重连 3秒后重试
    setTimeout(() => useWebSocket(url), 3000)
  }

  // 报错
  socket.value.onerror = (err) => {
    console.error('WebSocket 错误：', err)
    socket.value.close()
  }

  // 组件销毁时不关闭全局连接（多组件共用）
  onUnmounted(() => { })

  return { message, isConnect }
}
