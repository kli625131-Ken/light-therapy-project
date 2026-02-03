# 后端 API 集成使用说明

## 📁 文件结构

```
src/
├── api/
│   ├── http.js              # Axios 实例配置（拦截器）
│   ├── index.js             # API 统一导出
│   └── modules/
│       └── auth.js          # 认证相关 API
├── utils/
│   └── token.js             # Token 管理工具
└── views/
    └── LoginView.vue        # 登录页面（已集成）
```

## 🔧 配置说明

### 1. 环境变量配置

在项目根目录的 `.env` 文件中配置后端 API 地址：

```env
VITE_API_BASE_URL=http://192.168.0.15:8080
```

### 2. Axios 配置特性

**`src/api/http.js`** 提供以下功能：

- ✅ 自动从环境变量读取 `baseURL`
- ✅ 超时时间：15 秒
- ✅ 请求拦截器：自动添加 `Authorization: Bearer <token>`
- ✅ 响应拦截器：统一解包 `{code, message, data}` 结构
- ✅ 错误处理：401 自动清除 token 并跳转登录
- ✅ 网络错误和超时友好提示

## 📝 使用示例

### 1. 登录流程（LoginView.vue）

```vue
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login as apiLogin } from '../api/modules/auth'
import { setToken } from '../utils/token'
import { useUserStore } from '../stores/useUserStore'

const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const handleLogin = async () => {
  error.value = ''
  loading.value = true
  
  try {
    // 调用登录 API
    const response = await apiLogin({
      username: username.value.trim(),
      password: password.value
    })
    
    // response 已被拦截器解包，直接是 data 部分
    const { token, user } = response
    
    // 保存 token
    setToken(token)
    
    // 更新用户状态
    userStore.login(user.role, user)
    
    // 根据角色跳转
    if (user.role === 'researcher' || user.role === 'admin') {
      router.push('/researcher')
    } else {
      router.push('/subject')
    }
    
  } catch (err) {
    error.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <input v-model="username" placeholder="用户名" />
    <input v-model="password" type="password" placeholder="密码" />
    <button @click="handleLogin" :disabled="loading">
      {{ loading ? '登录中...' : '登录' }}
    </button>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>
```

### 2. 在其他组件中调用 API

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { getCurrentUser } from '../api/modules/auth'

const userInfo = ref(null)
const loading = ref(false)

const fetchUserInfo = async () => {
  loading.value = true
  try {
    const data = await getCurrentUser()
    userInfo.value = data
  } catch (error) {
    console.error('获取用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>
```

### 3. Token 管理

```javascript
import { setToken, getToken, removeToken, hasToken } from '../utils/token'

// 保存 token
setToken('your-jwt-token-here')

// 获取 token
const token = getToken()

// 检查是否已登录
if (hasToken()) {
  console.log('用户已登录')
}

// 登出时清除 token
removeToken()
```

## 🔐 后端接口约定

### 登录接口

**请求：**
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

**响应：**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "userId": 1,
    "role": "ADMIN",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**说明：**
- `code`: 0 表示成功
- `data.userId`: 用户 ID
- `data.role`: 用户角色（"ADMIN" / "RESEARCHER" / "SUBJECT"）
- `data.token`: JWT Token

### 错误响应

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

## 🚀 扩展其他 API

在 `src/api/modules/` 目录下创建新的模块文件：

```javascript
// src/api/modules/scheme.js
import http from '../http'

export const getSchemes = () => {
  return http.get('/api/schemes')
}

export const createScheme = (data) => {
  return http.post('/api/schemes', data)
}

export const updateScheme = (id, data) => {
  return http.put(`/api/schemes/${id}`, data)
}

export const deleteScheme = (id) => {
  return http.delete(`/api/schemes/${id}`)
}
```

然后在 `src/api/index.js` 中导出：

```javascript
export * from './modules/auth'
export * from './modules/scheme'
```

## ⚠️ 注意事项

1. **CORS 配置**：确保后端已配置 CORS，允许前端域名访问
2. **Token 过期处理**：当前配置会在 401 错误时自动清除 token 并跳转登录
3. **环境变量**：生产环境需要配置 `.env.production` 文件
4. **错误处理**：建议在关键操作中添加 try-catch 处理
5. **Loading 状态**：建议在 API 调用时显示 loading 状态，提升用户体验

## 🔄 从本地存储迁移到后端 API

当前项目使用 localStorage 存储数据，迁移到后端 API 的步骤：

1. ✅ 登录功能已集成后端 API
2. 🔲 将 `useDataStore` 中的数据操作改为调用后端 API
3. 🔲 移除 localStorage 持久化，改为从后端获取数据
4. 🔲 实现数据的增删改查接口调用

## 📞 技术支持

如有问题，请检查：
- 浏览器控制台的网络请求
- 后端服务是否正常运行
- API 地址配置是否正确
- Token 是否正确保存和发送
