import { createRouter, createWebHistory } from 'vue-router'
import AppointmentView from '@/views/AppointmentView.vue'
import CodeGenView from '@/views/CodeGenView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/appointment' },
    { path: '/appointment', name: 'appointment', component: AppointmentView },
    { path: '/codegen', name: 'codegen', component: CodeGenView },
  ],
})

export default router
