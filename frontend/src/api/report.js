import request from '@/utils/request'

export const reportApi = {
  list(params) {
    return request.get('/reports/list', { params })
  },
  getById(id) {
    return request.get(`/reports/${id}`)
  },
  generate(data) {
    return request.post('/reports', data)
  },
  getPdf(id) {
    return request.get(`/reports/${id}/pdf`, { responseType: 'blob' })
  },
  remove(id) {
    return request.delete(`/reports/${id}`)
  },
}
