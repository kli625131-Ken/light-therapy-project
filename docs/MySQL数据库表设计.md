# MySQL数据库表结构设计

## 1. 数据库概述
本数据库设计用于光疗系统，存储受试者信息、治疗方案、治疗记录、问卷数据等核心业务数据。

## 2. 表结构设计

### 2.1 用户表（user）
**功能**：存储受试者和研究者的信息

| 字段名 | 数据类型 | 长度 | 约束 | 描述 |
|--------|----------|------|------|------|
| id | VARCHAR | 50 | PRIMARY KEY | 用户ID（受试者：SUB-XXX，研究者：admin） |
| password | VARCHAR | 255 | NOT NULL | 密码（加密存储） |
| role | ENUM('subject', 'researcher') | - | NOT NULL | 用户角色：subject（受试者），researcher（研究者） |
| gender | ENUM('男', '女') | - | NULL | 性别（仅受试者） |
| age | INT | 3 | NULL | 年龄（仅受试者） |
| group_id | INT | 11 | FOREIGN KEY | 所属分组ID（仅受试者） |
| researcher_name | VARCHAR | 100 | NULL | 主试姓名（仅受试者） |
| contact_info | VARCHAR | 200 | NULL | 联系方式（仅受试者） |
| enrollment_date | DATE | - | NULL | 入组时间（仅受试者） |
| diagnosis_result | VARCHAR | 200 | NULL | 诊断结果（仅受试者） |
| last_active | DATETIME | - | NULL | 最后活跃时间 |
| created_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- INDEX idx_role (role)
- INDEX idx_group_id (group_id)
- INDEX idx_created_at (created_at)

### 2.2 分组表（`group`）
**功能**：存储受试者分组信息

| 字段名 | 数据类型 | 长度 | 约束 | 描述 |
|--------|----------|------|------|------|
| id | INT | 11 | PRIMARY KEY AUTO_INCREMENT | 分组ID |
| name | VARCHAR | 100 | NOT NULL UNIQUE | 分组名称 |
| description | TEXT | - | NULL | 分组描述 |
| created_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- UNIQUE INDEX idx_name (name)

### 2.3 治疗方案表（scheme）
**功能**：存储光疗方案信息

| 字段名 | 数据类型 | 长度 | 约束 | 描述 |
|--------|----------|------|------|------|
| id | INT | 11 | PRIMARY KEY AUTO_INCREMENT | 方案ID |
| name | VARCHAR | 200 | NOT NULL | 方案名称 |
| description | TEXT | - | NULL | 方案描述 |
| total_duration | INT | 11 | NOT NULL | 总时长（分钟） |
| is_custom | TINYINT | 1 | NOT NULL DEFAULT 0 | 是否自定义：0-系统预设，1-自定义 |
| created_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- INDEX idx_is_custom (is_custom)
- INDEX idx_created_at (created_at)

### 2.4 方案阶段表（scheme_stage）
**功能**：存储光疗方案的各个阶段

| 字段名 | 数据类型 | 长度 | 约束 | 描述 |
|--------|----------|------|------|------|
| id | INT | 11 | PRIMARY KEY AUTO_INCREMENT | 阶段ID |
| scheme_id | INT | 11 | NOT NULL FOREIGN KEY | 所属方案ID |
| stage_order | INT | 11 | NOT NULL | 阶段顺序 |
| brightness | INT | 11 | NOT NULL | 亮度（Lux） |
| temp | INT | 11 | NOT NULL | 色温（K） |
| duration | INT | 11 | NOT NULL | 阶段时长（分钟） |
| created_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- INDEX idx_scheme_id (scheme_id)
- INDEX idx_scheme_order (scheme_id, stage_order)

**外键**：
- FOREIGN KEY (scheme_id) REFERENCES scheme(id) ON DELETE CASCADE

### 2.5 治疗记录表（treatment_log）
**功能**：存储治疗记录

| 字段名 | 数据类型 | 长度 | 约束 | 描述 |
|--------|----------|------|------|------|
| id | BIGINT | 20 | PRIMARY KEY AUTO_INCREMENT | 记录ID |
| user_id | VARCHAR | 50 | NOT NULL FOREIGN KEY | 受试者ID |
| scheme_id | INT | 11 | NULL FOREIGN KEY | 方案ID（手动模式为NULL） |
| scheme_name | VARCHAR | 200 | NOT NULL | 方案名称（手动模式为'自定义手动模式'） |
| date | DATE | - | NOT NULL | 治疗日期 |
| start_time | TIME | - | NOT NULL | 开始时间 |
| end_time | TIME | - | NOT NULL | 结束时间 |
| duration_actual | INT | 11 | NOT NULL | 实际时长（分钟） |
| status | ENUM('自动完成', '手动停止') | - | NOT NULL | 治疗状态 |
| manual_brightness | INT | 11 | NULL | 手动模式下的亮度（Lux） |
| manual_temp | INT | 11 | NULL | 手动模式下的色温（K） |
| created_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- INDEX idx_user_id (user_id)
- INDEX idx_scheme_id (scheme_id)
- INDEX idx_date (date)
- INDEX idx_created_at (created_at)

