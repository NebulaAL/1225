export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch('/api' + path, { credentials: 'same-origin', ...options })
  } catch {
    throw new ApiError(0, '连接失败，请检查网络后重试')
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const message = body.message || ({
      401: '请重新登录',
      403: '当前操作未获授权，请刷新后重试',
      404: '这条数据已不存在',
    }[response.status] || '操作失败，请稍后重试')
    if (response.status === 401 && path !== '/auth/login') {
      window.dispatchEvent(new Event('session-expired'))
    }
    throw new ApiError(response.status, message)
  }
  return response.headers.get('content-type')?.includes('application/json') ? response.json() : null
}

export const get = path => request(path)

export async function write(path, method, value) {
  const { headerName, token } = await get('/auth/csrf')
  const headers = { [headerName]: token }
  let body
  if (value instanceof URLSearchParams) {
    body = value
  } else if (value !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(value)
  }
  return request(path, { method, headers, body })
}
