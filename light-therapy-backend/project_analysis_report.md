# 光疗后端系统技术分析报告

## 1. 项目概述

本项目是一个光疗(light therapy)后端系统，用于管理光疗设备、治疗方案、治疗会话和受试者信息。系统采用Spring Boot框架开发，提供RESTful API接口，支持对光疗设备的远程控制和治疗过程的监控。

## 2. 技术栈组成

| 技术/框架 | 版本 | 用途 |
|-----------|------|------|
| Spring Boot | 2.7.18 | 应用框架 |
| Java | 1.8 | 开发语言 |
| MySQL | 8.0.33 | 数据库 |
| Spring Data JPA | - | ORM框架 |
| Spring Security | - | 安全框架 |
| JWT | 0.11.5 | 认证机制 |
| Flyway | 9.22.3 | 数据库迁移 |
| SpringDoc OpenAPI | 1.7.0 | API文档 |
| log4j | 1.2.17 | 日志框架 |

## 3. 项目架构

### 3.1 整体架构

系统采用典型的分层架构，结合端口适配器模式(Port and Adapter)，将核心业务逻辑与外部依赖分离，提高系统的可扩展性和可测试性。

```
┌─────────────────────────────────────────────────────────┐
│                     表现层 (Controller)                  │
└─────────────────────────────┬───────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│                     业务层 (Service)                    │
└─────────────────────────────┬───────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│                   领域层 (Core Domain)                  │
└─────────────────────────────┬───────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│                   数据访问层 (Repository)               │
└─────────────────────────────┬───────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│                   基础设施层 (Infrastructure)           │
└─────────────────────────────────────────────────────────┘
```

### 3.2 核心模块划分

| 模块 | 主要职责 | 关键类/接口 |
|------|----------|-------------|
| 认证授权 | 用户登录、JWT令牌管理 | AuthServiceImpl, JwtTokenProvider |
| 治疗管理 | 治疗方案、会话、事件管理 | TreatmentServiceImpl, TreatmentExecutor |
| 设备控制 | 设备命令发送、状态管理 | DefaultTreatmentExecutor, CommandGateway |
| 实验管理 | 实验组、受试者管理 | GroupServiceImpl, SubjectRepository |
| 问卷管理 | 问卷模板、结果管理 | SurveyServiceImpl, SurveyTemplateRepository |

## 4. 代码组织结构

```
com.lontri.lighttherapy
├── common/           # 通用组件
│   ├── ApiResponse.java
│   ├── BizException.java
│   ├── GlobalExceptionHandler.java
│   └── TraceIdFilter.java
├── config/           # 配置类
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   └── SecurityConfig.java
├── controller/       # REST API控制器
│   ├── AuthController.java
│   ├── TreatmentController.java
│   └── UserAdminController.java
├── device/           # 设备相关
│   ├── codec/        # 命令编解码
│   └── template/     # 命令模板
├── dto/              # 数据传输对象
│   ├── AuthDtos.java
│   └── TreatmentDtos.java
├── entity/           # 实体类
│   ├── TreatmentSession.java
│   └── User.java
├── executor/         # 治疗执行器
│   ├── port/         # 端口定义
│   ├── util/         # 工具类
│   └── DefaultTreatmentExecutor.java
├── infra/            # 基础设施实现
│   ├── command/      # 命令发送实现
│   ├── device/       # 设备选择实现
│   └── session/      # 会话存储实现
├── repository/       # 数据访问
│   ├── TreatmentSessionRepository.java
│   └── UserRepository.java
├── service/          # 业务服务
│   ├── impl/         # 服务实现
│   ├── AuthService.java
│   └── TreatmentService.java
└── util/             # 通用工具
```

## 5. 核心业务逻辑

### 5.1 治疗会话生命周期

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   PLANNED   │────▶│   RUNNING   │────▶│    DONE     │
└─────────────┘     └─────────────┘     └─────────────┘
         │                      │
         │                      ▼
         │               ┌─────────────┐
         └───────────────▶│   FAILED    │
         │               └─────────────┘
         │
         ▼
┌─────────────┐
│  CANCELLED  │
└─────────────┘
```

### 5.2 治疗执行流程

1. 用户创建治疗会话，指定受试者和治疗方案
2. 系统验证受试者状态，确保无正在进行的治疗
3. 系统根据治疗方案选择合适的设备
4. 系统启动治疗会话，状态变为RUNNING
5. 治疗执行器根据治疗方案的阶段参数，通过命令网关向设备发送控制命令
6. 治疗过程中记录事件日志
7. 治疗完成或取消，系统恢复设备默认状态
8. 治疗会话状态更新为DONE/FAILED/CANCELLED

### 5.3 设备控制机制

- 支持TCP和UDP两种通信协议
- 采用命令网关路由器(CommandGatewayRouter)动态选择通信方式
- 命令发送前进行构建和验证
- 支持多设备并发控制
- 自动恢复设备默认状态

## 6. 数据流设计

### 6.1 治疗数据流程

```
客户端 API请求 → Controller → Service → TreatmentExecutor → CommandGateway → 设备
                                 ↓                ↓
                           Repository ← EventSink ←
                                 ↓
                               数据库
```

### 6.2 认证数据流程

```
用户登录 → AuthController → AuthServiceImpl → UserRepository → 数据库
                       ↓
                  JwtTokenProvider → 返回JWT令牌
