import request from '@/utils/request'

export const imageApi = {
  list(params) {
    return request.get('/images/list', { params })
  },
  upload(data) {
    return request.post('/images/upload', data)
  },
  remove(id) {
    return request.delete(`/images/${id}`)
  },
}
