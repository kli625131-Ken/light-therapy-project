import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/subject',
      name: 'subject',
      component: () => import('../views/SubjectView.vue'),
      meta: { requiresAuth: true, role: 'subject' }
    },
    {
      path: '/researcher',
      name: 'researcher',
      component: () => import('../views/ResearcherView.vue'),
      meta: { requiresAuth: true, role: 'admin' }
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/ResearcherView.vue'),
      meta: { requiresAuth: true, role: 'admin' }
    }
  ]
})

// 路由守卫：在路由切换前检查认证状态
router.beforeEach(async (to, from, next) => {
  // 不需要认证的路由，直接放行
  if (!to.meta.requiresAuth) {
    next()
    return
  }

  // 延迟导入，避免在Pinia初始化前访问store
  const { useUserStore } = await import('../stores/useUserStore')
  const userStore = useUserStore()

  try {
    // 检查认证状态
    const isAuthenticated = await userStore.checkAuth()
    
    if (isAuthenticated) {
      // 检查角色是否匹配
      const userRole = userStore.role
      const requiredRole = to.meta.role
      
      if (requiredRole === 'admin') {
        // 管理员角色可以访问admin和researcher路由
        if (userRole === 'admin' || userRole === 'researcher') {
          next()
        } else {
          next('/login')
        }
      } else if (requiredRole === 'subject') {
        // 受试者只能访问subject路由
        if (userRole === 'subject') {
          next()
        } else {
          next('/login')
        }
      } else {
        next()
      }
    } else {
      // 未认证或认证失败，跳转到登录页面
      next('/login')
    }
  } catch (error) {
    console.error('路由守卫错误:', error)
    // 发生错误时跳转到登录页面
    next('/login')
  }
})

export default router
