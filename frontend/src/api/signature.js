import request from '@/utils/request'

export const signatureApi = {
  pending() {
    return request.get('/signatures/pending')
  },
  sign(id, data) {
    return request.post(`/signatures/${id}/sign`, data)
  },
  history(id) {
    return request.get(`/signatures/${id}/history`)
  },
}
