import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './index.css'

const pinia = createPinia()

const app = createApp(App)

app.use(pinia)
app.use(router)

app.mount('#app')

// 开发环境：提供 API 测试工具
if (import.meta.env.DEV) {
    // 延迟加载，避免阻塞主应用
    setTimeout(async () => {
        try {
            const { login } = await import('./api/modules/auth.js')
            const { setToken, getToken, removeToken } = await import('./utils/token.js')

            // 挂载测试函数到 window
            window.apiTest = {
                async testLogin(username = 'admin', password = 'admin') {
                    console.log('🔐 测试登录...')
                    console.log('用户名:', username)
                    console.log('密码:', password)

                    try {
                        const response = await login({ username, password })
                        console.log('✅ 登录成功!')
                        console.log('响应数据:', response)

                        if (response.token) {
                            setToken(response.token)
                            console.log('✅ Token 已保存')
                        }

                        return response
                    } catch (error) {
                        console.error('❌ 登录失败:', error.message)
                        throw error
                    }
                },

                showToken() {
                    const token = getToken()
                    if (token) {
                        console.log('🔑 当前 Token:', token)
                    } else {
                        console.log('⚠️ 未找到 Token')
                    }
                    return token
                },

                clearToken() {
                    removeToken()
                    console.log('🗑️ Token 已清除')
                }
            }

            console.log('🔧 开发模式：API 测试工具已加载')
            console.log('💡 使用方法:')
            console.log('  apiTest.testLogin("username", "password")  - 测试登录')
            console.log('  apiTest.showToken()                        - 查看当前 token')
            console.log('  apiTest.clearToken()                       - 清除 token')
        } catch (error) {
            console.warn('⚠️ API 测试工具加载失败:', error.message)
        }
    }, 1000)
}
