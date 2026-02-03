# 协作与 AI 使用规范（Trae / Agent）

## 1. 分支约定
- 默认开发分支：`dev`
- 功能分支：`feat/<短名>`（例：feat/export-table）
- 修复分支：`fix/<短名>`
- 热修：`hotfix/<短名>`（必要时）

## 2. 提交规范（Commit Message）
格式建议：
- feat: 新功能
- fix: 修复 bug
- chore: 工程杂项（依赖、配置、脚本）
- docs: 文档
- refactor: 重构（不改变功能）
示例：
- feat(frontend): 优化治疗记录导出表格样式
- fix(backend): 修复 /api/surveys/my 空指针
- chore: update gitignore

## 3. AI 可以改什么
✅ 允许：
- `light-therapy-frontend/src/**`
- `light-therapy-backend/src/**`
- `docs/**`
- `scripts/**`

⛔ 禁止（除非你明确允许）：
- `.env*`（任何真实密钥）
- `node_modules/`, `dist/`, `target/`（构建产物）
- 数据库生产环境配置/账号密码

## 4. AI 提交前的自检清单（必须）
在提交前执行：
- `git status` 确认没有误入大文件/构建产物
- 前端：确保 `node_modules/`、`dist/` 没被追踪
- 后端：确保 `target/` 没被追踪

## 5. PR/合并规则（如果以后多人）
- 功能分支 → PR → 合并到 dev
- dev 稳定后再合并到 main（发布用）