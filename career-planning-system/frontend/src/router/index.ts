import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import JobAnalysis from '../views/JobAnalysis.vue'
import StudentProfile from '../views/StudentProfile.vue'
import HomeView from '../views/HomeView.vue'
import CareerReport from '../views/CareerReport.vue'
import AccountManagement from '../views/AccountManagement.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: { requiresAuth: false, depth: 0 }
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true, depth: 1 }
    },
    {
      path: '/jobs',
      name: 'jobs',
      component: JobAnalysis,
      meta: { depth: 2 }
    },
    {
      path: '/jobs/detail',
      name: 'jobDetail',
      component: () => import('../views/JobDetailView.vue'),
      meta: { requiresAuth: true, depth: 3 }
    },
    {
      path: '/profile',
      name: 'profile',
      component: StudentProfile,
      meta: { requiresAuth: true, depth: 2 }
    },
    {
      path: '/report',
      name: 'report',
      component: CareerReport,
      meta: { requiresAuth: true, depth: 2 }
    },
    {
      path: '/account',
      name: 'account',
      component: AccountManagement,
      meta: { requiresAuth: true, depth: 2 }
    }
  ]
})

// 全局路由守卫
// TODO: 对接后端后恢复登录验证
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token');

  if (to.meta.requiresAuth) {
    if (!token) {
      next('/login');
    } else {
      next();
    }
  } else {
    if (to.path === '/login' && token) {
      next('/');
    } else {
      next();
    }
  }
});

export default router
