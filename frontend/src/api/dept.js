import request from '@/utils/request'

export const deptApi = {
  tree() {
    return request.get('/depts/tree')
  },
  list() {
    return request.get('/depts/list')
  },
  create(data) {
    return request.post('/depts', data)
  },
  update(id, data) {
    return request.put(`/depts/${id}`, data)
  },
  remove(id) {
    return request.delete(`/depts/${id}`)
  },
}
