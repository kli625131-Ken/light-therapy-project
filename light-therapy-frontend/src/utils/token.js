/**
 * Token 管理工具
 * 提供 token 的存储、获取、删除功能
 */

/**
 * 根据角色获取不同的 Token 键名
 * @param {string|null} role - 用户角色，可为 null
 * @returns {string} Token 键名
 */
const getTokenKey = (role) => {
    return role ? `light_therapy_token_${role}` : 'light_therapy_token'
}

/**
 * 保存 token 到 localStorage
 * @param {string} token - JWT token
 * @param {string} role - 用户角色
 */
export const setToken = (token, role) => {
    if (token) {
        localStorage.setItem(getTokenKey(role), token)
    }
}

/**
 * 从 localStorage 获取 token
 * @param {string} role - 用户角色
 * @returns {string|null} token 或 null
 */
export const getToken = (role) => {
    return localStorage.getItem(getTokenKey(role))
}

/**
 * 从 localStorage 删除 token
 * @param {string} role - 用户角色
 */
export const removeToken = (role) => {
    localStorage.removeItem(getTokenKey(role))
}

/**
 * 检查是否已登录（是否有 token）
 * @param {string} role - 用户角色
 * @returns {boolean}
 */
export const hasToken = (role) => {
    return !!getToken(role)
}
