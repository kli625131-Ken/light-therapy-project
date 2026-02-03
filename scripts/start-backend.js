#!/usr/bin/env node

/**
 * 后端项目启动脚本
 * 功能：
 * 1. 检查后端项目依赖是否完整
 * 2. 若依赖不完整，自动安装所需依赖
 * 3. 启动后端开发服务器
 * 4. 提供清晰的启动状态反馈
 * 5. 包含错误处理机制
 * 6. 支持跨平台（Windows和Unix系统）
 */

const fs = require('fs');
const path = require('path');
const { execSync, exec } = require('child_process');

// 后端项目目录
const BACKEND_DIR = path.join(__dirname, '..', 'light-therapy-backend');

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

// 检查Maven是否安装
function checkMavenInstalled() {
  log('检查Maven是否安装...');
  try {
    const mvnVersion = execSync('mvn --version', { encoding: 'utf8' });
    log('Maven已安装', 'success');
    return true;
  } catch (error) {
    log('Maven未安装或不在PATH中', 'error');
    log('请先安装Maven并添加到系统PATH环境变量', 'error');
    process.exit(1);
  }
}

// 检查依赖是否需要更新
function checkDependencies() {
  log('检查后端项目依赖...');
  
  const pomXmlPath = path.join(BACKEND_DIR, 'pom.xml');
  if (!fs.existsSync(pomXmlPath)) {
    log('pom.xml 文件不存在', 'error');
    process.exit(1);
  }
  
  log('找到pom.xml文件', 'success');
  return true;
}

// 安装依赖
function installDependencies() {
  log('开始安装后端项目依赖...');
  
  try {
    const installCommand = process.platform === 'win32' ? 'mvn.cmd dependency:resolve' : 'mvn dependency:resolve';
    execSync(installCommand, { cwd: BACKEND_DIR, stdio: 'inherit' });
    log('依赖安装完成', 'success');
  } catch (error) {
    handleError(error, '依赖安装失败');
  }
}

// 构建项目
function buildProject() {
  log('开始构建后端项目...');
  
  try {
    const buildCommand = process.platform === 'win32' ? 'mvn.cmd compile' : 'mvn compile';
    execSync(buildCommand, { cwd: BACKEND_DIR, stdio: 'inherit' });
    log('项目构建完成', 'success');
  } catch (error) {
    handleError(error, '项目构建失败');
  }
}

// 启动后端开发服务器
function startBackendServer() {
  log('启动后端开发服务器...');
  
  try {
    const startCommand = process.platform === 'win32' ? 'mvn.cmd spring-boot:run' : 'mvn spring-boot:run';
    const serverProcess = exec(startCommand, { cwd: BACKEND_DIR });
    
    // 捕获标准输出
    serverProcess.stdout.on('data', (data) => {
      process.stdout.write(data);
    });
    
    // 捕获标准错误
    serverProcess.stderr.on('data', (data) => {
      process.stderr.write(data);
    });
    
    serverProcess.on('error', (error) => {
      handleError(error, '启动后端服务器失败');
    });
    
    serverProcess.on('exit', (code) => {
      if (code !== 0) {
        log(`后端服务器退出，退出码: ${code}`, 'error');
        process.exit(code);
      }
    });
    
    log('后端开发服务器正在启动...', 'info');
    log('请等待服务器启动完成并查看下方的启动信息', 'info');
  } catch (error) {
    handleError(error, '启动后端服务器失败');
  }
}

// 主函数
function main() {
  log('=== 后端项目启动脚本 ===');
  
  try {
    // 检查目录
    checkDirectoryExists(BACKEND_DIR);
    
    // 检查Maven
    checkMavenInstalled();
    
    // 检查依赖
    checkDependencies();
    
    // 安装依赖
    installDependencies();
    
    // 构建项目
    buildProject();
    
    // 启动服务器
    startBackendServer();
  } catch (error) {
    handleError(error, '启动后端项目失败');
  }
}

// 执行主函数
if (require.main === module) {
  main();
}
