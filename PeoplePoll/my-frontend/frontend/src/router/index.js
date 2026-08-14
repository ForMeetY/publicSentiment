import { createRouter, createWebHistory } from 'vue-router'
// import HomePage from '@/view/home/index.vue'
/*
配置路由
*/
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'HomePage',
      component: () => import('@/view/home/index.vue'),
    },
    /* 其他访问跳转home */
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
    /* Broadcasting and Telecommunication User Profile路由 */
    {
      path: '/user-profile',
      name: 'UserProfile',
      component: () => import('@/view/project/userProfile/index.vue'),
    },
    /* Public Opinion路由 */
    {
      path: '/public-opinion',
      name: 'PublicOpinion',
      component: () => import('@/view/project/publicOpinion/index.vue'),
      children: [
        {
          path: '/public-opinion/analysis',
          name: 'PublicOpinionAnalysis',
          component: () => import('@/view/statistic/weiboTotalStatistic.vue'),

        },
        {
          path: '/public-opinion/forecast',
          name: 'SentimentAnalysis',
          component: () => import('@/view/statistic/SentimentStatistic.vue'),
        },
        {
          path: '/public-opinion/character',
          name: 'UserInfo',
          component: () => import('@/view/statistic/userInfo.vue'),
        }
      ],
    },
    /* User Profile路由 */
    {
      path: '/userProfileDetail',
      name: 'UserProfileDetail',
      component: () => import('@/view/statistic/layoutPage.vue'),
    },

  ],
})

export default router
