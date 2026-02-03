<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Sun, Users, Lock, AlertCircle } from 'lucide-vue-next'
import { useUserStore } from '../stores/useUserStore'
import { login } from '../api/modules/auth'
import { setToken, removeToken } from '../utils/token'
import Button from '../components/ui/Button.vue'
import Card from '../components/ui/Card.vue'

const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const handleLogin = async () => {
  error.value = ''
  
  // 表单验证
  if (!username.value.trim()) {
    error.value = '请输入用户名'
    return
  }
  
  if (!password.value) {
    error.value = '请输入密码'
    return
  }
  
  loading.value = true
  
  try {
    // 使用后端API登录验证
    const response = await login({
      username: username.value.trim(),
      password: password.value
    })
    
    console.log('✅ 登录成功，响应数据:', response)

    // 根据服务器返回的角色确定实际角色
    const actualRole = response.role // Expect "SUBJECT" or "ADMIN"
    const navigationRole = actualRole === 'SUBJECT' ? 'subject' : 'admin'
    
    // 保存 token 到 localStorage，传递角色参数
    if (response.token) {
      setToken(response.token, navigationRole)
      console.log('✅ Token 已保存，角色:', navigationRole)
    } else {
      console.warn('⚠️ 登录响应中没有 token')
    }
    
    // 构造用户对象
    const user = {
      id: response.userId || response.subjectId || username.value.trim(),
      username: username.value.trim(),
      name: response.name || username.value.trim(),
      role: navigationRole,
      subject: response.subject,
      snapshot: response.snapshot
    }
    
    // 使用实际角色进行登录
    userStore.login(navigationRole, user)
    router.push(`/${navigationRole}`)
    
  } catch (err) {
    console.error('❌ 登录失败:', err)
    error.value = err.message || '登录失败，请检查用户名和密码'
    // 清除可能存在的无效 token，传递当前角色参数（如果有）
    const currentRole = userStore.role || null
    removeToken(currentRole)
  } finally {
    loading.value = false
  }
}


</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-cyan-50 to-blue-100 flex items-center justify-center p-4">
    <Card className="w-full max-w-md p-8 bg-white/90 backdrop-blur shadow-xl">
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg shadow-blue-200">
          <Sun class="text-white" :size="32" />
        </div>
        <h1 class="text-2xl font-bold text-gray-900">光疗设备管理系统</h1>
        <p class="text-gray-500 mt-2 text-sm">临床试验与数据采集平台</p>
      </div>
      
      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-gray-500 mb-1 uppercase">
            用户名
          </label>
          <div class="relative">
            <Users class="absolute left-3 top-3 text-gray-400" :size="18" />
            <input
              v-model="username"
              type="text"
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 ring-blue-500 outline-none"
              placeholder="请输入用户名"
            />
          </div>
        </div>
        
        <div>
          <label class="block text-xs font-medium text-gray-500 mb-1 uppercase">密码</label>
          <div class="relative">
            <Lock class="absolute left-3 top-3 text-gray-400" :size="18" />
            <input
              v-model="password"
              type="password"
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 ring-blue-500 outline-none"
              placeholder="请输入密码"
            />
          </div>
        </div>
        
        <div v-if="error" class="text-red-500 text-sm flex items-center gap-1 bg-red-50 p-2 rounded">
          <AlertCircle :size="14" />
          {{ error }}
        </div>
        
        <Button @click="handleLogin" :disabled="loading" className="w-full h-12 text-lg mt-4">
          {{ loading ? '登录中...' : '登录系统' }}
        </Button>
      </div>
    </Card>
  </div>
</template>
