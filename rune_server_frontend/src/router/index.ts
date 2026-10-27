import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from "@/views/MainLayout.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { noAuth: true }
    },
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('../views/Home.vue'),
        },

        // dragon
        {
          path: 'dragons',
          name: 'DragonList',
          component: () => import('../views/DragonList.vue'),
        },
        {
          path: 'dragons/new',
          name: 'DragonNew',
          component: () => import('../views/DragonForm.vue'),
        },
        {
          path: 'dragons/:id/edit',
          name: 'DragonEdit',
          component: () => import('../views/DragonForm.vue')
        },
        {
          path: 'dragons/:id',
          name: 'DragonDetail',
          component: () => import('../views/DragonDetail.vue'),
        },

        // rune
        {
          path: 'runes',
          name: 'RuneList',
          component: () => import('../views/RuneList.vue'),
        },
        {
          path: 'runes/new',
          name: 'RuneNew',
          component: () => import('../views/RuneForm.vue'),
        },
        {
          path: 'runes/:id/edit',
          name: 'RuneEdit',
          component: () => import('../views/RuneForm.vue'),
        },

        // runeEquipment
        {
          path: 'equipped',
          name: 'Equipped',
          component: () => import('../views/Equipped.vue'),
        },
        // optimize
        {
          path: 'optimizer',
          name: 'Optimizer',
          component: () => import('../views/Optimizer.vue'),
        }
      ]
    }
  ],
})
// 全局路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.noAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
