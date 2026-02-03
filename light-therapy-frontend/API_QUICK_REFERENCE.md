# 🚀 后端 API 集成 - 快速参考

## ✅ 已完成的工作

### 1. 安装依赖
```bash
npm install axios
```

### 2. 创建的文件

| 文件路径 | 说明 |
|---------|------|
| `.env` | 环境变量配置（API 地址） |
| `src/utils/token.js` | Token 管理工具 |
| `src/api/http.js` | Axios 实例配置 |
| `src/api/modules/auth.js` | 认证 API |
| `src/api/index.js` | API 统一导出 |
| `src/api/test.js` | API 测试工具 |
| `API_INTEGRATION.md` | 完整使用文档 |

### 3. 更新的文件

| 文件路径 | 更新内容 |
|---------|---------|
| `src/views/LoginView.vue` | 集成后端登录 API |
| `src/main.js` | 开发环境加载测试工具 |

## 🔧 核心功能

### Token 管理
```javascript
import { setToken, getToken, removeToken, hasToken } from '@/utils/token'

setToken('your-token')    // 保存
const token = getToken()  // 获取
removeToken()             // 删除
hasToken()                // 检查
```

### API 调用
```javascript
import { login } from '@/api/modules/auth'

const response = await login({ username, password })
// response 已自动解包，直接是 data 部分
```

### 请求拦截器
- ✅ 自动添加 `Authorization: Bearer <token>`
- ✅ 从 localStorage 读取 token

### 响应拦截器
- ✅ 统一解包 `{code, message, data}`
- ✅ code === 200 返回 data
- ✅ 401 自动清除 token 并跳转登录
- ✅ 其他错误统一处理

## 🧪 测试方法

### 方法 1：使用测试工具（推荐）
1. 启动开发服务器：`npm run dev`
2. 打开浏览器控制台
3. 输入：`apiTest.runFullTest()`

### 方法 2：在登录页面测试
1. 访问 `http://localhost:5173/login`
2. 输入用户名和密码
3. 点击登录按钮
4. 查看控制台和网络请求

### 方法 3：手动测试单个 API
```javascript
// 在浏览器控制台
apiTest.testLogin('admin', 'admin')
apiTest.showToken()
apiTest.testGetCurrentUser()
apiTest.testLogout()
```

## 📋 后端接口要求

### 登录接口
```
POST http://192.168.0.15:8080/api/auth/login
```

**请求体：**
```json
{
  "username": "string",
  "password": "string"
}
```

**响应体：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "user-id",
      "username": "username",
      "name": "User Name",
      "role": "researcher"  // 或 "subject"
    }
  }
}
```

## ⚠️ 重要提示

1. **CORS 配置**：确保后端已配置 CORS
2. **Token 格式**：使用 JWT Bearer Token
3. **角色字段**：user.role 必须是 "researcher" 或 "subject"
4. **错误处理**：所有 API 调用都应包裹在 try-catch 中

## 🔄 下一步工作

- [ ] 实现其他数据接口（方案、用户、日志等）
- [ ] 将 Pinia store 改为从后端获取数据
- [ ] 移除 localStorage 数据持久化
- [ ] 添加请求 loading 状态管理
- [ ] 实现 token 自动刷新机制

## 📞 问题排查

### 登录失败
1. 检查后端服务是否运行
2. 检查 API 地址是否正确（.env 文件）
3. 查看浏览器控制台网络请求
4. 检查 CORS 配置

### Token 未发送
1. 检查 localStorage 中是否有 token
2. 查看请求头是否包含 Authorization
3. 确认 token 格式正确

### 401 错误
1. Token 可能已过期
2. Token 格式不正确
3. 后端验证失败

---

📖 详细文档请查看：`API_INTEGRATION.md`
