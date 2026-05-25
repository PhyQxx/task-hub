import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/gantt' },
    { path: '/gantt', name: 'gantt', component: () => import('@/views/GanttView.vue') },
    { path: '/kanban', name: 'kanban', component: () => import('@/views/KanbanView.vue') },
    { path: '/swimlane', name: 'swimlane', component: () => import('@/views/SwimLaneView.vue') },
    { path: '/member', name: 'member', component: () => import('@/views/MemberView.vue') },
    { path: '/milestone', name: 'milestone', component: () => import('@/views/MilestoneView.vue') },
    { path: '/worklog', name: 'worklog', component: () => import('@/views/WorkLogView.vue') },
    // Admin Routes
    { path: '/admin/dashboard', name: 'admin-dashboard', component: () => import('@/views/AdminDashboard.vue') },
    { path: '/admin/projects', name: 'admin-project-manage', component: () => import('@/views/AdminProjectManage.vue') },
    { path: '/admin/tasks', name: 'admin-task-manage', component: () => import('@/views/AdminTaskManage.vue') },
    { path: '/admin/members', name: 'admin-member-manage', component: () => import('@/views/AdminMemberManage.vue') },
  ],
})

export default router
