/**
 * Axios HTTP 客户端配置
 * 包含请求/响应拦截器，统一处理 token 和响应数据
 */

import axios from 'axios'
import { getToken, removeToken } from '../utils/token'
import { useUserStore } from '../stores/useUserStore'

// 创建 axios 实例
const http = axios.create({
    // baseURL: import.meta.env.VITE_API_BASE_URL || 'http://192.168.0.15:8080',
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    timeout: 15000, // 15秒超时
    headers: {
        'Content-Type': 'application/json'
    },
    withCredentials: false // 暂时关闭凭证，解决CORS问题
})

// 请求拦截器：自动添加 token
http.interceptors.request.use(
    (config) => {
        // 从 Pinia store 获取当前角色
        const userStore = useUserStore()
        const currentRole = userStore.role
        
        // 根据当前角色获取对应的 token
        const token = getToken(currentRole)
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        
        console.log('📤 API 请求:', {
            url: config.url,
            method: config.method,
            headers: config.headers,
            data: config.data,
            role: currentRole
        })
        
        return config
    },
    (error) => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器：统一处理响应数据和错误
http.interceptors.response.use(
    (response) => {
        const { data } = response

        // 开发环境：打印响应数据
        if (import.meta.env.DEV) {
            console.log('📥 API 响应:', {
                url: response.config.url,
                status: response.status,
                data: data
            })
        }

        // 统一解包 {code, message, data} 结构
        if (data && typeof data === 'object') {
            const { code, message, data: responseData } = data

            // 检查是否有 code 字段
            if (code !== undefined) {
                // 成功响应 (code === 200 或 code === 0)
                if (code === 200 || code === 0) {
                    return responseData !== undefined ? responseData : data
                }

                // 业务错误
                const errorMsg = message || '请求失败'
                console.error('❌ 业务错误:', { code, message, data: responseData })
                return Promise.reject(new Error(errorMsg))
            }

            // 如果没有 code 字段，可能是直接返回数据对象
            // 检查是否有 token 字段（登录接口可能直接返回 {token, user}）
            if (data.token || data.user) {
                console.log('✅ 检测到直接返回的登录数据')
                return data
            }
        }

        // 如果响应格式不符合预期，直接返回
        return data
    },
    (error) => {
        // HTTP 错误处理
        if (error.response) {
            const { status, data } = error.response

            switch (status) {
                case 401:
                    // 未授权，清除 token 并跳转登录
                    console.error('未授权，请重新登录')
                    // 获取当前角色，用于清除对应的token
                    const userStore = useUserStore()
                    removeToken(userStore.role)
                    // 跳转到前端登录页面，使用正确的base路径
                    window.location.href = '/iotlight/#/login'
                    break
                case 403:
                    console.error('无权限访问')
                    break
                case 404:
                    console.error('请求的资源不存在')
                    break
                case 500:
                    console.error('服务器错误')
                    break
                default:
                    console.error(`请求失败: ${status}`)
            }

            // 尝试从响应中获取错误消息
            const errorMsg = data?.message || error.message || '网络请求失败'
            return Promise.reject(new Error(errorMsg))
        }

        // 网络错误或超时
        if (error.code === 'ECONNABORTED') {
            console.error('请求超时')
            return Promise.reject(new Error('请求超时，请稍后重试'))
        }

        console.error('网络错误:', error.message)
        return Promise.reject(error)
    }
)

export default http
