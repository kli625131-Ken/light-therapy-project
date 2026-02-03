import { defineStore } from 'pinia'
import { ref, computed, onMounted } from 'vue'
import { getToken, removeToken } from '../utils/token'
import { getCurrentUser } from '../api/modules/auth'

export const useUserStore = defineStore('user', () => {
  const role = ref(null)
  const currentUser = ref(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => role.value !== null)
  const isSubject = computed(() => role.value === 'subject')
  const isAdmin = computed(() => role.value === 'admin')
  const isResearcher = computed(() => role.value === 'researcher' || role.value === 'admin')

  // 根据角色获取不同的用户信息存储键名
  const getUserInfoKey = () => {
    return role.value ? `light_therapy_user_info_${role.value}` : 'light_therapy_user_info'
  }

  // 保存用户信息到 localStorage
  const saveUserInfo = () => {
    if (role.value && currentUser.value) {
      const userInfo = {
        role: role.value,
        currentUser: currentUser.value
      }
      localStorage.setItem(getUserInfoKey(), JSON.stringify(userInfo))
    }
  }

  // 从 localStorage 恢复用户信息
  const restoreUserInfo = () => {
    try {
      // 如果当前角色为空，不尝试恢复，避免误清除
      if (!role.value) {
        return
      }
      // 尝试获取当前角色的用户信息
      const userInfoStr = localStorage.getItem(getUserInfoKey())
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr)
        role.value = userInfo.role
        currentUser.value = userInfo.currentUser
      }
    } catch (error) {
      console.error('恢复用户信息失败:', error)
      // 恢复失败时，只清除当前角色的无效数据，不影响其他角色
      const currentRole = role.value
      localStorage.removeItem(getUserInfoKey())
      removeToken(currentRole)
      role.value = null
      currentUser.value = null
    }
  }

  // 清除用户信息
  const clearUserInfo = () => {
    // 先保存当前角色，用于后续清除token
    const currentRole = role.value
    // 清除当前角色的用户信息
    localStorage.removeItem(getUserInfoKey())
    // 清除token，使用保存的当前角色
    removeToken(currentRole)
    // 重置状态
    role.value = null
    currentUser.value = null
  }

  // 登录函数
  function login(roleName, userObj) {
    role.value = roleName
    currentUser.value = userObj
    saveUserInfo()
  }

  // 登出函数
  function logout() {
    clearUserInfo()
  }

  // 检查认证状态
  async function checkAuth() {
    // 如果已经登录，直接返回
    if (isLoggedIn.value) {
      return true
    }

    // 尝试从localStorage获取所有可能的角色信息
    const possibleRoles = ['subject', 'admin']
    let foundUserInfo = null
    let foundRole = null
    
    for (const possibleRole of possibleRoles) {
      const userInfoKey = `light_therapy_user_info_${possibleRole}`
      const userInfoStr = localStorage.getItem(userInfoKey)
      if (userInfoStr) {
        try {
          foundUserInfo = JSON.parse(userInfoStr)
          foundRole = possibleRole
          break
        } catch (error) {
          console.error(`解析${userInfoKey}失败:`, error)
          // 删除无效数据
          localStorage.removeItem(userInfoKey)
          localStorage.removeItem(`light_therapy_token_${possibleRole}`)
        }
      }
    }
    
    // 如果找到了有效的用户信息，直接恢复登录状态
    if (foundUserInfo && foundRole) {
      role.value = foundUserInfo.role
      currentUser.value = foundUserInfo.currentUser
      return true
    }

    // 尝试从服务器获取当前用户信息
    try {
      loading.value = true
      const userData = await getCurrentUser()
      if (userData) {
        // 根据用户角色确定导航角色
        const navigationRole = userData.role === 'SUBJECT' ? 'subject' : 'admin'
        
        // 构造用户对象
        const userObj = {
          id: userData.userId || userData.subjectId,
          username: userData.username,
          name: userData.name,
          role: navigationRole,
          subject: userData.subject,
          snapshot: userData.snapshot
        }
        
        // 登录用户
        login(navigationRole, userObj)
        return true
      }
    } catch (error) {
      console.error('验证用户信息失败:', error)
      // 验证失败时，只清除当前角色的无效数据（如果有）
      if (role.value) {
        clearUserInfo()
      }
    } finally {
      loading.value = false
    }
    
    return false
  }

  // 在组件挂载时自动恢复用户信息
  onMounted(() => {
    restoreUserInfo()
  })

  return {
    role,
    currentUser,
    loading,
    isLoggedIn,
    isSubject,
    isResearcher,
    isAdmin,
    login,
    logout,
    checkAuth
  }
})
