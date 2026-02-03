# 后端Java服务开发规划

## 1. 技术栈选择

| 技术/框架 | 版本 | 用途 |
|-----------|------|------|
| Java | 8 | 开发语言 |
| Spring | 4.3.x | 应用框架 |
| Spring MVC | 4.3.x | Web框架 |
| Hibernate | 3.2.x | 数据操作 |
| MySQL | 8.0.x | 数据库 |
| JWT | - | 身份认证 |
| Log4j2 | 2.21.x | 日志管理 |
| Tomcat | 8.x | 运行 WAR/JAR 包 |
| Maven | 3.8.x | 项目构建工具 |

## 2. 项目结构设计

```
light-therapy-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── lighttherapy/
│   │   │           ├── LightTherapyApplication.java          # 应用入口（传统Spring配置）
│   │   │           ├── config/                               # 配置类
│   │   │           │   ├── AppConfig.java                    # 应用配置（@Configuration）
│   │   │           │   ├── WebConfig.java                    # Web配置（@EnableWebMvc）
│   │   │           │   ├── SecurityConfig.java               # 安全配置
│   │   │           │   ├── JwtConfig.java                    # JWT配置
│   │   │           │   └── HibernateConfig.java              # Hibernate配置
│   │   │           ├── controller/                           # 控制器层
│   │   │           │   ├── AuthController.java              # 认证控制器
│   │   │           │   ├── UserController.java              # 用户控制器
│   │   │           │   ├── GroupController.java             # 分组控制器
│   │   │           │   ├── SchemeController.java            # 方案控制器
│   │   │           │   ├── TreatmentLogController.java      # 治疗记录控制器
│   │   │           │   ├── SurveyController.java            # 问卷控制器
│   │   │           │   └── BackupController.java            # 备份控制器
│   │   │           ├── service/                              # 服务层
│   │   │           │   ├── AuthService.java                 # 认证服务
│   │   │           │   ├── UserService.java                 # 用户服务
│   │   │           │   ├── GroupService.java                # 分组服务
│   │   │           │   ├── SchemeService.java               # 方案服务
│   │   │           │   ├── TreatmentLogService.java         # 治疗记录服务
│   │   │           │   ├── SurveyService.java               # 问卷服务
│   │   │           │   └── BackupService.java               # 备份服务
│   │   │           ├── dao/                                  # 数据访问层（DAO）
│   │   │           │   ├── UserDAO.java                     # 用户DAO
│   │   │           │   ├── GroupDAO.java                    # 分组DAO
│   │   │           │   ├── SchemeDAO.java                   # 方案DAO
│   │   │           │   ├── SchemeStageDAO.java              # 方案阶段DAO
│   │   │           │   ├── TreatmentLogDAO.java             # 治疗记录DAO
│   │   │           │   ├── SurveyTemplateDAO.java           # 问卷模板DAO
│   │   │           │   ├── SurveyQuestionDAO.java           # 问卷问题DAO
│   │   │           │   ├── SurveyResultDAO.java             # 问卷结果DAO
│   │   │           │   └── DataBackupDAO.java               # 数据备份DAO
│   │   │           ├── model/                                # 数据模型
│   │   │           │   ├── entity/                          # 实体类
│   │   │           │   │   ├── User.java                    # 用户实体（@Entity）
│   │   │           │   │   ├── Group.java                   # 分组实体
│   │   │           │   │   ├── Scheme.java                  # 方案实体
│   │   │           │   │   ├── SchemeStage.java             # 方案阶段实体
│   │   │           │   │   ├── TreatmentLog.java            # 治疗记录实体
│   │   │           │   │   ├── SurveyTemplate.java          # 问卷模板实体
│   │   │           │   │   ├── SurveyQuestion.java          # 问卷问题实体
│   │   │           │   │   ├── SurveyResult.java            # 问卷结果实体
│   │   │           │   │   └── DataBackup.java              # 数据备份实体
│   │   │           │   ├── dto/                             # 数据传输对象
│   │   │           │   │   ├── LoginRequest.java            # 登录请求DTO
│   │   │           │   │   ├── LoginResponse.java           # 登录响应DTO
│   │   │           │   │   ├── UserDTO.java                 # 用户DTO
│   │   │           │   │   ├── GroupDTO.java                # 分组DTO
│   │   │           │   │   ├── SchemeDTO.java               # 方案DTO
│   │   │           │   │   ├── SchemeStageDTO.java          # 方案阶段DTO
│   │   │           │   │   ├── TreatmentLogDTO.java         # 治疗记录DTO
│   │   │           │   │   ├── SurveyTemplateDTO.java       # 问卷模板DTO
│   │   │           │   │   ├── SurveyQuestionDTO.java       # 问卷问题DTO
│   │   │           │   │   ├── SurveyResultDTO.java         # 问卷结果DTO
│   │   │           │   │   └── DataBackupDTO.java           # 数据备份DTO
│   │   │           │   └── enums/                           # 枚举类
│   │   │           │       ├── Role.java                    # 角色枚举
│   │   │           │       ├── Gender.java                  # 性别枚举
│   │   │           │       ├── QuestionType.java            # 问题类型枚举
│   │   │           │       ├── BackupType.java              # 备份类型枚举
│   │   │           │       └── BackupStatus.java            # 备份状态枚举
│   │   │           ├── security/                            # 安全相关
│   │   │           │   ├── JwtTokenProvider.java            # JWT令牌生成器
│   │   │           │   ├── JwtAuthenticationFilter.java     # JWT认证过滤器
│   │   │           │   └── CustomUserDetailsService.java    # 自定义用户详情服务
│   │   │           ├── exception/                           # 异常处理
│   │   │           │   ├── ResourceNotFoundException.java   # 资源未找到异常
│   │   │           │   ├── InvalidRequestException.java     # 无效请求异常
│   │   │           │   ├── AuthenticationException.java     # 认证异常
│   │   │           │   └── GlobalExceptionHandler.java      # 全局异常处理器
│   │   │           └── util/                                # 工具类
│   │   │               ├── DateUtils.java                   # 日期工具类
│   │   │               ├── FileUtils.java                   # 文件工具类
│   │   │               ├── ExcelUtils.java                  # Excel导出工具类
│   │   │               └── HTMLUtils.java                   # HTML生成工具类
│   │   └── resources/                                       # 资源文件
│   │       ├── applicationContext.xml                       # Spring配置文件
│   │       ├── spring-mvc.xml                               # Spring MVC配置
│   │       ├── hibernate.cfg.xml                            # Hibernate配置
│   │       ├── log4j2.xml                                   # 日志配置
│   │       └── db/                                          # 数据库脚本
│   │           ├── schema.sql                               # 表结构脚本
│   │           └── data.sql                                 # 初始数据脚本
│   └── test/                                                # 测试代码
│       └── java/
│           └── com/
│               └── lighttherapy/
│                   ├── controller/                          # 控制器测试
│                   ├── service/                             # 服务层测试
│                   └── dao/                                 # 数据访问层测试
├── pom.xml                                                  # Maven配置文件
├── README.md                                                # 项目说明文档
└── src/main/webapp/                                        # Web资源目录
    ├── WEB-INF/
    │   └── web.xml                                         # Web部署描述符
    └── index.jsp                                           # 首页

## 3. API接口设计

### 3.1 认证接口

| API路径 | 方法 | 功能 | 权限 |
|---------|------|------|------|
| /api/auth/login | POST | 用户登录 | 公共 |
| /api/auth/logout | POST | 用户登出 | 登录用户 |
| /api/auth/refresh | POST | 刷新Token | 登录用户 |

### 3.2 用户接口

| API路径 | 方法 | 功能 | 权限 |
|---------|------|------|------|
| /api/users | GET | 获取用户列表 | 研究者 |
| /api/users/{id} | GET | 获取用户详情 | 研究者/本人 |
| /api/users | POST | 创建用户 | 研究者 |
| /api/users/{id} | PUT | 更新用户信息 | 研究者/本人 |
| /api/users/{id} | DELETE | 删除用户 | 研究者 |

### 3.3 分组接口

| API路径 | 方法 | 功能 | 权限 |
|---------|------|------|------|
| /api/groups | GET | 获取分组列表 | 研究者 |
| /api/groups | POST | 创建分组 | 研究者 |
| /api/groups/{id} | DELETE | 删除分组 | 研究者 |

### 3.4 方案接口

| API路径 | 方法 | 功能 | 权限 |
|---------|------|------|------|
| /api/schemes | GET | 获取方案列表 | 所有用户 |
| /api/schemes/{id} | GET | 获取方案详情 | 所有用户 |
| /api/schemes | POST | 创建方案 | 研究者 |
| /api/schemes/{id} | PUT | 更新方案 | 研究者 |
| /api/schemes/{id} | DELETE | 删除方案 | 研究者 |

### 3.5 治疗记录接口

| API路径 | 方法 | 功能 | 权限 |
|---------|------|------|------|
| /api/treatment-logs | GET | 获取治疗记录列表 | 研究者 |
| /api/treatment-logs/user/{userId} | GET | 获取用户治疗记录 | 研究者/本人 |
| /api/treatment-logs/{id} | GET | 获取治疗记录详情 | 研究者/本人 |
| /api/treatment-logs | POST | 创建治疗记录 | 所有用户 |
| /api/treatment-logs/export | POST | 导出治疗记录 | 研究者 |
| /api/treatment-logs/clinical-report/{userId} | GET | 生成临床报告 | 研究者 |

### 3.6 问卷接口

| API路径 | 方法 | 功能 | 权限 |
|---------|------|------|------|
| /api/surveys/templates | GET | 获取问卷模板列表 | 研究者 |
| /api/surveys/templates/active | GET | 获取激活的问卷模板 | 所有用户 |
| /api/surveys/templates | POST | 创建问卷模板 | 研究者 |
| /api/surveys/templates/{id} | PUT | 更新问卷模板 | 研究者 |
| /api/surveys/templates/{id}/activate | PUT | 激活问卷模板 | 研究者 |
| /api/surveys/results | POST | 提交问卷结果 | 所有用户 |
| /api/surveys/results/user/{userId} | GET | 获取用户问卷结果 | 研究者/本人 |

### 3.7 数据备份接口

| API路径 | 方法 | 功能 | 权限 |
|---------|------|------|------|
| /api/backups | GET | 获取备份列表 | 研究者 |
| /api/backups | POST | 创建备份 | 研究者 |
| /api/backups/{id} | GET | 下载备份文件 | 研究者 |
| /api/backups/{id} | DELETE | 删除备份 | 研究者 |
| /api/backups/restore | POST | 恢复备份 | 研究者 |

### 3.8 受试者专用接口详细说明

#### 3.8.1 认证接口

##### /api/auth/login - POST
- **功能**：用户登录
- **权限**：公共
- **请求参数**：
  ```json
  {
    "username": "string",  // 受试者ID或研究者账号
    "password": "string",  // 密码
    "role": "string"        // subject 或 researcher
  }
  ```
- **响应格式**：
  ```json
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "string",    // JWT令牌
      "userInfo": {
        "id": "string",     // 用户ID
        "role": "string",   // 用户角色
        "name": "string",   // 用户名
        "groupId": "string",// 所属分组ID（受试者）
        "groupName": "string"// 所属分组名称（受试者）
      }
    }
  }
  ```

#### 3.8.2 受试者个人信息接口

##### /api/users/me - GET
- **功能**：获取当前登录受试者的个人信息
- **权限**：受试者
- **响应格式**：
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "id": "string",
      "name": "string",
      "gender": "string",
      "age": "number",
      "groupId": "string",
      "groupName": "string",
      "researcherName": "string",
      "contactInfo": "string",
      "enrollmentDate": "string",
      "diagnosisResult": "string"
    }
  }
  ```

