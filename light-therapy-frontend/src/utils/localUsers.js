/**
 * 本地用户数据存储
 * 包含受试者和研究者两类用户的账号信息
 */

// 研究者账号集合
export const researchers = [
  {
    id: 'admin',
    username: 'admin',
    password: 'admin',
    name: '管理员',
    role: 'researcher'
  },
  {
    id: 'researcher1',
    username: 'researcher1',
    password: 'researcher1',
    name: '研究者1',
    role: 'researcher'
  },
  {
    id: 'researcher2',
    username: 'researcher2',
    password: 'researcher2',
    name: '研究者2',
    role: 'researcher'
  }
]

// 受试者账号集合
export const subjects = [
  {
    id: 'SUB-001',
    username: 'SUB-001',
    password: 'password1',
    name: '受试者001',
    role: 'subject'
  },
  {
    id: 'SUB-002',
    username: 'SUB-002',
    password: 'password2',
    name: '受试者002',
    role: 'subject'
  },
  {
    id: 'SUB-003',
    username: 'SUB-003',
    password: 'password3',
    name: '受试者003',
    role: 'subject'
  },
  {
    id: 'SUB-004',
    username: 'SUB-004',
    password: 'password4',
    name: '受试者004',
    role: 'subject'
  },
  {
    id: 'SUB-005',
    username: 'SUB-005',
    password: 'password5',
    name: '受试者005',
    role: 'subject'
  }
]

/**
 * 本地登录验证函数
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @param {string} role - 角色类型
 * @returns {Object|null} - 验证成功返回用户信息，失败返回null
 */
export const localLogin = (username, password, role) => {
  const userList = role === 'researcher' ? researchers : subjects
  
  const user = userList.find(u => u.username === username && u.password === password)
  
  if (user) {
    // 返回用户信息，模拟后端响应格式
    return {
      userId: user.id,
      role: user.role,
      token: `mock-token-${user.role}-${user.id}` // 生成模拟token
    }
  }
  
  return null
}