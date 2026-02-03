/**
 * API 测试工具
 * 用于在浏览器控制台测试 API 调用
 */

import { login, logout, getCurrentUser } from './api/modules/auth'
import { listDevices } from './api/modules/device'
import { setToken, getToken, removeToken } from './utils/token'

// 将测试函数挂载到 window 对象，方便在控制台调用
window.apiTest = {
    // 测试登录
    async testLogin(username = 'admin', password = 'admin123') {
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

    // 测试获取当前用户信息
    async testGetCurrentUser() {
        console.log('👤 测试获取当前用户信息...')

        const token = getToken()
        if (!token) {
            console.warn('⚠️ 未找到 token，请先登录')
            return
        }

        try {
            const user = await getCurrentUser()
            console.log('✅ 获取用户信息成功!')
            console.log('用户数据:', user)
            return user
        } catch (error) {
            console.error('❌ 获取用户信息失败:', error.message)
            throw error
        }
    },

    // 测试登出
    async testLogout() {
        console.log('🚪 测试登出...')

        try {
            await logout()
            removeToken()
            console.log('✅ 登出成功!')
        } catch (error) {
            console.error('❌ 登出失败:', error.message)
            // 即使 API 调用失败，也清除本地 token
            removeToken()
            console.log('✅ 本地 token 已清除')
        }
    },

    // 查看当前 token
    showToken() {
        const token = getToken()
        if (token) {
            console.log('🔑 当前 Token:', token)
        } else {
            console.log('⚠️ 未找到 Token')
        }
        return token
    },

    // 清除 token
    clearToken() {
        removeToken()
        console.log('🗑️ Token 已清除')
    },

    // 测试获取设备列表
    async testListDevices() {
        console.log('📱 测试获取设备列表...')

        const token = getToken()
        if (!token) {
            console.warn('⚠️ 未找到 token，请先登录')
            return
        }

        try {
            const devices = await listDevices()
            console.log('✅ 获取设备列表成功!')
            console.log('设备数据:', devices)
            return devices
        } catch (error) {
            console.error('❌ 获取设备列表失败:', error.message)
            throw error
        }
    },

    // 完整测试流程
    async runFullTest() {
        console.log('🚀 开始完整测试流程...\n')

        try {
            // 1. 清除旧 token
            console.log('1️⃣ 清除旧 token')
            this.clearToken()
            console.log('')

            // 2. 测试登录
            console.log('2️⃣ 测试登录')
            await this.testLogin()
            console.log('')

            // 3. 查看 token
            console.log('3️⃣ 查看保存的 token')
            this.showToken()
            console.log('')

            // 4. 测试获取用户信息
            console.log('4️⃣ 测试获取用户信息')
            await this.testGetCurrentUser()
            console.log('')

            // 5. 测试获取设备列表
            console.log('5️⃣ 测试获取设备列表')
            await this.testListDevices()
            console.log('')

            // 6. 测试登出
            console.log('6️⃣ 测试登出')
            await this.testLogout()
            console.log('')

            console.log('✅ 完整测试流程执行成功!')
        } catch (error) {
            console.error('❌ 测试流程失败:', error)
        }
    }
}

console.log('📋 API 测试工具已加载!')
console.log('使用方法:')
console.log('  apiTest.testLogin("username", "password")  - 测试登录')
console.log('  apiTest.testGetCurrentUser()               - 测试获取用户信息')
console.log('  apiTest.testLogout()                       - 测试登出')
console.log('  apiTest.showToken()                        - 查看当前 token')
console.log('  apiTest.clearToken()                       - 清除 token')
console.log('  apiTest.runFullTest()                      - 运行完整测试流程')
