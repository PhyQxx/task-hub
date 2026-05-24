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
  ],
})

export default router
