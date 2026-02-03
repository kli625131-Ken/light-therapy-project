# 脚本用于安装Maven并运行光疗后端项目

# 定义常量
$MAVEN_VERSION = "3.9.6"
$MAVEN_URL = "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip"
$MAVEN_INSTALL_DIR = "D:\apache-maven-$MAVEN_VERSION"
$PROJECT_DIR = "$PSScriptRoot"

# 检查Maven是否已安装
if (-not (Test-Path "$MAVEN_INSTALL_DIR\bin\mvn.cmd")) {
    Write-Host "正在下载Maven $MAVEN_VERSION..."
    $tempFile = "$env:TEMP\apache-maven-$MAVEN_VERSION-bin.zip"
    
    # 下载Maven
    Invoke-WebRequest -Uri $MAVEN_URL -OutFile $tempFile
    
    Write-Host "正在解压Maven..."
    # 解压Maven
    Expand-Archive -Path $tempFile -DestinationPath "D:\"
    
    # 验证安装
    if (Test-Path "$MAVEN_INSTALL_DIR\bin\mvn.cmd") {
        Write-Host "Maven $MAVEN_VERSION 安装成功！"
    } else {
        Write-Host "Maven 安装失败，请检查下载链接或权限。"
        exit 1
    }
}

# 配置环境变量
$env:PATH = "$MAVEN_INSTALL_DIR\bin;$env:PATH"
$env:MAVEN_HOME = $MAVEN_INSTALL_DIR

# 验证Maven安装
Write-Host "正在验证Maven安装..."
mvn -version

# 运行项目
Write-Host "正在运行光疗后端项目..."
Set-Location -Path $PROJECT_DIR
mvn spring-boot:run