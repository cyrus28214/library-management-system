import './assets/main.css'
import axios from 'axios'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import App from './App.vue'
import router from './router'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
axios.defaults.baseURL = 'http://localhost:8000';
axios.defaults.validateStatus = () => true; // allow all status codes, handle them in response.data.message
// axios.defaults.baseURL = 'http://127.0.0.1:4523/m1/5948597-0-default/'; // ApiFox local mock server
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')

