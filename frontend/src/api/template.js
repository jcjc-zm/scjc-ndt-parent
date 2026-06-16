import request from '@/utils/request'

export const templateApi = {
  list(params) {
    return request.get('/templates/list', { params })
  },
  getById(id) {
    return request.get(`/templates/${id}`)
  },
  create(data) {
    return request.post('/templates', data)
  },
  update(id, data) {
    return request.put(`/templates/${id}`, data)
  },
  remove(id) {
    return request.delete(`/templates/${id}`)
  },
}