##### /api/users/me - PUT
- **功能**：更新当前登录受试者的个人信息
- **权限**：受试者
- **请求参数**：
  ```json
  {
    "name": "string",
    "gender": "string",
    "age": "number",
    "contactInfo": "string"
  }
  ```

#### 3.8.3 治疗控制接口

##### /api/treatment/start - POST
- **功能**：开始治疗
- **权限**：受试者
- **请求参数**：
  ```json
  {
    "schemeId": "number",  // 方案ID，为null时表示手动模式
    "manualParams": {       // 手动模式参数
      "brightness": "number", // 亮度（Lux）
      "temp": "number"       // 色温（K）
    }
  }
  ```
- **响应格式**：
  ```json
  {
    "code": 200,
    "message": "治疗已开始",
    "data": {
      "treatmentId": "number",  // 治疗记录ID
      "startTime": "string"    // 开始时间
    }
  }
  ```

##### /api/treatment/stop - POST
- **功能**：停止治疗
- **权限**：受试者
- **请求参数**：
  ```json
  {
    "treatmentId": "number",  // 治疗记录ID
    "status": "string"        // 治疗状态：自动完成/手动停止
  }
  ```

#### 3.8.4 受试者治疗记录接口

##### /api/treatment-logs/my - GET
- **功能**：获取当前受试者的治疗记录列表
- **权限**：受试者
- **响应格式**：
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": [
      {
        "id": "number",
        "date": "string",
        "startTime": "string",
        "endTime": "string",
        "durationActual": "number",
        "schemeName": "string",
        "status": "string"
      }
    ]
  }
  ```

##### /api/treatment-logs/my/{id} - GET
- **功能**：获取当前受试者的单条治疗记录详情
- **权限**：受试者
- **响应格式**：
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": {
      "id": "number",
      "date": "string",
      "startTime": "string",
      "endTime": "string",
      "durationActual": "number",
      "schemeName": "string",
      "schemeId": "number",
      "status": "string",
      "manualBrightness": "number",
      "manualTemp": "number"
    }
  }
  ```

