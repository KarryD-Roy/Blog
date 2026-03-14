import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import PostDetail from '../views/PostDetail.vue';
import Skills from '../views/Skills.vue';

const routes = [
  { path: '/', name: 'home', component: Home },
  { path: '/posts/:id', name: 'post-detail', component: PostDetail, props: true },
  { path: '/skills', name: 'skills', component: Skills }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;

