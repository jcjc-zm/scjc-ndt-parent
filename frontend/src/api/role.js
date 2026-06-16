import request from '@/utils/request'

export const roleApi = {
  list() {
    return request.get('/roles/list')
  },
}
