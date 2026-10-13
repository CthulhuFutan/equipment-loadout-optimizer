import axios from "axios";
import {ElMessage} from "element-plus";

const api = axios.create({
    // baseURL: 'http://localhost:8088/api',
    // 允许公共访问，便于移动端测试
    baseURL: import.meta.env.VITE_API_BASE || '/api',
    // 10s超时
    timeout: 10000,
})

// 请求拦截器
api.interceptors.request.use(config => {
        // 从 localStorage 取出登录时保存的 token
        const token = localStorage.getItem('token');
        if (token) {
            // 把 token 塞到请求头 Authorization 里，格式：Bearer <token>
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

// 响应拦截器
api.interceptors.response.use(
    response => {
        const body = response.data
        const url = response.config?.url ?? ''
        if (url.includes('/auth/')) return body
        if (body && typeof body.code === 'number' && body.code !== 200) {
            ElMessage.error(body.message || '操作失败')
            const err = new Error(body.message || '操作失败')
            ;(err as any).code = body.code
            return Promise.reject(err)
        }
        return body
    },
    error => {
        if (error.response) {
            const { status } = error.response;
            if (status === 401 || status === 403) {
                localStorage.removeItem('token');
                localStorage.removeItem('username');
                localStorage.removeItem('userId');
                ElMessage.error('登录已过期，请重新登录');
                window.location.href = '/login';
            } else {
                // 其他错误（403 无权限、500 服务器炸了等）
                ElMessage.error(error.response.data?.message || '请求失败');
            }
        } else {
            ElMessage.error('网络连接失败，请检查后端服务是否启动');
        }
        return Promise.reject(error);
    }
)

export default api
