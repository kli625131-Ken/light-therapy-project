前端迁移 Vue3 方案（A 阶段 · 最终执行版）
适用对象：当前基于 React 19 + Vite + JavaScript 的前端项目
当前核心问题：App.jsx 为 1300+ 行单体文件，包含全部状态、逻辑与视图
迁移原则：迁移即重构（Refactor During Migration），严禁等价复制单体结构

0. 迁移目标与总体原则
0.1 核心目标
从 React 19 单体 App.jsx 迁移至 Vue 3 标准工程结构

在 A 阶段完成：

工程基础搭建

单体结构拆解

状态 / 路由 / 视图的清晰边界建立

0.2 强制性原则（Hard Rules）
以下规则为 进入页面迁移前的硬门槛

❌ 禁止 在 Vue 项目中继续使用单一大文件承载全部逻辑

✅ 必须 拆分为：

css
Copy code
src/
  components/
  views/
  stores/
  router/
❌ 禁止 直接 copy-paste App.jsx 到 Vue 组件中

✅ 必须 先完成 结构映射 → 再编码

1. 迁移范围与优先级（Migration Scope & Priorities）
1.1 识别并拆解单体（Identify Monolith）
当前状态：

App.jsx

承载：认证、角色判断、数据种子、UI、页面切换

规模：1300+ 行

迁移目标：

拆分为 状态层 / 路由层 / 视图层 / UI 组件层

1.2 分阶段优先级（A 阶段仅做以下内容）
Phase A-1：基础设施（Infrastructure）
Vite（Vue3）

Tailwind CSS（保持不变）

目录结构初始化

基础 UI 组件抽离

Phase A-2：状态层（State Layer）
提取并迁移：

SEED_DATA

users

logs

使用 Pinia 统一管理

实现持久化（对齐 usePersistentState）

Phase A-3：视图层（View Layer）
拆分并迁移为独立路由视图：

Login

Subject

Researcher

2. 技术栈（A 阶段最终确定）
模块	技术
框架	Vue 3（Composition API）
构建	Vite
语言	JavaScript
样式	Tailwind CSS（保持）
状态管理	Pinia
路由	Vue Router
持久化	Pinia Persist 或 localStorage

图标与样式 A 阶段不做调整

3. 状态管理设计（替代 App.jsx + usePersistentState）
3.1 Store 拆分方案（强制）
src/stores/useUserStore.js
负责：

currentUser

role

login / logout

登录态持久化

src/stores/useDataStore.js
负责：

SEED_DATA

users

logs

3.2 持久化策略（必须验证）
使用 Pinia Persist 插件 或

手动 watch + localStorage

行为需 等价于 React 中的 usePersistentState

4. 路由与权限模型（替代 App.jsx 条件渲染）
4.1 React 现状（必须被消除）
js
Copy code
if (!currentUser) return <Login />
if (role === 'admin') return <Researcher />
return <Subject />
4.2 Vue Router 映射方案（强制）
条件	路由
未登录	/login
admin	/researcher
subject	/subject

4.3 路由守卫（A 阶段必须）
登录态校验

角色校验

非法访问自动 redirect

5. 组件迁移与目录规范（Component Strategy）
5.1 UI 组件抽离（通用）
css
Copy code
src/components/ui/
  Button.vue
  Card.vue
  Modal.vue
5.2 页面级视图（路由承载）
bash
Copy code
src/views/
  LoginView.vue
  SubjectView.vue
  ResearcherView.vue
规则：

views 不允许存业务无关组件

components/ui 不允许直接访问 store

6. Project-Specific Migration Guide（关键章节）
6.1 App.jsx → Vue 结构映射表
App.jsx 模块	Vue 目标位置
登录状态 / currentUser	useUserStore
SEED_DATA 初始化	useDataStore
logs 操作	useDataStore
登录界面 JSX	LoginView.vue
Subject UI	SubjectView.vue
Researcher UI	ResearcherView.vue
条件渲染	Vue Router + Guard
Button / Card	components/ui

6.2 推荐拆解顺序（不可跳过）
提取 状态（Store）

建立 路由

迁移 LoginView

迁移 Subject / Researcher

清理残留逻辑

7. 验证与验收（A 阶段）
7.1 功能校验清单
 刷新页面登录态仍存在

 不同角色进入不同路由

 未登录无法访问业务页面

 不存在 Vue 版“大 App.vue 单体”

7.2 结构验收
App.vue ≤ 100 行

无跨层直接访问（view → localStorage ❌）

8. A 阶段完成定义（Definition of Done）
A 阶段 完成的唯一标准：

Vue 项目中 不存在任何形式的单体 App.jsx 等价物，
所有逻辑均已归位至 stores / router / views / components