import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const permissions = ref(JSON.parse(localStorage.getItem('permissions') || '[]'))
  const projectIds = ref(JSON.parse(localStorage.getItem('projectIds') || '[]'))

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const realName = computed(() => userInfo.value?.realName || '')
  const roles = computed(() => userInfo.value?.roles || [])
  const deptName = computed(() => userInfo.value?.deptName || '')

  // Role checks
  const isSystemAdmin = computed(() => roles.value.includes('SYSTEM_ADMIN'))
  const isCompanyAdmin = computed(() => roles.value.includes('COMPANY_ADMIN'))
  const isBuAdmin = computed(() => roles.value.includes('BU_ADMIN'))
  const isProjectAdmin = computed(() => roles.value.includes('PROJECT_ADMIN'))
  const isTechnicalLeader = computed(() => roles.value.includes('TECHNICAL_LEADER'))
  const isProjectManagerRole = computed(() => roles.value.includes('PROJECT_MANAGER'))

  // Operations
  const canManageUsers = computed(() => isSystemAdmin.value || isCompanyAdmin.value || isBuAdmin.value)
  const canCreateProject = computed(() => isSystemAdmin.value || isBuAdmin.value)
  const canSign = computed(() => isTechnicalLeader.value || isProjectManagerRole.value)
  const canEditInspection = computed(() =>
    isSystemAdmin.value || isBuAdmin.value || isProjectAdmin.value
  )

  async function login(credentials) {
    const res = await authApi.login(credentials)
    const { token: t, userInfo: user } = res.data
    token.value = t
    userInfo.value = user
    permissions.value = user.roles || []
    projectIds.value = user.projectIds || []

    localStorage.setItem('token', t)
    localStorage.setItem('userInfo', JSON.stringify(user))
    localStorage.setItem('permissions', JSON.stringify(user.roles || []))
    localStorage.setItem('projectIds', JSON.stringify(user.projectIds || []))

    return res
  }

  async function fetchUserInfo() {
    const res = await authApi.me()
    userInfo.value = res.data
    permissions.value = res.data.roles || []
    projectIds.value = res.data.projectIds || []

    localStorage.setItem('userInfo', JSON.stringify(res.data))
    localStorage.setItem('permissions', JSON.stringify(res.data.roles || []))
    localStorage.setItem('projectIds', JSON.stringify(res.data.projectIds || []))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    projectIds.value = []

    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('permissions')
    localStorage.removeItem('projectIds')

    router.push('/login')
  }

  return {
    token,
    userInfo,
    permissions,
    projectIds,
    isLoggedIn,
    username,
    realName,
    roles,
    deptName,
    isSystemAdmin,
    isCompanyAdmin,
    isBuAdmin,
    isProjectAdmin,
    isTechnicalLeader,
    isProjectManagerRole,
    canManageUsers,
    canCreateProject,
    canSign,
    canEditInspection,
    login,
    fetchUserInfo,
    logout,
  }
})
