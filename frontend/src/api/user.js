import request from '@/utils/request'

export const userApi = {
  list(params) {
    return request.get('/users/list', { params })
  },
  getById(id) {
    return request.get(`/users/${id}`)
  },
  create(data) {
    return request.post('/users', data)
  },
  update(id, data) {
    return request.put(`/users/${id}`, data)
  },
  updateStatus(id, status) {
    return request.put(`/users/${id}/status`, { status })
  },
  assignRoles(id, roleIds) {
    return request.post(`/users/${id}/roles`, { roleIds })
  },
  assignProjects(id, projectIds) {
    return request.post(`/users/${id}/projects`, { projectIds })
  },
}
