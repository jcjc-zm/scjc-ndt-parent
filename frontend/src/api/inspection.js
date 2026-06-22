import request from '@/utils/request'

export const inspectionApi = {
  list(params) {
    return request.get('/inspections/list', { params })
  },
  getById(id) {
    return request.get(`/inspections/${id}`)
  },
  create(data) {
    return request.post('/inspections', data)
  },
  update(id, data) {
    return request.put(`/inspections/${id}`, data)
  },
  remove(id) {
    return request.delete(`/inspections/${id}`)
  },
  batchImport(projectId, data) {
    return request.post('/inspections/batch-import', data, { params: { projectId } })
  },
  exportData(params) {
    return request.get('/inspections/export', { params, responseType: 'blob' })
  },
}
