import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import PostDetail from '../views/PostDetail.vue';
import Skills from '../views/Skills.vue';
import Search from '../views/Search.vue';
import PostList from '../views/PostList.vue';
import Statistics from '../views/Statistics.vue';

const routes = [
  { path: '/', name: 'home', component: Home },
  { path: '/posts/:id', name: 'post-detail', component: PostDetail, props: true },
  { path: '/skills', name: 'skills', component: Skills },
  { path: '/search', name: 'search', component: Search },
  { path: '/posts', name: 'post-list', component: PostList },
  { path: '/statistics', name: 'statistics', component: Statistics }
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

export default router;
