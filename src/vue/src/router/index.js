import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

function authUserId(user) {
  if (!user || typeof user !== 'object') return null
  return user.user_id != null ? user.user_id : user.id
}

function normRole(role) {
  return String(role || '').toLowerCase()
}

const routes = [
  {
    path: '/',
    name: 'Layout',
    component: Layout,
    redirect: () => {
      try {
        const u = JSON.parse(sessionStorage.getItem('user') || '{}')
        if (normRole(u.role) === 'community') return '/elderly-mgmt'
      } catch (e) { /* ignore */ }
      return '/person'
    },
    children: [
      {
        path: '/person',
        name: 'Person',
        component: () => import('@/views/Person.vue'),
        meta: { title: '个人信息' }
      },
      {
        path: '/elderly-mgmt',
        name: 'ElderlyMgmt',
        component: () => import('@/views/ElderlyMgmt.vue'),
        meta: { title: '老人监测', roles: ['community'] }
      },
      {
        path: '/elderly-archive',
        name: 'ElderlyArchive',
        component: () => import('@/views/ElderlyArchive.vue'),
        meta: { title: '老人档案', roles: ['community'] }
      },

      {
        path: '/alerts',
        name: 'Alerts',
        component: () => import('@/views/Alerts.vue'),
        meta: { title: '事件处理', roles: ['community'] }
      },
      {
        path: '/announcements',
        name: 'Announcements',
        component: () => import('@/views/Announcements.vue'),
        meta: { title: '公告' }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  const storageData = sessionStorage.getItem('user')
  const token = sessionStorage.getItem('token')
  let user = {}
  if (storageData) {
    try {
      user = JSON.parse(storageData)
    } catch (e) {
      user = {}
    }
  }

  if ((!authUserId(user) || !token) && to.path !== '/login' && to.path !== '/register') {
    next('/login')
    return
  }

  if (authUserId(user) && token && normRole(user.role) === 'family') {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
    next('/login')
    return
  }

  if (to.meta.roles && Array.isArray(to.meta.roles) && to.meta.roles.length > 0) {
    const r = normRole(user.role)
    const ok = to.meta.roles.some((x) => normRole(x) === r)
    if (!ok) {
      if (r === 'community') {
        next('/elderly-mgmt')
        return
      }
      next('/person')
      return
    }
  }

  next()
})

export default router
