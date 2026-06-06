import { reactive } from 'vue'

const notices = reactive([])
let nextId = 1

const titleByType = {
  success: '操作已完成',
  warning: '操作提醒',
  error: '操作未完成',
  info: '通知消息',
}

export function useNotifications() {
  return notices
}

export function dismissNotification(id) {
  const index = notices.findIndex((item) => item.id === id)
  if (index === -1) return
  const [notice] = notices.splice(index, 1)
  if (notice.timer) window.clearTimeout(notice.timer)
}

function pushNotification(type, message, options = {}) {
  const id = nextId++
  const notice = {
    id,
    type,
    title: options.title || titleByType[type] || titleByType.info,
    message,
    timer: null,
  }

  notices.unshift(notice)
  notice.timer = window.setTimeout(() => dismissNotification(id), options.duration ?? 3000)
  return id
}

export const notify = {
  success(message, options) {
    return pushNotification('success', message, options)
  },
  warning(message, options) {
    return pushNotification('warning', message, options)
  },
  error(message, options) {
    return pushNotification('error', message, options)
  },
  info(message, options) {
    return pushNotification('info', message, options)
  },
}
