/**
 * 认证相关 API
 */

import http from '../http'

/**
 * 用户登录
 * @param {Object} credentials - 登录凭证
 * @param {string} credentials.username - 用户名
 * @param {string} credentials.password - 密码
 * @returns {Promise} 返回登录响应数据
 */
export const login = ({ username, password }) => {
    return http.post('/api/auth/login', {
        username,
        password
    })
}

/**
 * 用户登出
 * @returns {Promise}
 */
export const logout = () => {
    return http.post('/api/auth/logout')
}

/**
 * 获取当前用户信息
 * @returns {Promise}
 */
export const getCurrentUser = () => {
    return http.get('/api/auth/me')
}

/**
 * 刷新 token
 * @returns {Promise}
 */
export const refreshToken = () => {
    return http.post('/api/auth/refresh')
}
