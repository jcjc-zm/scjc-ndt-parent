import request from '@/utils/request'

export const projectApi = {
  list(params) {
    return request.get('/projects/list', { params })
  },
  getTree() {
    return request.get('/projects/tree')
  },
  getById(id) {
    return request.get(`/projects/${id}`)
  },
  create(data) {
    return request.post('/projects', data)
  },
  update(id, data) {
    return request.put(`/projects/${id}`, data)
  },
  updateStatus(id, status) {
    return request.put(`/projects/${id}/status`, { status })
  },
}
