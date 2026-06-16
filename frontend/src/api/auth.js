import request from '@/utils/request'

export const authApi = {
  login(data) {
    return request.post('/auth/login', data)
  },
  me() {
    return request.get('/auth/me')
  },
}
