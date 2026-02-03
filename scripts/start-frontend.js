#!/usr/bin/env node

/**
 * 前端项目启动脚本
 * 功能：
 * 1. 检查前端项目依赖是否完整
 * 2. 若依赖不完整，自动安装所需依赖
 * 3. 启动前端开发服务器
 * 4. 提供清晰的启动状态反馈
 * 5. 包含错误处理机制
 * 6. 支持跨平台（Windows和Unix系统）
 */

const fs = require('fs');
const path = require('path');
const { execSync, exec } = require('child_process');

// 前端项目目录
const FRONTEND_DIR = path.join(__dirname, '..', 'light-therapy-frontend');

// 日志函数
function log(message, type = 'info') {
  const timestamp = new Date().toISOString().slice(0, 19).replace('T', ' ');
  const prefixes = {
    info: '\x1b[32m[INFO]\x1b[0m',
    warning: '\x1b[33m[WARNING]\x1b[0m',
    error: '\x1b[31m[ERROR]\x1b[0m',
    success: '\x1b[32m[SUCCESS]\x1b[0m'
  };
  console.log(`${prefixes[type] || prefixes.info} [${timestamp}] ${message}`);
}

// 错误处理函数
function handleError(error, message) {
  log(`${message}: ${error.message}`, 'error');
  process.exit(1);
}

// 检查目录是否存在
function checkDirectoryExists(dirPath) {
  if (!fs.existsSync(dirPath)) {
    log(`目录不存在: ${dirPath}`, 'error');
    process.exit(1);
  }
  log(`找到目录: ${dirPath}`);
}

// 检查依赖是否完整
function checkDependencies() {
  log('检查前端项目依赖...');
  
  const packageJsonPath = path.join(FRONTEND_DIR, 'package.json');
  if (!fs.existsSync(packageJsonPath)) {
    log('package.json 文件不存在', 'error');
    process.exit(1);
  }
  
  const nodeModulesPath = path.join(FRONTEND_DIR, 'node_modules');
  if (!fs.existsSync(nodeModulesPath)) {
    log('node_modules 目录不存在，需要安装依赖', 'warning');
    return false;
  }
  
  // 检查是否有package-lock.json文件
  const packageLockPath = path.join(FRONTEND_DIR, 'package-lock.json');
  if (!fs.existsSync(packageLockPath)) {
    log('package-lock.json 文件不存在，建议重新安装依赖', 'warning');
    return false;
  }
  
  log('依赖检查完成，依赖已存在', 'success');
  return true;
}

// 安装依赖
function installDependencies() {
  log('开始安装前端项目依赖...');
  
  try {
    const installCommand = process.platform === 'win32' ? 'npm.cmd install' : 'npm install';
    execSync(installCommand, { cwd: FRONTEND_DIR, stdio: 'inherit' });
    log('依赖安装完成', 'success');
  } catch (error) {
    handleError(error, '依赖安装失败');
  }
}

// 启动前端开发服务器
function startFrontendServer() {
  log('启动前端开发服务器...');
  
  try {
    const startCommand = process.platform === 'win32' ? 'npm.cmd run dev' : 'npm run dev';
    const serverProcess = exec(startCommand, { cwd: FRONTEND_DIR, stdio: 'inherit' });
    
    serverProcess.on('error', (error) => {
      handleError(error, '启动前端服务器失败');
    });
    
    serverProcess.on('exit', (code) => {
      if (code !== 0) {
        log(`前端服务器退出，退出码: ${code}`, 'error');
        process.exit(code);
      }
    });
    
    log('前端开发服务器启动成功', 'success');
    log('请在浏览器中访问服务器输出的URL', 'info');
  } catch (error) {
    handleError(error, '启动前端服务器失败');
  }
}

// 主函数
function main() {
  log('=== 前端项目启动脚本 ===');
  
  try {
    // 检查目录
    checkDirectoryExists(FRONTEND_DIR);
    
    // 检查依赖
    const dependenciesExist = checkDependencies();
    
    // 安装依赖（如果需要）
    if (!dependenciesExist) {
      installDependencies();
    }
    
    // 启动服务器
    startFrontendServer();
  } catch (error) {
    handleError(error, '启动前端项目失败');
  }
}

// 执行主函数
if (require.main === module) {
  main();
}
