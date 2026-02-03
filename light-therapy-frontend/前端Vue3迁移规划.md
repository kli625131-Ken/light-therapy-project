# 前端Vue3+JavaScript+Vite架构迁移及界面重构规划

## 1. 技术栈迁移

| 原技术栈 | 新技术栈 | 备注 |
|---------|---------|------|
| React 19 | Vue 3 | 框架迁移 |
| JavaScript | JavaScript | 保持不变 |
| Vite | Vite | 构建工具保持不变 |
| Tailwind CSS | 本地CSS + CSS变量 | 样式方案变更，保证UI一致 |
| Lucide React | Lucide Vue | 图标库迁移 |
| React Hooks | Vue Composition API | 状态管理迁移 |

## 2. 项目结构设计

```
light-therapy-frontend/
├── public/                          # 静态资源
│   └── vite.svg
├── src/                             # 源代码
│   ├── assets/                      # 资源文件
│   │   ├── images/                  # 图片资源
│   │   └── styles/                  # 样式文件
│   │       ├── variables.css        # CSS变量定义
│   │       ├── reset.css            # 重置样式
│   │       └── global.css           # 全局样式
│   ├── components/                  # 通用组件
│   │   ├── Button.vue               # 按钮组件
│   │   ├── Card.vue                 # 卡片组件
│   │   ├── Modal.vue                # 模态框组件
│   │   ├── Input.vue                # 输入框组件
│   │   ├── Select.vue               # 选择器组件
│   │   ├── RangeSlider.vue          # 滑块组件
│   │   └── Loading.vue              # 加载组件
│   ├── views/                       # 页面组件
│   │   ├── Login.vue                # 登录页面
│   │   ├── subject/                 # 受试者页面
│   │   │   ├── Control.vue          # 光疗控制页面
│   │   │   └── Records.vue          # 治疗记录页面
│   │   └── researcher/              # 研究者页面
│   │       ├── UserManagement.vue   # 受试者管理页面
│   │       ├── SchemeManagement.vue # 方案管理页面
│   │       ├── SurveyConfig.vue     # 问卷配置页面
│   │       └── DataManagement.vue   # 数据管理页面
│   ├── router/                      # 路由配置
│   │   └── index.js                 # 路由定义
│   ├── store/                       # 状态管理
│   │   ├── index.js                 # Pinia入口
│   │   ├── modules/                 # 状态模块
│   │   │   ├── auth.js              # 认证状态
│   │   │   ├── user.js              # 用户状态
│   │   │   ├── scheme.js            # 方案状态
│   │   │   └── treatment.js         # 治疗状态
│   │   └── persist.js               # 状态持久化
│   ├── api/                         # API请求
│   │   ├── index.js                 # Axios实例配置
│   │   ├── auth.js                  # 认证API
│   │   ├── user.js                  # 用户API
│   │   ├── scheme.js                # 方案API
│   │   ├── treatment.js             # 治疗记录API
│   │   ├── survey.js                # 问卷API
│   │   └── backup.js                # 备份API
│   ├── utils/                       # 工具函数
│   │   ├── request.js               # 请求工具
│   │   ├── date.js                  # 日期处理
│   │   ├── export.js                # 导出工具
│   │   └── validator.js             # 表单验证
│   ├── App.vue                      # 根组件
│   └── main.js                      # 入口文件
├── .gitignore                       # Git忽略文件
├── index.html                       # HTML模板
├── package.json                     # 项目配置
├── vite.config.js                   # Vite配置
├── postcss.config.js                # PostCSS配置
└── README.md                        # 项目说明

## 3. 路由设计

| 路径 | 组件 | 角色 | 描述 |
|------|------|------|------|
| /login | Login.vue | 公共 | 登录页面 |
| /subject/control | Control.vue | 受试者 | 光疗控制页面 |
| /subject/records | Records.vue | 受试者 | 治疗记录页面 |
| /researcher/users | UserManagement.vue | 研究者 | 受试者管理页面 |
| /researcher/schemes | SchemeManagement.vue | 研究者 | 方案管理页面 |
| /researcher/survey | SurveyConfig.vue | 研究者 | 问卷配置页面 |
| /researcher/data | DataManagement.vue | 研究者 | 数据管理页面 |
| /404 | NotFound.vue | 公共 | 404页面 |

## 4. 组件设计

### 4.1 通用组件

| 组件名 | 功能 | 说明 |
|--------|------|------|
| Button | 按钮组件 | 支持多种样式和尺寸，保持与现有UI一致 |
| Card | 卡片组件 | 用于内容展示，保持与现有UI一致 |
| Modal | 模态框组件 | 支持不同尺寸和样式，用于表单和详情展示 |
| Input | 输入框组件 | 支持文本输入、密码输入等 |
| Select | 选择器组件 | 支持单选和多选 |
| RangeSlider | 滑块组件 | 用于调整亮度和色温参数 |
| Loading | 加载组件 | 用于异步操作的加载状态显示 |

### 4.2 页面组件

#### 4.2.1 登录页面 (Login.vue)
- 支持受试者和研究者两种角色登录
- 角色切换功能
- 表单验证
- 错误提示

#### 4.2.2 受试者页面

**Control.vue (光疗控制页面)**
- 光效模拟区域
- 方案选择列表
- 手动模式控制面板
- 治疗过程监控
- 治疗控制按钮

**Records.vue (治疗记录页面)**
- 治疗记录列表
- 记录详情展示

#### 4.2.3 研究者页面

**UserManagement.vue (受试者管理页面)**
- 受试者列表
- 新增/编辑受试者表单
- 分组管理
- 受试者记录查看
- 数据导出功能

**SchemeManagement.vue (方案管理页面)**
- 方案列表
- 新增/编辑方案表单
- 方案阶段配置
- 方案删除功能

**SurveyConfig.vue (问卷配置页面)**
- 问卷模板编辑
- 问题添加/编辑/删除
- 评分范围配置
- 问卷模式切换

**DataManagement.vue (数据管理页面)**
- 数据导出功能
- 临床报告生成
- 数据备份/恢复

## 5. 状态管理设计

### 5.1 状态管理方案
使用Pinia进行状态管理，设计以下状态模块：

### 5.2 状态模块

#### 5.2.1 Auth模块
- 登录状态
- 用户信息
- 令牌管理

#### 5.2.2 User模块
- 受试者列表
- 当前查看的受试者信息
- 分组列表

#### 5.2.3 Scheme模块
- 方案列表
- 当前编辑的方案
- 方案阶段信息

#### 5.2.4 Treatment模块
- 治疗记录列表
- 当前治疗状态
- 治疗参数

## 6. API对接设计

### 6.1 Axios配置
- 基础URL配置
- 请求拦截器（添加Token）
- 响应拦截器（错误处理）
- 超时配置

### 6.2 API接口封装
- 按功能模块封装API接口
- 统一的错误处理
- 类型定义

### 6.3 主要API对接

| 功能模块 | API接口 | 方法 |
|---------|---------|------|
| 认证 | /api/auth/login | POST |
| 用户管理 | /api/users | GET/POST/PUT/DELETE |
| 分组管理 | /api/groups | GET/POST/DELETE |
| 方案管理 | /api/schemes | GET/POST/PUT/DELETE |
| 治疗记录 | /api/treatment-logs | GET/POST |
| 问卷管理 | /api/surveys | GET/POST/PUT |
| 数据备份 | /api/backups | GET/POST/DELETE |

## 7. 样式方案设计

### 7.1 样式迁移策略
- 保留原UI设计，使用CSS变量实现主题统一
- 采用BEM命名规范组织CSS代码
- 实现响应式设计，适配不同屏幕尺寸
- 使用CSS Grid和Flexbox进行布局

### 7.2 CSS变量设计

```css
/* 主题色 */
:root {
  --primary-color: #3b82f6;          /* 蓝色 */
  --primary-hover: #2563eb;          /* 深蓝色 */
  --secondary-color: #64748b;        /* 灰色 */
  --danger-color: #ef4444;           /* 红色 */
  --success-color: #10b981;          /* 绿色 */
  --warning-color: #f59e0b;          /* 黄色 */
  --info-color: #06b6d4;             /* 青色 */
  
  /* 中性色 */
  --bg-primary: #ffffff;             /* 主背景 */
  --bg-secondary: #f8fafc;           /* 次背景 */
  --bg-tertiary: #f1f5f9;            /* 三级背景 */
  
  /* 文本色 */
  --text-primary: #1e293b;           /* 主文本 */
  --text-secondary: #64748b;         /* 次文本 */
  --text-tertiary: #94a3b8;          /* 三级文本 */
  
  /* 边框色 */
  --border-color: #e2e8f0;           /* 边框 */
  --border-radius: 0.5rem;           /* 圆角 */
  
  /* 阴影 */
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  
  /* 过渡动画 */
  --transition: all 0.2s ease-in-out;
}
```

### 7.3 响应式设计

```css
/* 断点设计 */
:root {
  --breakpoint-sm: 640px;
  --breakpoint-md: 768px;
  --breakpoint-lg: 1024px;
  --breakpoint-xl: 1280px;
}

