import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getStoredUser } from '@/utils/auth'
import type { UserVO } from '@/types/api'
import AppointmentView from '@/views/AppointmentView.vue'
import CodeGenView from '@/views/CodeGenView.vue'
import LoginView from '@/views/LoginView.vue'
import AppListView from '@/views/AppListView.vue'
import VisualEditView from '@/views/VisualEditView.vue'
import AppChatView from '@/views/AppChatView.vue'
import AdminView from '@/views/AdminView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/apps' },
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/apps', name: 'apps', component: AppListView },
    { path: '/app/:id/chat', name: 'app-chat', component: AppChatView },
    { path: '/app/:id/edit', name: 'visual-edit', component: VisualEditView },
    { path: '/admin', name: 'admin', component: AdminView, meta: { admin: true } },
    { path: '/appointment', name: 'appointment', component: AppointmentView },
    { path: '/codegen', name: 'codegen', component: CodeGenView },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.public && !getToken()) {
    return '/login'
  }
  if (to.meta.admin) {
    const user = getStoredUser<UserVO>()
    if (!user || user.role !== 'ADMIN') {
      return '/apps'
    }
  }
})

export default router