**外键**：
- FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
- FOREIGN KEY (scheme_id) REFERENCES scheme(id) ON DELETE SET NULL

### 2.6 问卷模板表（survey_template）
**功能**：存储问卷模板

| 字段名 | 数据类型 | 长度 | 约束 | 描述 |
|--------|----------|------|------|------|
| id | INT | 11 | PRIMARY KEY AUTO_INCREMENT | 模板ID |
| name | VARCHAR | 200 | NOT NULL | 模板名称 |
| description | TEXT | - | NULL | 模板描述 |
| simplified_mode | TINYINT | 1 | NOT NULL DEFAULT 0 | 是否简化模式：0-完整模式，1-简化模式 |
| is_active | TINYINT | 1 | NOT NULL DEFAULT 1 | 是否激活：0-未激活，1-激活 |
| created_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- INDEX idx_is_active (is_active)
- INDEX idx_created_at (created_at)

### 2.7 问卷问题表（survey_question）
**功能**：存储问卷问题

| 字段名 | 数据类型 | 长度 | 约束 | 描述 |
|--------|----------|------|------|------|
| id | INT | 11 | PRIMARY KEY AUTO_INCREMENT | 问题ID |
| template_id | INT | 11 | NOT NULL FOREIGN KEY | 所属模板ID |
| question_order | INT | 11 | NOT NULL | 问题顺序 |
| type | ENUM('range', 'select', 'text') | - | NOT NULL | 问题类型：range（评分题），select（选择题），text（文本题） |
| label | TEXT | - | NOT NULL | 问题内容 |
| min_value | INT | 11 | NULL | 评分题最小值 |
| max_value | INT | 11 | NULL | 评分题最大值 |
| options | TEXT | - | NULL | 选择题选项（JSON格式：["选项1", "选项2"]） |
| required | TINYINT | 1 | NOT NULL DEFAULT 1 | 是否必填：0-选填，1-必填 |
| created_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- INDEX idx_template_id (template_id)
- INDEX idx_template_order (template_id, question_order)

**外键**：
- FOREIGN KEY (template_id) REFERENCES survey_template(id) ON DELETE CASCADE

### 2.8 问卷结果表（survey_result）
**功能**：存储问卷填写结果

| 字段名 | 数据类型 | 长度 | 约束 | 描述 |
|--------|----------|------|------|------|
| id | BIGINT | 20 | PRIMARY KEY AUTO_INCREMENT | 结果ID |
| user_id | VARCHAR | 50 | NOT NULL FOREIGN KEY | 受试者ID |
| treatment_log_id | BIGINT | 20 | NOT NULL FOREIGN KEY | 关联的治疗记录ID |
| template_id | INT | 11 | NOT NULL FOREIGN KEY | 使用的问卷模板ID |
| answers | TEXT | - | NOT NULL | 问卷答案（JSON格式：{"question_id": "answer"}） |
| total_score | INT | 11 | NULL | 总得分（简化模式下使用） |
| date | DATE | - | NOT NULL | 填写日期 |
| created_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- INDEX idx_user_id (user_id)
- INDEX idx_treatment_log_id (treatment_log_id)
- INDEX idx_template_id (template_id)
- INDEX idx_date (date)

**外键**：
- FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
- FOREIGN KEY (treatment_log_id) REFERENCES treatment_log(id) ON DELETE CASCADE
- FOREIGN KEY (template_id) REFERENCES survey_template(id) ON DELETE CASCADE

### 2.9 数据备份表（data_backup）
**功能**：存储数据备份记录

| 字段名 | 数据类型 | 长度 | 约束 | 描述 |
|--------|----------|------|------|------|
| id | INT | 11 | PRIMARY KEY AUTO_INCREMENT | 备份ID |
| backup_filename | VARCHAR | 255 | NOT NULL | 备份文件名 |
| backup_path | VARCHAR | 500 | NOT NULL | 备份文件路径 |
| backup_size | BIGINT | 20 | NOT NULL | 备份文件大小（字节） |
| backup_type | ENUM('full', 'incremental') | - | NOT NULL | 备份类型：full（全量备份），incremental（增量备份） |
| status | ENUM('success', 'failed') | - | NOT NULL | 备份状态：success（成功），failed（失败） |
| created_by | VARCHAR | 50 | NOT NULL FOREIGN KEY | 备份创建者ID |
| created_at | DATETIME | - | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- INDEX idx_created_by (created_by)
- INDEX idx_created_at (created_at)
- INDEX idx_status (status)

**外键**：
- FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE CASCADE

## 3. 数据库关系图

