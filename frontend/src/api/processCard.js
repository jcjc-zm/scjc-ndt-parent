import request from '@/utils/request'

export const processCardApi = {
  list(params) {
    return request.get('/process-cards/list', { params })
  },
  getById(id) {
    return request.get(`/process-cards/${id}`)
  },
  create(data) {
    return request.post('/process-cards', data)
  },
  update(id, data) {
    return request.put(`/process-cards/${id}`, data)
  },
  remove(id) {
    return request.delete(`/process-cards/${id}`)
  },
  batchImport(projectId, data) {
    return request.post('/process-cards/batch-import', data, { params: { projectId } })
  },
  exportData(params) {
    return request.get('/process-cards/export', { params, responseType: 'blob' })
  },
}
