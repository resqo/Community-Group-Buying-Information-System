const GLOBAL_KEYS = ['token', 'userId', 'username', 'role', 'avatarUrl']

function keyFor(role) {
  return `session:${role}`
}

function readGlobalSession() {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  if (!token || !role) return null
  return {
    token,
    role,
    userId: localStorage.getItem('userId'),
    username: localStorage.getItem('username'),
    avatarUrl: localStorage.getItem('avatarUrl') || '',
  }
}

export function getSession(role) {
  if (role) {
    const saved = localStorage.getItem(keyFor(role))
    if (saved) {
      try {
        const session = JSON.parse(saved)
        if (session?.token) return session
      } catch {
        localStorage.removeItem(keyFor(role))
      }
    }
  }

  const globalSession = readGlobalSession()
  if (!role || globalSession?.role === role) return globalSession
  return null
}

export function applySession(session) {
  if (!session) return
  localStorage.setItem('token', session.token)
  localStorage.setItem('userId', session.userId)
  localStorage.setItem('username', session.username)
  localStorage.setItem('role', session.role)
  if (session.avatarUrl) localStorage.setItem('avatarUrl', session.avatarUrl)
  else localStorage.removeItem('avatarUrl')
}

export function saveSession(data) {
  const session = {
    token: data.token,
    userId: data.userId,
    username: data.username,
    role: data.role,
    avatarUrl: data.avatarUrl || '',
  }
  localStorage.setItem(keyFor(session.role), JSON.stringify(session))
  applySession(session)
  return session
}

export function clearSession(role) {
  if (role) localStorage.removeItem(keyFor(role))
  const currentRole = localStorage.getItem('role')
  if (!role || currentRole === role) {
    GLOBAL_KEYS.forEach((key) => localStorage.removeItem(key))
  }
}