#### 3.8.5 受试者问卷接口

##### /api/surveys/my - GET
- **功能**：获取当前受试者的问卷历史
- **权限**：受试者
- **响应格式**：
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": [
      {
        "id": "number",
        "treatmentLogId": "number",
        "date": "string",
        "templateId": "number",
        "templateName": "string",
        "answers": "object",
        "totalScore": "number"
      }
    ]
  }
  ```

#### 3.8.6 错误处理机制

所有API接口遵循统一的错误响应格式：

```json
{
  "code": "number",  // 错误代码
  "message": "string", // 错误信息
  "data": "object"    // 错误详情（可选）
}
```

常见错误代码：
- 400：请求参数错误
- 401：未授权或令牌失效
- 403：权限不足
- 404：资源未找到
- 500：服务器内部错误

针对受试者的特殊错误情况：
- 4001：治疗正在进行中，无法重复开始
- 4002：治疗未开始，无法停止
- 4003：方案不存在或不可用
- 4004：问卷模板未找到或未激活

## 4. 业务逻辑实现

### 4.1 认证与授权
- JWT令牌生成与验证
- 用户身份认证（支持研究者和受试者）
- 基于角色的权限控制
- 会话管理
- 受试者数据隔离机制

### 4.2 用户管理
#### 4.2.1 研究者功能
- 受试者信息CRUD
- 分组管理
- 用户角色管理

#### 4.2.2 受试者功能
- 本人信息查看与修改
- 密码修改
- 个人治疗数据查看

### 4.3 治疗方案管理
#### 4.3.1 研究者功能
- 方案CRUD
- 方案阶段管理
- 方案状态管理（系统预设/自定义）

#### 4.3.2 受试者功能
- 查看可用方案列表
- 查看方案详情
- 选择方案进行治疗

### 4.4 治疗记录管理
#### 4.4.1 研究者功能
- 治疗记录生成
- 治疗记录查询与统计
- 治疗记录导出
- 临床报告生成

#### 4.4.2 受试者功能
- 查看本人治疗记录
- 查看单条治疗记录详情

### 4.5 问卷管理
#### 4.5.1 研究者功能
- 问卷模板CRUD
- 问卷问题管理
- 问卷结果查询与统计

#### 4.5.2 受试者功能
- 查看当前激活的问卷模板
- 填写并提交问卷
- 查看本人问卷历史

### 4.6 数据管理
#### 4.6.1 研究者功能
- 数据备份（全量/增量）
- 数据恢复
- 数据导出（CSV/Excel）
- 临床报告生成（HTML）

#### 4.6.2 受试者功能
- 导出本人治疗数据（可选）

## 5. 数据访问层开发

### 5.1 实体类设计
- 基于数据库表结构设计实体类
- 配置Hibernate注解映射关系（@Entity, @Table, @Column等）
- 定义实体间关联关系（@OneToMany, @ManyToOne, @ManyToMany等）
- 配置主键生成策略（@GeneratedValue）

#### 5.1.1 核心实体类

| 实体类 | 主要字段 | 描述 | 与受试者的关系 |
|--------|----------|------|----------------|
| User | id, username, password, role, groupId, researcherName | 用户实体（研究者/受试者） | 受试者的核心身份信息 |
| Group | id, name, description | 分组实体 | 受试者所属分组 |
| Scheme | id, name, description, totalDuration, isCustom | 治疗方案实体 | 受试者可选择的治疗方案 |
| SchemeStage | id, schemeId, stageOrder, brightness, temp, duration | 方案阶段实体 | 方案的具体阶段配置 |
| TreatmentLog | id, userId, schemeId, schemeName, date, startTime, endTime, durationActual, status | 治疗记录实体 | 受试者的治疗记录 |
| SurveyTemplate | id, name, description, simplifiedMode, isActive | 问卷模板实体 | 受试者治疗后需填写的问卷 |
| SurveyQuestion | id, templateId, questionOrder, type, label, minValue, maxValue, options, required | 问卷问题实体 | 问卷模板的具体问题 |
| SurveyResult | id, userId, treatmentLogId, templateId, date, answers, totalScore | 问卷结果实体 | 受试者填写的问卷结果 |

### 5.2 DAO接口设计
- 定义DAO接口，声明数据操作方法
- 实现DAO接口，使用Hibernate Session进行数据操作
- 使用HQL或Criteria API实现复杂查询
- 封装通用的CRUD操作到基类DAO

#### 5.2.1 受试者相关DAO方法

| DAO名称 | 方法名称 | 功能描述 |
|---------|----------|----------|
| UserDAO | findByUsername(String username) | 根据用户名查找用户 |
| UserDAO | updateUserInfo(User user) | 更新用户信息 |
| SchemeDAO | findAllAvailable() | 查询所有可用方案 |
| TreatmentLogDAO | findByUserId(Long userId) | 查询指定用户的治疗记录 |
| TreatmentLogDAO | saveTreatmentLog(TreatmentLog log) | 保存治疗记录 |
| SurveyTemplateDAO | findActiveTemplate() | 查询当前激活的问卷模板 |
| SurveyResultDAO | saveSurveyResult(SurveyResult result) | 保存问卷结果 |
| SurveyResultDAO | findByUserId(Long userId) | 查询指定用户的问卷结果 |

### 5.3 事务管理
- 使用@Transactional注解管理事务
- 配置事务传播行为（Propagation）
- 处理事务回滚（RollbackFor）
- 配置事务管理器（HibernateTransactionManager）

### 5.4 Hibernate配置
- 配置SessionFactory
- 配置数据源（DataSource）
- 配置Hibernate属性（方言、显示SQL、自动建表等）
- 配置二级缓存（可选）

### 5.5 与其他系统模块的交互逻辑

#### 5.5.1 受试者治疗流程交互
1. 受试者选择治疗方案或手动设置参数
2. 调用TreatmentLogDAO.saveTreatmentLog()创建治疗记录
3. 治疗过程中实时更新治疗状态
4. 治疗结束后，调用SurveyTemplateDAO.findActiveTemplate()获取问卷模板
5. 受试者填写问卷后，调用SurveyResultDAO.saveSurveyResult()保存问卷结果

#### 5.5.2 受试者数据查询交互
1. 受试者登录后，调用UserDAO.findByUsername()获取个人信息
2. 查看治疗记录时，调用TreatmentLogDAO.findByUserId()获取本人记录
3. 查看问卷历史时，调用SurveyResultDAO.findByUserId()获取本人问卷结果

#### 5.5.3 研究者与受试者数据交互
1. 研究者创建受试者时，调用UserDAO.save()保存受试者信息
2. 研究者创建方案时，调用SchemeDAO.save()保存方案
3. 研究者查看受试者数据时，调用相应DAO方法查询，但需验证权限
4. 研究者生成临床报告时，调用TreatmentLogDAO和SurveyResultDAO联合查询数据

## 6. 安全认证机制

### 6.1 角色定义

| 角色名称 | 角色描述 | 权限范围 |
|---------|----------|----------|
| 研究者（researcher） | 系统管理员，负责管理受试者、方案、问卷等 | 所有功能模块的完全访问权限 |
| 受试者（subject） | 接受光疗治疗的患者 | 仅访问与本人相关的功能，如光疗控制、治疗记录查看、问卷填写等 |

### 6.2 JWT认证流程
1. 用户登录，系统验证身份和角色
2. 生成JWT令牌（包含用户ID、角色、过期时间等信息）
3. 将令牌返回给客户端
4. 客户端每次请求携带令牌
5. 服务器验证令牌有效性和角色权限
6. 授权访问请求资源

### 6.3 权限控制
- 使用Spring Security实现基于角色的权限控制
- 使用@PreAuthorize注解进行方法级权限控制
- 配置URL级别的权限控制
- 针对受试者角色，添加数据隔离机制，确保只能访问本人数据

### 6.4 密码安全
- 使用BCrypt算法加密存储密码
- 密码复杂度验证
- 防止暴力破解（登录失败次数限制）
- 支持密码重置功能

## 7. 异常处理与日志记录

### 7.1 异常处理
- 自定义业务异常
- 全局异常处理器
- 统一错误响应格式

### 7.2 日志记录
- 使用Log4j2记录系统日志
- 分级别记录日志（DEBUG、INFO、WARN、ERROR）
- 日志文件按日期滚动
- 敏感信息脱敏处理

## 8. 测试策略

### 8.1 单元测试
- 测试服务层核心逻辑
- 使用Mockito模拟依赖
- 测试覆盖率要求：≥80%

### 8.2 集成测试
- 测试控制器层
- 测试数据库交互
- 测试API接口完整性

### 8.3 端到端测试
- 模拟真实用户场景
- 测试完整业务流程

## 9. 部署策略

### 9.1 容器化部署
- 使用Docker容器化应用
- 编写Dockerfile和docker-compose.yml
- 支持多环境部署

### 9.2 环境配置
- 开发环境配置
- 测试环境配置
- 生产环境配置

### 9.3 CI/CD
- 集成Jenkins或GitHub Actions
- 自动化构建、测试、部署
- 支持蓝绿部署或金丝雀发布

## 10. 性能优化

### 10.1 缓存策略
- 使用Redis缓存热点数据
- 缓存用户会话
- 缓存问卷模板等静态数据

### 10.2 数据库优化
- 合理设计索引
- 优化查询语句
- 批量操作处理

### 10.3 并发处理
- 优化并发性能
- 合理配置线程池
- 防止并发冲突

## 11. 监控与维护

### 11.1 系统监控
- 集成Spring Boot Actuator
- 监控应用健康状态
- 监控系统指标（CPU、内存、磁盘等）

### 11.2 日志监控
- 集成ELK Stack或Graylog
- 实时日志分析
- 异常告警

### 11.3 定期维护
- 定期数据备份
- 数据库索引优化
- 日志文件清理

## 12. 开发规范

### 12.1 代码规范
- 遵循Java编码规范
- 使用Lombok简化代码
- 代码注释规范

### 12.2 命名规范
- 类名、方法名、变量名命名规范
- 包结构合理
- 常量命名规范

### 12.3 文档规范
- API文档（Swagger）
- 技术文档
- 部署文档
