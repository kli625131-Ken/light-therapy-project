# 光疗后端系统

## 项目概述

本项目是一个光疗(light therapy)后端系统，用于管理光疗设备、治疗方案、治疗会话和受试者信息。系统采用Spring Boot框架开发，提供RESTful API接口，支持对光疗设备的远程控制和治疗过程的监控。

## 技术栈

- Spring Boot 2.7.18
- Java 1.8
- MySQL 8.0
- Spring Data JPA
- Spring Security
- JWT
- Flyway
- Maven

## 系统要求

- Java 1.8 或更高版本
- Maven 3.6 或更高版本
- MySQL 8.0 或更高版本

## 安装和配置

### 1. 安装Java

确保你的系统中安装了Java 1.8或更高版本。你可以从[Oracle官网](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)下载并安装。

### 2. 安装MySQL

从[MySQL官网](https://dev.mysql.com/downloads/mysql/)下载并安装MySQL 8.0或更高版本。

### 3. 创建数据库和用户

1. 启动MySQL服务
2. 使用MySQL客户端连接到数据库：
   ```bash
   mysql -u root -p
   ```
3. 创建数据库：
   ```sql
   CREATE DATABASE light_therapy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
4. 创建用户并授予权限：
   ```sql
   CREATE USER 'lontri'@'localhost' IDENTIFIED BY 'Admin123!';
   GRANT ALL PRIVILEGES ON light_therapy.* TO 'lontri'@'localhost';
   FLUSH PRIVILEGES;
   ```

### 4. 配置项目

1. 克隆项目代码
2. 修改`src/main/resources/application.yml`文件中的数据库配置（如果需要）：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/light_therapy?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Tokyo&allowPublicKeyRetrieval=true
       username: lontri
       password: Admin123!
       driver-class-name: com.mysql.cj.jdbc.Driver
   ```

## 运行项目

### 使用Maven命令运行

```bash
mvn spring-boot:run
```

### 使用打包后的jar文件运行

1. 打包项目：
   ```bash
   mvn clean package
   ```
2. 运行jar文件：
   ```bash
   java -jar target/light-therapy-backend-1.0.0.jar
   ```

## API文档

项目集成了SpringDoc OpenAPI，启动项目后可以通过以下地址访问API文档：

```
http://localhost:8080/swagger-ui.html
```

## 项目结构

```
com.lontri.lighttherapy
├── common/           # 通用组件
├── config/           # 配置类
├── controller/       # REST API控制器
├── device/           # 设备相关
├── dto/              # 数据传输对象
├── entity/           # 实体类
├── executor/         # 治疗执行器
├── infra/            # 基础设施实现
├── repository/       # 数据访问
├── service/          # 业务服务
└── util/             # 通用工具
```

## 核心功能

1. **用户认证与授权**：基于JWT的认证机制，支持角色-based访问控制
2. **治疗方案管理**：创建、修改、删除治疗方案，定义治疗阶段
3. **治疗会话管理**：创建、启动、结束、取消治疗会话
4. **设备控制**：支持TCP和UDP两种通信协议，控制光疗设备的亮度和色温
5. **受试者管理**：管理受试者信息，支持分组管理
6. **问卷管理**：创建问卷模板，记录问卷结果
7. **事件记录**：记录治疗过程中的关键事件

## 数据库迁移

项目使用Flyway进行数据库版本管理，迁移脚本位于`src/main/resources/db/migration/`目录。启动项目时，Flyway会自动执行迁移脚本，创建数据库表结构。

## 注意事项

1. 确保MySQL服务已经启动
2. 确保数据库配置正确
3. 首次启动项目时，Flyway会自动创建数据库表结构
4. 项目默认端口为8080，可以在`application.yml`文件中修改

## 常见问题

### Q: 启动项目时出现"Access denied for user 'lontri'@'localhost'"错误

A: 这是因为MySQL数据库中没有创建'lontri'用户或密码不正确。请按照"安装和配置"部分的步骤创建用户并授予权限。

### Q: 启动项目时出现"Unknown database 'light_therapy'"错误

A: 这是因为MySQL数据库中没有创建'light_therapy'数据库。请按照"安装和配置"部分的步骤创建数据库。

### Q: API文档无法访问

A: 请确保项目已经成功启动，并且访问地址正确：`http://localhost:8080/swagger-ui.html`

## 联系方式

如有问题或建议，请联系项目维护人员。