/* 响应式工具类 */
@media (min-width: var(--breakpoint-sm)) {
  .sm-container {
    max-width: var(--breakpoint-sm);
  }
}

@media (min-width: var(--breakpoint-md)) {
  .md-container {
    max-width: var(--breakpoint-md);
  }
}

@media (min-width: var(--breakpoint-lg)) {
  .lg-container {
    max-width: var(--breakpoint-lg);
  }
}
```

## 8. 迁移步骤

### 8.1 项目初始化
1. 使用Vite创建Vue3项目
2. 安装必要依赖
3. 配置项目结构

### 8.2 基础组件开发
1. 开发通用组件（Button, Card, Modal等）
2. 实现组件样式，保证与原UI一致
3. 编写组件文档

### 8.3 路由配置
1. 配置路由规则
2. 实现路由守卫
3. 配置角色权限

### 8.4 状态管理
1. 初始化Pinia
2. 实现状态模块
3. 配置状态持久化

### 8.5 API对接
1. 配置Axios实例
2. 封装API接口
3. 实现API错误处理

### 8.6 页面开发
1. 开发登录页面
2. 开发受试者页面
3. 开发研究者页面
4. 实现页面间跳转

### 8.7 功能实现
1. 光疗控制功能
2. 治疗记录管理
3. 受试者管理
4. 方案管理
5. 问卷配置
6. 数据管理

### 8.8 样式优化
1. 统一样式规范
2. 实现响应式设计
3. 优化样式性能

### 8.9 测试与调试
1. 功能测试
2. 兼容性测试
3. 性能测试
4. 调试修复

## 9. 性能优化方案

### 9.1 代码优化
- 使用Vue3的Composition API
- 组件按需导入
- 减少不必要的组件渲染
- 使用v-if和v-show合理切换

### 9.2 资源优化
- 图片懒加载
- 静态资源CDN加速
- 代码分割
- 按需加载路由

### 9.3 渲染优化
- 使用虚拟滚动处理长列表
- 优化大型组件的渲染性能
- 使用keep-alive缓存组件
- 减少DOM操作

### 9.4 网络优化
- API请求防抖节流
- 合理使用缓存
- 压缩请求响应数据
- 使用HTTP/2

## 10. 测试策略

### 10.1 单元测试
- 使用Vitest进行单元测试
- 测试组件功能
- 测试工具函数
- 测试API封装

### 10.2 集成测试
- 测试页面间跳转
- 测试组件间交互
- 测试状态管理

### 10.3 端到端测试
- 使用Cypress进行端到端测试
- 测试完整用户流程
- 测试不同角色权限

### 10.4 兼容性测试
- 测试主流浏览器（Chrome, Firefox, Safari, Edge）
- 测试不同屏幕尺寸
- 测试移动端设备

## 11. 交付标准

### 11.1 功能完整性
- 所有原有功能完整迁移
- 新增功能正常实现
- 无功能缺失

### 11.2 UI一致性
- 与原UI设计保持一致
- 响应式设计适配良好
- 动画效果流畅

### 11.3 性能指标
- 页面加载时间 < 2s
- 首屏渲染时间 < 1s
- 组件渲染时间 < 100ms
- 无内存泄漏

### 11.4 代码质量
- 代码规范符合ESLint规则
- 组件文档完整
- 代码注释清晰
- 测试覆盖率 ≥ 70%

## 12. 迁移风险与应对策略

### 12.1 风险1：框架差异导致的功能实现问题
- 应对策略：充分理解Vue3和React的差异，编写适配代码
- 风险等级：中

### 12.2 风险2：样式迁移导致的UI不一致
- 应对策略：使用CSS变量统一主题，仔细对比原UI进行调整
- 风险等级：高

### 12.3 风险3：API对接出现问题
- 应对策略：与后端密切配合，编写API测试用例
- 风险等级：中

### 12.4 风险4：性能下降
- 应对策略：使用Vue3的性能优化特性，进行性能测试和优化
- 风险等级：低

### 12.5 风险5：迁移时间超出预期
- 应对策略：合理规划迁移步骤，分阶段实施，优先迁移核心功能
- 风险等级：中

## 13. 开发工具与环境

| 工具 | 版本 | 用途 |
|------|------|------|
| Node.js | 18.x | 运行环境 |
| npm/yarn | 9.x/1.22.x | 包管理工具 |
| Vite | 5.x | 构建工具 |
| Vue | 3.3.x | 前端框架 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.6.x | HTTP客户端 |
| ESLint | 8.x | 代码检查 |
| Prettier | 3.x | 代码格式化 |
| Vitest | 1.x | 单元测试 |
| Cypress | 13.x | 端到端测试 |

## 14. 开发规范

### 14.1 代码规范
- 使用ESLint + Prettier进行代码检查和格式化
- 遵循Vue3官方编码规范
- 组件命名使用PascalCase
- 文件命名使用kebab-case

### 14.2 样式规范
- 采用BEM命名规范
- 使用CSS变量统一主题
- 避免使用!important
- 优先使用Flexbox和Grid布局

### 14.3 提交规范
- 提交信息使用Conventional Commits规范
- 提交前进行代码检查
- 避免提交大文件

### 14.4 文档规范
- 组件文档清晰完整
- API文档详细
- 开发文档及时更新
