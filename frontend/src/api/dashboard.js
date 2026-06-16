import request from '@/utils/request'

export const dashboardApi = {
  overview() {
    return request.get('/dashboard/overview')
  },
  projectDistribution() {
    return request.get('/dashboard/project-distribution')
  },
  weeklyWorkload() {
    return request.get('/dashboard/weekly-workload')
  },
  inspectionStats() {
    return request.get('/dashboard/inspection-stats')
  },
  myTodos() {
    return request.get('/dashboard/my-todos')
  },
}
