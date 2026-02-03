# light-therapy-project

## 目录结构
- `light-therapy-backend/`：后端（Spring Boot）
- `light-therapy-frontend/`：前端（Vite + Vue）

## 项目启动
```bash
## 前端启动
cd light-therapy-frontend
npm install
npm run dev

## 后端启动
cd light-therapy-backend
# Windows
mvnw.cmd spring-boot:run
# macOS/Linux
./mvnw spring-boot:run

## 说明
本仓库已忽略 node_modules/、target/、.env 等本地/构建产物（见 .gitignore）

然后在命令行提交（你已经在 dev 分支）：
```bat
git add README.md
git commit -m "docs: add root README"
git push