```
+----------------+     +----------------+     +----------------+     +----------------+     +----------------+     +----------------+
|     user       |     |     group      |     |    scheme      |     | scheme_stage   |     | treatment_log  |     | survey_result  |
+----------------+     +----------------+     +----------------+     +----------------+     +----------------+     +----------------+
| id             |<----| id             |<----| id             |<----| scheme_id      |<----| scheme_id      |<----| treatment_log_id|
| password       |     | name           |     | name           |     | stage_order    |     | user_id        |<----| user_id        |
| role           |     | description    |     | description    |     | brightness     |     | scheme_name    |     | template_id    |<----+  
| gender         |     | created_at     |     | total_duration |     | temp           |     | date           |     | answers        |    |
| age            |     | updated_at     |     | is_custom      |     | duration       |     | start_time     |     | total_score    |    |
| group_id       |     +----------------+     | created_at     |     | created_at     |     | end_time       |     | date           |    |
| researcher_name|                           | updated_at     |     | updated_at     |     | duration_actual|     | created_at     |    |
| contact_info   |                           +----------------+     +----------------+     | status         |     | updated_at     |    |
| enrollment_date|                                                                      | created_at     |     +----------------+    |
| diagnosis_result|                                                                     | updated_at     |                           |
| last_active    |                                                                      +----------------+                           |
| created_at     |                                                                                                                      |
| updated_at     |                                                                                                                      |
+----------------+                                                                                                                      |
                                                                                                                                       |
+----------------+     +----------------+                                                                                              |
| survey_template|     | survey_question|                                                                                              |
+----------------+     +----------------+                                                                                              |
| id             |<----| template_id    |                                                                                              |
| name           |     | question_order |                                                                                              |
| description    |     | type           |                                                                                              |
| simplified_mode|     | label          |                                                                                              |
| is_active      |     | min_value      |                                                                                              |
| created_at     |     | max_value      |                                                                                              |
| updated_at     |     | options        |                                                                                              |
+----------------+     | required       |                                                                                              |
                       | created_at     |                                                                                              |
                       | updated_at     |                                                                                              |
                       +----------------+                                                                                              |
                                                                                                                                        |
+----------------+                                                                                                                      |
| data_backup    |                                                                                                                      |
+----------------+                                                                                                                      |
| id             |                                                                                                                      |
| backup_filename|                                                                                                                      |
| backup_path    |                                                                                                                      |
| backup_size    |                                                                                                                      |
| backup_type    |                                                                                                                      |
| status         |                                                                                                                      |
| created_by     |                                                                                                                      |
| created_at     |                                                                                                                      |
+----------------+                                                                                                                      |
```

## 4. 索引设计

### 4.1 主键索引
- 所有表的主键字段都将自动创建主键索引

### 4.2 唯一索引
- `user.id`：用户ID唯一
- `group.name`：分组名称唯一

### 4.3 普通索引
- `user.role`：按角色查询用户
- `user.group_id`：按分组查询用户
- `scheme.is_custom`：按自定义状态查询方案
- `treatment_log.user_id`：按用户查询治疗记录
- `treatment_log.scheme_id`：按方案查询治疗记录
- `treatment_log.date`：按日期查询治疗记录
- `survey_template.is_active`：查询激活的问卷模板
- `survey_question.template_id`：按模板查询问题
- `survey_result.user_id`：按用户查询问卷结果
- `survey_result.treatment_log_id`：按治疗记录查询问卷结果
- `survey_result.template_id`：按模板查询问卷结果
- `data_backup.created_by`：按创建者查询备份记录
- `data_backup.created_at`：按创建时间查询备份记录
- `data_backup.status`：按状态查询备份记录

## 5. 数据库优化建议

1. **分区表**：对于治疗记录表（treatment_log）和问卷结果表（survey_result），可以考虑按日期进行分区，提高查询性能
2. **缓存策略**：对于频繁访问的静态数据（如激活的问卷模板、预设方案），可以使用缓存机制减少数据库查询
3. **连接池配置**：合理配置数据库连接池，提高并发处理能力
4. **定期清理**：定期清理无效数据和过期备份，优化数据库性能
5. **查询优化**：使用合适的索引，避免全表扫描，优化查询语句

## 6. 安全设计

1. **密码加密**：用户密码使用bcrypt等强哈希算法加密存储
2. **SQL注入防护**：使用参数化查询，避免SQL注入攻击
3. **权限控制**：严格控制数据库用户权限，仅授予必要的操作权限
4. **数据备份**：定期进行数据备份，确保数据安全
5. **日志审计**：开启数据库操作日志，便于审计和追踪

## 7. 数据迁移策略

1. **初始数据导入**：使用SQL脚本或数据迁移工具导入初始数据
2. **增量数据同步**：对于现有系统的数据迁移，采用增量同步方式，确保数据一致性
3. **数据验证**：迁移后进行数据验证，确保数据完整性和准确性
4. **回滚机制**：制定数据迁移回滚方案，确保迁移失败时可以恢复数据
