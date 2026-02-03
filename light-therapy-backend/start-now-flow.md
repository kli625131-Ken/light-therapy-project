# api/treatments/start-now 执行流程图

```mermaid
flowchart TD
    A[客户端调用api/treatments/start-now] --> B[TreatmentServiceImpl.createAndStart]
    B --> C[create方法创建会话]
    C --> D[保存会话到数据库]
    D --> E[绑定设备到会话]
    E --> F[检查是否有绑定设备]
    F -- 无设备 --> G[抛出BizException]
    F -- 有设备 --> H[stateMachine.apply(START, null)]
    H --> I[TreatmentSessionStateMachineImpl.apply]
    I --> J[验证状态转换合法性]
    J -- 非法转换 --> K[抛出BizException]
    J -- 合法转换 --> L[更新会话状态为RUNNING]
    L --> M[保存会话到数据库]
    M --> N[注册事务同步]
    N --> O[事务提交]
    O --> P[afterCommit回调]
    P --> Q[treatmentExecutor.startScheme]
    Q --> R[创建执行器句柄]
    R --> S[提交任务到线程池]
    S --> T[runScheme方法执行]
    T --> U[获取会话和设备信息]
    U --> V[检查设备列表]
    V -- 无设备 --> W[标记会话失败]
    V -- 有设备 --> X[获取治疗阶段]
    X --> Y[sendStageToDevices发送第一阶段指令]
    Y --> Z[进入治疗循环]
    Z --> AA[管理阶段切换]
    AA --> AB[定时发送指令]
    AB --> AC[治疗结束]
    AC --> AD[恢复设备默认状态]
    
    subgraph 指令发送流程
    Y --> BA[获取设备配置]
    BA --> BB[根据设备类型构建指令]
    BB --> BC[调用safeSend发送指令]
    BC --> BD[校验设备和指令]
    BD --> BE[调用doSend发送指令]
    BE --> BF[commandGatewayRouter.send]
    BF --> BG[查找设备信息]
    BG --> BH[选择UDP/TCP发送模式]
    BH -- UDP --> BI[构建正确格式指令]
    BI --> BJ[udpSender.send]
    BJ --> BK[创建UDP套接字]
    BK --> BL[发送UDP数据包]
    BH -- TCP --> BM[tcpSender.send]
    BM --> BN[发送TCP指令]
    end
```

## 流程说明

1. **客户端请求**：客户端调用`api/treatments/start-now`接口，请求开始治疗。

2. **创建会话**：`TreatmentServiceImpl.createAndStart`方法执行，首先调用`create`方法创建治疗会话，包括：
   - 解析请求参数
   - 验证受试者和设备信息
   - 创建会话记录
   - 绑定设备到会话

3. **设备检查**：检查会话是否绑定了设备，没有设备则抛出异常。

4. **状态机处理**：调用`stateMachine.apply(START, null)`处理启动动作，包括：
   - 验证状态转换合法性
   - 更新会话状态为`RUNNING`
   - 保存会话到数据库
   - 注册事务同步，确保在事务提交后执行执行器启动

5. **执行器启动**：事务提交后，`treatmentExecutor.startScheme`执行，包括：
   - 创建执行器句柄
   - 提交任务到线程池执行`runScheme`方法

6. **治疗执行**：`runScheme`方法执行，包括：
   - 获取会话和设备信息
   - 检查设备列表
   - 获取治疗阶段
   - 发送第一阶段指令
   - 进入治疗循环，管理阶段切换和定时发送指令

7. **指令发送**：`sendStageToDevices`方法执行，包括：
   - 获取设备配置
   - 根据设备类型构建指令
   - 调用`safeSend`发送指令
   - 最终通过`CommandGatewayRouter`选择UDP或TCP方式发送指令

8. **UDP发送**：如果设备使用UDP发送模式，构建正确格式的UDP指令并发送。

9. **治疗结束**：治疗完成或中断时，恢复设备默认状态并结束治疗。

## 关键技术点

1. **事务管理**：使用Spring事务管理确保会话状态和设备绑定的一致性，执行器启动在事务提交后执行。

2. **状态机**：使用状态机管理治疗会话的生命周期，确保状态转换的合法性。

3. **异步执行**：治疗执行器使用线程池异步执行，避免阻塞API响应。

4. **UDP/TCP路由**：根据设备配置自动选择UDP或TCP方式发送指令。

5. **指令格式**：构建正确格式的指令，包括长度计算和校验。

6. **错误处理**：完善的错误处理机制，确保治疗过程中的异常能够被正确捕获和处理。

这个流程图展示了`api/treatments/start-now`接口的完整执行流程，从客户端请求到设备接收指令的整个过程，包括了事务管理、状态转换、异步执行和指令发送等关键环节。