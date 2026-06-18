import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录', public: true },
    },
    {
      path: '/',
      component: () => import('@/components/Layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '驾驶舱', icon: 'Odometer' },
        },
        {
          path: 'project',
          name: 'ProjectList',
          component: () => import('@/views/project/ProjectListView.vue'),
          meta: { title: '数据管理', icon: 'FolderOpened' },
        },
        {
          path: 'project/create',
          name: 'ProjectCreate',
          component: () => import('@/views/project/ProjectCreateView.vue'),
          meta: { title: '创建立项', hidden: true },
        },
        {
          path: 'project/:id/detail',
          name: 'ProjectDetail',
          component: () => import('@/views/project/ProjectDetailView.vue'),
          meta: { title: '项目详情', hidden: true },
        },
        {
          path: 'inspection/:projectId/entry',
          name: 'InspectionEntry',
          component: () => import('@/views/inspection/InspectionEntryView.vue'),
          meta: { title: '检测数据录入', icon: 'EditPen', hidden: true },
        },
        {
          path: 'process-card',
          name: 'ProcessCard',
          component: () => import('@/views/process/ProcessCardView.vue'),
          meta: { title: '工艺卡管理', icon: 'Document' },
        },
        {
          path: 'report',
          name: 'ReportList',
          component: () => import('@/views/report/ReportListView.vue'),
          meta: { title: '报告管理', icon: 'Document' },
        },
        {
          path: 'template',
          name: 'TemplateManage',
          component: () => import('@/views/report/TemplateManageView.vue'),
          meta: { title: '模板管理', icon: 'Notebook' },
        },
        {
          path: 'report/design/:id',
          name: 'ReportDesign',
          component: () => import('@/views/report/ReportDesignView.vue'),
          meta: { title: '报告设计', hidden: true },
        },
        {
          path: 'report/preview/:id',
          name: 'ReportPreview',
          component: () => import('@/views/report/ReportPreviewView.vue'),
          meta: { title: '报告预览', hidden: true },
        },
        {
          path: 'report/create/:inspectionId',
          name: 'ReportCreate',
          component: () => import('@/views/report/ReportCreateView.vue'),
          meta: { title: '生成报告', hidden: true },
        },
        {
          path: 'approval',
          name: 'Approval',
          component: () => import('@/views/approval/ApprovalView.vue'),
          meta: { title: '签字审批', icon: 'Edit' },
        },
        {
          path: 'user',
          name: 'UserList',
          component: () => import('@/views/user/UserListView.vue'),
          meta: { title: '用户管理', icon: 'User' },
        },
        {
          path: 'system',
          name: 'SystemSettings',
          redirect: '/system/dept',
          children: [
            {
              path: 'dept',
              name: 'DeptManage',
              component: () => import('@/views/system/DeptManageView.vue'),
              meta: { title: '组织架构', icon: 'OfficeBuilding' },
            },
            {
              path: 'role',
              name: 'RoleManage',
              component: () => import('@/views/system/RoleManageView.vue'),
              meta: { title: '角色管理', icon: 'UserFilled', hidden: true },
            },
            {
              path: 'images',
              name: 'ImageLibrary',
              component: () => import('@/views/system/ImageLibraryView.vue'),
              meta: { title: '图片库', icon: 'Picture', hidden: true },
            },
          ],
          meta: { title: '系统设置', icon: 'Setting' },
        },
      ],
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('@/views/error/403View.vue'),
      meta: { title: '无权限', public: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/404View.vue'),
      meta: { title: '页面不存在', public: true },
    },
  ],
})

// Navigation guard - JWT auth check
router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || ''} - SCJC NDT` || 'SCJC NDT 无损检测管理系统'

  const token = localStorage.getItem('token')

  if (to.meta.public) {
    // Public pages - redirect to dashboard if already logged in
    if (token && to.name === 'Login') {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  // Protected pages - require token
  if (!token) {
    next(`/login?redirect=${to.path}`)
    return
  }

  // Optional: role-based access control
  // const permissions = JSON.parse(localStorage.getItem('permissions') || '[]')
  // if (to.meta.roles && !to.meta.roles.some(r => permissions.includes(r))) {
  //   next('/403')
  //   return
  // }

  next()
})

export default router
