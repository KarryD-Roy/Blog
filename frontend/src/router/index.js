import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import PostDetail from '../views/PostDetail.vue';
import Skills from '../views/Skills.vue';
import Search from '../views/Search.vue';
import PostList from '../views/PostList.vue';
import Statistics from '../views/Statistics.vue';
import AiWriter from '../views/AiWriter.vue';
import AiRecommendation from '../views/AiRecommendation.vue';
import TheoryDetail from '../views/TheoryDetail.vue';

const routes = [
  { path: '/', name: 'home', component: Home },
  { path: '/posts/:id', name: 'post-detail', component: PostDetail, props: true },
  { path: '/skills', name: 'skills', component: Skills },
  { path: '/search', name: 'search', component: Search },
  { path: '/posts', name: 'post-list', component: PostList },
  { path: '/statistics', name: 'statistics', component: Statistics },
  { path: '/ai/writer', name: 'ai-writer', component: AiWriter },
  { path: '/ai/recommendation', name: 'ai-recommendation', component: AiRecommendation },
  { path: '/theory/:skillId', name: 'theory-detail', component: TheoryDetail },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/auth/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/auth/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/user/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('../views/user/Messages.vue'),
    meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      return { top: 0 };
    }
  }
});

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  if (to.meta.requiresAuth && !token) {
    next({ name: 'login', query: { redirect: to.fullPath } });
  } else if (to.meta.guest && token) {
    next({ name: 'home' });
  } else {
    next();
  }
});

export default router;