```

## 7. 关键技术实现

### 7.1 JWT认证机制

- 使用BCrypt进行密码加密
- 令牌包含用户ID和角色信息
- 支持令牌过期验证
- 集成Spring Security进行权限控制

### 7.2 治疗执行器

- 使用线程池管理并发治疗
- 支持方案治疗和手动治疗两种模式
- 实现了治疗阶段的自动切换
- 具备设备命令发送的容错机制
- 支持治疗会话的暂停、恢复和终止

### 7.3 设备选择策略

- 根据治疗方案选择合适的设备
- 支持多设备同时治疗
- 设备可用性验证

### 7.4 事件驱动设计

- 治疗过程中的关键节点记录事件
- 支持自定义事件类型
- 事件数据持久化存储

## 8. 开发环境配置

### 8.1 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/light_therapy?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Tokyo&allowPublicKeyRetrieval=true
    username: lontri
    password: Admin123!
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 8.2 JWT配置

```yaml
app:
  jwt:
    secret: "change-me-to-a-long-random-secret-change-me"
    expireSeconds: 86400
```

### 8.3 治疗默认值配置

```yaml
treatment:
  default:
    lux: 50
    cctK: 4000
```

## 9. 数据库设计

### 9.1 核心表结构

| 表名 | 主要字段 | 用途 |
|------|----------|------|
| user | id, username, password_hash, role | 用户信息 |
| subject | id, user_id, subject_code, display_name | 受试者信息 |
| treatment_session | id, subject_id, scheme_id, status, start_time, end_time | 治疗会话 |
| treatment_event | id, session_id, event_type, message, event_time | 治疗事件 |
| scheme | id, name, description | 治疗方案 |
| scheme_stage | id, scheme_id, stage_no, light_intensity, light_color_temp, duration_minutes | 治疗阶段 |
| device | id, device_sn, device_type, gateway_id, status | 设备信息 |

### 9.2 数据库迁移

使用Flyway进行数据库版本管理，迁移脚本位于`src/main/resources/db/migration/`目录。

## 10. API设计

### 10.1 认证API

| 接口 | 方法 | 路径 | 功能 |
|------|------|------|------|
| 用户登录 | POST | /api/auth/login | 用户登录获取JWT令牌 |

### 10.2 治疗API

| 接口 | 方法 | 路径 | 功能 |
|------|------|------|------|
| 创建治疗会话 | POST | /api/treatment/sessions | 创建治疗会话 |
| 启动治疗 | PUT | /api/treatment/sessions/{id}/start | 启动治疗会话 |
| 结束治疗 | PUT | /api/treatment/sessions/{id}/end | 结束治疗会话 |
| 取消治疗 | PUT | /api/treatment/sessions/{id}/cancel | 取消治疗会话 |
| 获取治疗事件 | GET | /api/treatment/sessions/{id}/events | 获取治疗事件列表 |

## 11. 系统安全

### 11.1 认证与授权

- 基于JWT的无状态认证
- 角色-based访问控制
- 密码BCrypt加密存储

### 11.2 输入验证

- 使用Spring Validation进行请求参数验证
- 统一的异常处理机制

### 11.3 安全头配置

- 集成Spring Security安全头
- 支持CORS配置

## 12. 监控与日志

### 12.1 日志记录

- 使用log4j进行日志管理
- 治疗过程关键节点记录
- 设备命令发送日志

### 12.2 事件监控

- 治疗事件持久化
- 支持事件查询和分析

## 13. 部署与运行

### 13.1 打包方式

- WAR包格式，可部署到Tomcat等Servlet容器
- 支持Spring Boot内嵌Tomcat运行

### 13.2 启动命令

```bash
# 使用内嵌Tomcat运行
java -jar light-therapy-backend-1.0.0.war

# 部署到Tomcat
cp light-therapy-backend-1.0.0.war $CATALINA_HOME/webapps/
```

## 14. 项目亮点与优势

1. **模块化设计**：清晰的模块划分，便于维护和扩展
2. **端口适配器架构**：核心业务逻辑与外部依赖解耦，提高系统可测试性
3. **灵活的设备控制**：支持多种通信协议和设备类型
4. **完整的治疗生命周期管理**：从计划到执行再到结束的全流程管理
5. **强大的事件记录**：详细的治疗过程事件，便于分析和追溯
6. **良好的安全性设计**：JWT认证、密码加密、权限控制

## 15. 改进建议

1. **完善文档**：增加API文档和业务流程文档
2. **添加单元测试和集成测试**：提高代码质量和系统稳定性
3. **优化日志系统**：考虑使用ELK等日志分析工具
4. **添加监控指标**：集成Prometheus和Grafana进行系统监控
5. **优化数据库查询**：添加适当的索引，优化查询性能
6. **考虑使用更现代的日志框架**：如logback或log4j2替代旧版log4j

## 16. 总结

本项目是一个功能完整、架构清晰的光疗后端系统，采用了现代的Java技术栈和设计模式。系统支持完整的光疗治疗流程管理，包括治疗方案设计、设备控制、治疗过程监控和数据记录。通过模块化设计和端口适配器架构，系统具有良好的可扩展性和可维护性，能够适应未来业务需求的变化。