import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './styles.css';
// import vRenderMarkdown from './directives/vRenderMarkdown.js';

const app = createApp(App);
app.use(router);
// app.directive('render-markdown', vRenderMarkdown);
app.mount('#app');

