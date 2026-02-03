package com.lontri.lighttherapy.executor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.entity.*;
import com.lontri.lighttherapy.entity.SchemeStage;
import com.lontri.lighttherapy.entity.SchemeStageDeviceConfig;
import com.lontri.lighttherapy.entity.TreatmentEvent;
import com.lontri.lighttherapy.entity.TreatmentSession;
import com.lontri.lighttherapy.entity.TreatmentSessionDevice;
import com.lontri.lighttherapy.enums.DeviceType;
import com.lontri.lighttherapy.enums.EventType;
import com.lontri.lighttherapy.enums.TreatmentSessionStatus;
import com.lontri.lighttherapy.infra.command.CommandGatewayRouter;
import com.lontri.lighttherapy.repository.SchemeStageDeviceConfigRepository;
import com.lontri.lighttherapy.repository.SchemeStageRepository;
import com.lontri.lighttherapy.repository.TreatmentEventRepository;
import com.lontri.lighttherapy.repository.TreatmentSessionDeviceRepository;
import com.lontri.lighttherapy.repository.TreatmentSessionRepository;
import com.lontri.lighttherapy.repository.DeviceRepository;
import com.lontri.lighttherapy.service.CommandBuilderService;
import com.lontri.lighttherapy.service.TreatmentEventService;
import com.lontri.lighttherapy.service.CommandBuilderService.BuiltCommands;
@Service
public class DefaultTreatmentExecutor implements TreatmentExecutor {
	private static final int SEND_RETRY = 3;
	private static final long RETRY_BACKOFF_MS = 120;
	/* ======================= 依赖 ======================= */

    private final TreatmentSessionRepository sessionRepo;
    private final TreatmentSessionDeviceRepository tsdRepo;
    private final SchemeStageRepository stageRepo;
    private final SchemeStageDeviceConfigRepository stageDeviceConfigRepo;
    private final TreatmentEventRepository eventRepo;
    private final CommandBuilderService commandBuilder;
    private final TreatmentEventService eventService;
    private final CommandGatewayRouter commandGatewayRouter;
    
    private final DeviceRepository deviceRepository;
    private final com.lontri.lighttherapy.executor.port.TreatmentDefaults treatmentDefaults;
    /* ======================= 执行器 ======================= */

    private final ExecutorService pool =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r);
                t.setName("treatment-exec-" + t.getId());
                t.setDaemon(true);
                return t;
            });

    private final Map<Long, ExecHandle> running = new ConcurrentHashMap<>();

    /* ======================= 构造器 ======================= */

    public DefaultTreatmentExecutor(
            TreatmentSessionRepository sessionRepo,
            TreatmentSessionDeviceRepository tsdRepo,
            SchemeStageRepository stageRepo,
            SchemeStageDeviceConfigRepository stageDeviceConfigRepo,
            TreatmentEventRepository eventRepo,
            TreatmentEventService eventService,
            CommandBuilderService commandBuilder,
            CommandGatewayRouter commandGatewayRouter,
            com.lontri.lighttherapy.executor.port.TreatmentDefaults treatmentDefaults,
            DeviceRepository deviceRepository
    ) {
        this.sessionRepo = sessionRepo;
        this.tsdRepo = tsdRepo;
        this.stageRepo = stageRepo;
        this.stageDeviceConfigRepo = stageDeviceConfigRepo;
        this.eventRepo = eventRepo;
        this.eventService = eventService;
        this.commandBuilder = commandBuilder;
        this.commandGatewayRouter = commandGatewayRouter;
        this.treatmentDefaults = treatmentDefaults;
        this.deviceRepository = deviceRepository;
    }

    /* ======================= 内部句柄 ======================= */

    private static class ExecHandle {
        Future<?> future;
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicBoolean paused = new AtomicBoolean(false);
    }

    /* ======================= 接口实现 ======================= */

    @Override
    public void startScheme(long sessionId) {
        running.computeIfAbsent(sessionId, id -> {
            ExecHandle h = new ExecHandle();
            h.future = pool.submit(() -> runScheme(id, h));
            eventRepo.save(event(id, "EXECUTOR_STARTED", "executor started"));
            return h;
        });
    }

    @Override
    public void startManual(long sessionId, String deviceSn, int lux, int cctK) {
        // 调用多设备版本，仅包含一个设备
        // List<DeviceManualConfig> deviceConfigs = Collections.singletonList(
        //     new DeviceManualConfig(deviceSn, lux, null, cctK)
        // );
        // startManual(sessionId, deviceConfigs);
    }
    
    @Override
    public void startManual(long sessionId, List<DeviceManualConfig> deviceConfigs) {
        running.computeIfAbsent(sessionId, id -> {
            ExecHandle h = new ExecHandle();
            h.future = pool.submit(() -> runManual(id, h, deviceConfigs));
            return h;
        });
    }

    @Override
    public void pauseSession(Long sessionId) {
        if (sessionId == null) return;
        ExecHandle h = running.get(sessionId);
        if (h != null) {
            h.paused.set(true);
            eventRepo.save(event(sessionId, "PAUSE", "session paused"));
        }
    }
    @Override
    public void resumeSession(Long sessionId) {
        if (sessionId == null) return;
        ExecHandle h = running.get(sessionId);
        if (h != null) {
            h.paused.set(false);
            eventRepo.save(event(sessionId, "RESUME", "session resumed"));
        }
    }

    @Override
    public void stop(long sessionId, String reason) {
        ExecHandle h = running.get(sessionId);
        if (h != null) {
            h.stop.set(true);
            h.future.cancel(true);
            running.remove(sessionId);
        }
        eventRepo.save(event(sessionId, "STOP", "session stopped",
                "{\"reason\":\"" + escapeJson(reason) + "\"}"));
        // 不再在这里调用restoreDefault，因为runManual的finally块会调用
        // restoreDefault(sessionId, reason);
    }

    @Override
    public boolean isRunning(long sessionId) {
        ExecHandle h = running.get(sessionId);
        return h != null && !h.stop.get();
    }
    
    /* ======================= runScheme ======================= */

    private void runScheme(long sessionId, ExecHandle h) {
        try {
            TreatmentSession session = sessionRepo.findById(sessionId)
                    .orElseThrow(() -> new IllegalStateException("session not found: " + sessionId));

            List<String> deviceSns = tsdRepo.findBySessionId(sessionId).stream()
                    .map(TreatmentSessionDevice::getDeviceSn)
                    .filter(sn -> sn != null && !sn.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            if (deviceSns.isEmpty()) {
                eventRepo.save(event(sessionId, "ERROR", "no devices bound", json("reason", "no devices bound")));
                markFailed(sessionId, "no devices bound");
                restoreDefault(sessionId, "no devices bound");
                return;
            }

            List<SchemeStage> stages = 
                    stageRepo.findBySchemeIdOrderByStageNoAsc(session.getSchemeId());

            if (stages == null || stages.isEmpty()) {
                eventRepo.save(event(sessionId, "ERROR", "no stages", json("reason", "scheme has no stages")));
                markFailed(sessionId, "no stages");
                restoreDefault(sessionId, "no stages");
                return;
            }

            // 根据 session 中的 stageId 确定从哪个阶段开始
            int idx = 0;
            if (session.getStageId() != null) {
                for (int i = 0; i < stages.size(); i++) {
                    if (stages.get(i).getId().equals(session.getStageId())) {
                        idx = i;
                        break;
                    }
                }
            }

            long stageStartAt = System.currentTimeMillis();
            SchemeStage cur = stages.get(idx);
            long remainingMs = toMs(cur.getDurationMinutes());
            long pauseStartedAt = -1L;

            // 立即下发第一条stage指令
            sendStageToDevices(sessionId, deviceSns, cur);

            while (!h.stop.get()) {

                if (h.paused.get()) {
                    if (pauseStartedAt < 0) {
                        pauseStartedAt = System.currentTimeMillis();
                    }
                    sleepQuietly(200);
                    continue;
                }

                if (pauseStartedAt >= 0) {
                    long pausedMs = System.currentTimeMillis() - pauseStartedAt;
                    stageStartAt += pausedMs;
                    pauseStartedAt = -1L;
                }

                // 进入 stage 时下发一次
                if (System.currentTimeMillis() - stageStartAt < 50) {
                    sendStageToDevices(sessionId, deviceSns, cur);
                }

                long elapsed = System.currentTimeMillis() - stageStartAt;
                long left = remainingMs - elapsed;

                if (left <= 0) {
                    idx++;
                    if (idx >= stages.size()) {
                        // 所有阶段执行完成，停止执行器但保持会话状态为RUNNING
                        // 等待前端调用end接口来结束会话并创建survey-result
                        eventRepo.save(event(sessionId, "INFO", "scheme completed, executor stopped", json("reason", "all stages completed")));
                        // 停止执行器
                        stop(sessionId, "scheme completed");
                        return;
                    }
                    cur = stages.get(idx);
                    stageStartAt = System.currentTimeMillis();
                    remainingMs = toMs(cur.getDurationMinutes());
                    // 立即下发下一个stage指令
                    sendStageToDevices(sessionId, deviceSns, cur);
                    continue;
                }

                sleepInterruptibly(Math.min(left, 500));
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            eventRepo.save(event(sessionId, "ERROR", "executor interrupted", json("reason", "thread interrupted")));
            restoreDefault(sessionId, "executor interrupted");
        } catch (BizException be) {
            // 业务异常：记录详细信息，但不标记为失败，因为这可能是配置问题，而不是执行器问题
            eventRepo.save(event(sessionId, "ERROR", "biz exception", json(
                "reason", be.getMessage(),
                "status", String.valueOf(be.getHttpStatus())
            )));
            // 只恢复默认，不标记为失败，让会话继续运行
            restoreDefault(sessionId, "biz exception: " + be.getMessage());
        } catch (Exception ex) {
            // 其他异常：记录详细信息并标记为失败
            eventRepo.save(event(sessionId, "ERROR", "executor error", json(
                "reason", ex.getMessage(),
                "stackTrace", shrink(ex.toString(), 512)
            )));
            markFailed(sessionId, ex.getMessage());
            restoreDefault(sessionId, "executor error: " + shrink(ex.toString(), 128));
        } finally {
            running.remove(sessionId);
        }
    }

    private boolean safeSend(long sessionId, String deviceSn, List<String> cmds, String context) {

        // 1) 基础校验
        if (deviceSn == null || deviceSn.trim().isEmpty()) {
            // 研究者执行场景（sessionId=0）时跳过事件记录
            if (sessionId != 0) {
                eventRepo.save(event(sessionId, "ERROR", "empty deviceSn(" + context + ")", json(
                        "context", context,
                        "reason", "deviceSn empty"
                )));
            }
            return false;
        }

        if (cmds == null || cmds.isEmpty()) {
            // 研究者执行场景（sessionId=0）时跳过事件记录
            if (sessionId != 0) {
                eventRepo.save(event(sessionId, "ERROR", "empty cmds(" + context + ") deviceSn=" + deviceSn, json(
                        "context", context,
                        "deviceSn", deviceSn,
                        "reason", "cmds empty"
                )));
            }
            return false;
        }

        // 2) 过滤掉 null/空命令
        List<String> sendList = cmds.stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());

        if (sendList.isEmpty()) {
            // 研究者执行场景（sessionId=0）时跳过事件记录
            if (sessionId != 0) {
                eventRepo.save(event(sessionId, "ERROR", "all cmds blank(" + context + ") deviceSn=" + deviceSn, json(
                        "context", context,
                        "deviceSn", deviceSn,
                        "reason", "all cmds blank"
                )));
            }
            return false;
        }

        boolean allOk = true;

        // 3) 逐条发送 + 重试
        for (String cmd : sendList) {

            boolean ok = false;
            Exception lastEx = null;

            for (int attempt = 1; attempt <= SEND_RETRY; attempt++) {
                try {
                    // 真正的发送动作：你只需要实现 doSend()
                    doSend(deviceSn, cmd);

                    ok = true;
                    break;

                } catch (InterruptedException ie) {
                    // stop() 会 cancel(true) -> 中断：快速退出
                    Thread.currentThread().interrupt();
                    lastEx = ie;
                    break;

                } catch (Exception ex) {
                    lastEx = ex;
                    // 小退避，避免打爆链路
                    sleepQuietly(RETRY_BACKOFF_MS * attempt);
                }
            }

            if (ok) {
                // 成功事件（可选，但很利于排查）
                // 研究者执行场景（sessionId=0）时跳过事件记录
                if (sessionId != 0) {
                    eventRepo.save(event(sessionId, "CMD_SENT",
                            "sent cmd(" + context + ") deviceSn=" + deviceSn,
                            json(
                                    "context", context,
                                    "deviceSn", deviceSn,
                                    "cmd", shrink(cmd, 256),
                                    "retry", ok ? "0" : String.valueOf(SEND_RETRY)
                            )));
                }
            } else {
                allOk = false;

                // 研究者执行场景（sessionId=0）时跳过事件记录
                if (sessionId != 0) {
                    eventRepo.save(event(sessionId, "CMD_SEND_FAILED",
                            "send failed(" + context + ") deviceSn=" + deviceSn,
                            json(
                                    "context", context,
                                    "deviceSn", deviceSn,
                                    "cmd", shrink(cmd, 256),
                                    "retry", String.valueOf(SEND_RETRY),
                                    "error", lastEx == null ? "unknown" : shrink(lastEx.toString(), 512)
                            )));
                }

                // 你可以选择：遇到某条失败就不继续发后续命令（更保守）
                // break;
            }
        }

        return allOk;
    }
    @Override
    public boolean manualControlOnce(long sessionId, String deviceSn,
                                     Integer dim, Integer sumDim, Integer skyDim, Integer cctK,Integer skyCctK,
                                     String source, String note) {

        // 防御：deviceSn不能为空
        if (deviceSn == null || deviceSn.trim().isEmpty()) {
            // 研究者执行场景（sessionId=0）时跳过事件记录
            if (sessionId != 0) {
                eventRepo.save(event(sessionId, "ERROR", "manual control invalid deviceSn",
                    json("reason", "deviceSn cannot be null or empty")));
            }
            return false;
        }

        // 防御：至少提供一个参数
        long nonNullCount = 0;
        if (dim != null) nonNullCount++;
        if (skyDim != null) nonNullCount++;
        if (sumDim != null) nonNullCount++;
        if (cctK != null) nonNullCount++;
        if (skyCctK != null) nonNullCount++;
        if (nonNullCount == 0) {
            // 研究者执行场景（sessionId=0）时跳过事件记录
            if (sessionId != 0) {
                eventRepo.save(event(sessionId, "ERROR", "manual control invalid payload",
                    json("deviceSn", deviceSn, "dim", String.valueOf(dim), "skyDim", String.valueOf(skyDim), "cctK", String.valueOf(cctK),
                         "reason", "at least one of dim, skyDim or cctK required")));
            }
            return false;
        }

        // 打印设备信息
        deviceRepository.findByDeviceSn(deviceSn).ifPresent(d -> {
            // 研究者执行场景（sessionId=0）时跳过事件记录
            if (sessionId != 0) {
                eventRepo.save(event(sessionId, "INFO", "device info", json(
                        "deviceSn", d.getDeviceSn(),
                        "deviceType", d.getDeviceType(),
                        "sendMode", d.getSendMode(),
                        "gatewayId", d.getGatewayId(),
                        "udpIp", d.getUdpIp(),
                        "udpPort", d.getUdpPort(),
                        "tcpServerIp", d.getTcpServerIp(),
                        "tcpServerPort", d.getTcpServerPort()
                )));
            }
        });

        BuiltCommands cmds = commandBuilder.buildDimCctCommands(deviceSn, dim, sumDim, skyDim, cctK, skyCctK);

        // 收集需要发送的指令
        java.util.List<String> commands = new java.util.ArrayList<>();
        java.util.List<String> kinds = new java.util.ArrayList<>();
        
        if (cmds.getDimCommand() != null && !cmds.getDimCommand().trim().isEmpty()) {
            commands.add(cmds.getDimCommand());
            kinds.add("DIM");
        }
        
        if (cmds.getCctCommand() != null && !cmds.getCctCommand().trim().isEmpty() && 
            (commands.isEmpty() || !cmds.getCctCommand().equals(cmds.getDimCommand()))) {
            commands.add(cmds.getCctCommand());
            kinds.add("CCT");
        }
        if (cmds.getDimcctCommand() != null && !cmds.getDimcctCommand().trim().isEmpty() && 
            (commands.isEmpty() || !cmds.getDimcctCommand().equals(cmds.getCctCommand()))) {
            commands.add(cmds.getDimcctCommand());
            kinds.add("DIM_CCT");
        }

        if (commands.isEmpty()) {
            // 研究者执行场景（sessionId=0）时跳过事件记录
            if (sessionId != 0) {
                eventRepo.save(event(sessionId, "CMD_BUILD_EMPTY", "manual control cmd empty",
                    json("deviceSn", deviceSn, "dim", String.valueOf(dim), "skyDim", String.valueOf(skyDim), "cctK", String.valueOf(cctK))));
            }
            return false;
        }

        // 发送所有指令
        boolean allOk = true;
        for (int i = 0; i < commands.size(); i++) {
            String cmd = commands.get(i);
            String kind = kinds.get(i);
            boolean ok = safeSend(sessionId, deviceSn, java.util.Collections.singletonList(cmd), "MANUAL_CONTROL_" + kind);
            if (!ok) {
                allOk = false;
            }
        }

        // 构建kind字符串，用于事件记录
        String kindStr = String.join(", ", kinds);

        // 研究者执行场景（sessionId=0）时跳过事件记录
        if (sessionId != 0) {
            eventRepo.save(event(
                sessionId,
                "MANUAL_CONTROL",
                "manual control " + kindStr + " deviceSn=" + deviceSn + " ok=" + allOk,
                json("deviceSn", deviceSn,
                     "kind", kindStr,
                     "dim", dim == null ? null : String.valueOf(dim),
                     "skyDim", skyDim == null ? null : String.valueOf(skyDim),
                     "cctK", cctK == null ? null : String.valueOf(cctK),
                     "ok", String.valueOf(allOk),
                     "source", source,
                     "note", note)
            ));
        }

        return allOk;
    }

    private void runManual(long sessionId, ExecHandle h, List<DeviceManualConfig> deviceConfigs) {
        try {
            // 1) 手动模式：进入时先下发一次“手动参数”
            for (DeviceManualConfig config : deviceConfigs) {
                sendManual(sessionId, config.getDeviceSn(), config.getDimP(), config.getSumDimP(), config.getSkyDimP(), config.getCctK(), config.getSkycctK());
            }

            // 2) 进入循环：RUNNING 时可选择“维持/心跳重发”
            //    - 如果你的设备不需要重发维持，可以把 sendManual 放到循环里去掉，仅 sleep 即可
            long lastSendAt = System.currentTimeMillis();

            while (!h.stop.get()) {

                // 2.1 pause：冻结（不下发、不维持），只等待 resume/stop
                if (h.paused.get()) {
                    sleepQuietly(200);
                    continue;
                }

                // 2.2 非 pause：按需要维持（每 30 秒重发一次）
                // long now = System.currentTimeMillis();
                // if (now - lastSendAt >= 60000) {
                //     for (DeviceManualConfig config : deviceConfigs) {
                //         sendManual(sessionId, config.getDeviceSn(), config.getDimP(), config.getSumDimP(), config.getSkyDimP(), config.getCctK(), config.getSkycctK());
                //     }
                //     lastSendAt = now;
                // }

                // 2.3 小睡，保证 stop/pause 响应快
                sleepInterruptibly(200);
            }

        } catch (InterruptedException ie) {
            // stop() 的 future.cancel(true) 会进来：正常退出即可
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            // 你也可以选择 markFailed(sessionId, ex.getMessage())
            // 手动模式出错一般也应该恢复默认
        } finally {
            // 3) 清理 running
            running.remove(sessionId);

            // 4) stop/异常退出：恢复默认（只在 finally 做一次，避免重复）
            restoreDefault(sessionId, "manual stopped");
        }
    }


//    private boolean shouldStop(long sessionId, ExecHandle h) {
//        return h.stop.get() || Thread.currentThread().isInterrupted() || !sessionStore.isRunning(sessionId);
//    }

    // private void holdWithChecks(long sessionId, ExecHandle h, int seconds) {
//        for (int i = 0; i < seconds; i++) {
//            if (shouldStop(sessionId, h)) return;
//            try {
//                sleeper.sleepSeconds(1);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                return;
//            }
//        }
    // }

    private void restoreDefault(long sessionId, String reason) {
        // 研究者执行场景（sessionId=0）时跳过恢复默认值
        if (sessionId == 0) {
            return;
        }
        
        List<String> sns = getDeviceSnsFromSnapshot(sessionId);
        Integer defaultDim = treatmentDefaults.defaultDim();        // 使用配置文件中的默认亮度
        Integer defaultSumdim = treatmentDefaults.defaultSumdim();        // 使用配置文件中的默认亮度
        Integer defaultSkydim = treatmentDefaults.defaultSkydim();        // 使用配置文件中的默认亮度
        Integer defaultCct = treatmentDefaults.defaultCctK();        // 使用配置文件中的默认色温
        Integer defaultSkycct = treatmentDefaults.defaultSkycctK();        // 使用配置文件中的默认色温

        for (String sn : sns) {
            BuiltCommands cmds = commandBuilder.buildDimCctCommands(sn, defaultDim, defaultSumdim,defaultSkydim, defaultCct,defaultSkycct);

            List<String> list = new java.util.ArrayList<>(3);
            // 避免重复发送相同的指令（特别是485设备）
            if (cmds.getCctCommand() != null) list.add(cmds.getCctCommand());
            if (cmds.getDimCommand() != null && (list.isEmpty() || !cmds.getDimCommand().equals(cmds.getCctCommand()))) {
                list.add(cmds.getDimCommand());
            }
            if (cmds.getDimcctCommand() != null && (list.isEmpty() || !cmds.getDimcctCommand().equals(cmds.getCctCommand()))) {
                list.add(cmds.getDimcctCommand());
            }

            if (!list.isEmpty()) {
                safeSend(sessionId, sn, list, "RESTORE");
            }
        }
    }

    // private void safeDelay(long millis) {
    //     try {
    //         Thread.sleep(millis);
    //     } catch (InterruptedException ie) {
    //         Thread.currentThread().interrupt();
    //         throw new RuntimeException("command execution interrupted", ie);
    //     }
    // }

    /* ======================= 工具方法 ======================= */

    private long toMs(Integer minutes) {
        int m = (minutes == null || minutes <= 0) ? 1 : minutes;
        return m * 60_000L;
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleepInterruptibly(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }
    /**
     * 真正的发送动作（你在这里对接你项目原来的 TCP/UDP/MQTT 下发）
     *
     * 要求：
     * - 发送失败抛异常
     * - 如果线程被中断，抛 InterruptedException 或检查 Thread.currentThread().isInterrupted()
     */
    private void doSend(String deviceSn, String cmd) throws Exception {
        // 使用CommandGatewayRouter发送指令
        commandGatewayRouter.send(deviceSn, cmd);
        Thread.sleep(500); // 等待指令生效
    }
    private String json(Object... kv) {
        // kv: k1,v1,k2,v2...
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = String.valueOf(kv[i]);
            String v = kv[i + 1] == null ? null : String.valueOf(kv[i + 1]);
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(k)).append("\":");
            if (v == null) sb.append("null");
            else sb.append("\"").append(escapeJson(v)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String shrink(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    /* ======================= 下发/恢复 ======================= */

    private void sendStageToDevices(long sessionId, List<String> sns, SchemeStage stage) {
        if (sns == null || sns.isEmpty()) return;

        // 获取该stage的所有设备独立配置
        Map<String, SchemeStageDeviceConfig> deviceConfigs = new ConcurrentHashMap<>();
        List<SchemeStageDeviceConfig> configs = stageDeviceConfigRepo.findByStageId(stage.getId());
        if (configs != null) {
            for (SchemeStageDeviceConfig config : configs) {
                deviceConfigs.put(config.getDeviceSn(), config);
            }
        }

        for (String sn : sns) {
            // 获取设备信息以判断类型
            Device device = deviceRepository.findByDeviceSn(sn).orElse(null);
            boolean isMcbOr485 = device != null && (DeviceType.MCB.equals(device.getDeviceType()) || DeviceType._485.equals(device.getDeviceType()));
            
            // 优先使用设备的独立配置，没有则使用stage的默认配置
            Integer lux = stage.getLightIntensity();
            Integer cct = stage.getLightColorTemp();
            Integer skyLux = null;
            Integer sumLux = null;
            Integer skycct = null;
            
            SchemeStageDeviceConfig deviceConfig = deviceConfigs.get(sn);
            if (deviceConfig != null) {
                if (isMcbOr485) {
                    // MCB或485设备
                    if (deviceConfig.getLightIntensity() != null) {
                        sumLux = deviceConfig.getLightIntensity();
                    }
                    if (deviceConfig.getSkyLightIntensity() != null) {
                        skyLux = deviceConfig.getSkyLightIntensity();
                    }
                    if (deviceConfig.getLightColorTemp() != null) {
                        skycct = cct = deviceConfig.getLightColorTemp();
                    }
                } else {
                    // 其他设备
                    if (deviceConfig.getLightIntensity() != null) {
                        lux = deviceConfig.getLightIntensity();
                    }
                    if (deviceConfig.getLightColorTemp() != null) {
                        cct = deviceConfig.getLightColorTemp();
                    }
                }
            } else {
                // 无设备配置时使用阶段默认值
                if (isMcbOr485) {
                    sumLux = stage.getLightIntensity();
                    cct = stage.getLightColorTemp();
                    skycct = cct;
                }
            }

            // 没有任何控制参数就不下发
            boolean hasParams = false;
            if (isMcbOr485) {
                hasParams = sumLux != null || skyLux != null || cct != null;
            } else {
                hasParams = lux != null || cct != null;
            }
            if (!hasParams) continue;

            // 根据设备类型构建指令
            BuiltCommands cmds;
            if (isMcbOr485) {
                cmds = commandBuilder.buildDimCctCommands(sn, null, sumLux, skyLux, null, skycct);
            } else {
                cmds = commandBuilder.buildDimCctCommands(sn, lux, null, null, cct, null);
            }

            List<String> list = new ArrayList<>(3);
            // 避免重复发送相同的指令（特别是485设备）
            if (cmds.getCctCommand() != null) list.add(cmds.getCctCommand());
            if (cmds.getDimCommand() != null && (list.isEmpty() || !cmds.getDimCommand().equals(cmds.getCctCommand()))) {
                list.add(cmds.getDimCommand());
            }
            if (cmds.getDimcctCommand() != null) list.add(cmds.getDimcctCommand());
            if (!list.isEmpty()) {
                safeSend(sessionId, sn, list, "STAGE"); // tag 改成你想要的
                
                // 记录设备进入stage的事件，包含设备的实际参数
                eventRepo.save(event(
                        sessionId,
                        "STAGE_ENTER",
                        "enter stage " + stage.getStageNo() + " for device " + sn,
                        "{\"stageNo\":" + stage.getStageNo()
                                + ",\"deviceSn\":\"" + sn + "\""
                                + ",\"lux\":" + (isMcbOr485 ? (sumLux == null ? "null" : sumLux) : (lux == null ? "null" : lux))
                                + ",\"skyLux\":" + (skyLux == null ? "null" : skyLux)
                                + ",\"cct\":" + (cct == null ? "null" : cct)
                                + ",\"durationMinutes\":" + stage.getDurationMinutes()
                                + "}"
                ));
            }
        }
    }

    private void sendManual(Long sessionId, String sn, Integer dimP, Integer sumDimP, Integer skyDimP, Integer cctK, Integer skycctK) {
        BuiltCommands cmds = commandBuilder.buildDimCctCommands(sn, dimP, sumDimP, skyDimP, cctK, skycctK);

        List<String> list = new ArrayList<>(3);
        // 避免重复发送相同的指令（特别是485设备）
        // 优先发送DIM_CCT指令，然后再发送其他指令
        
        if (cmds.getCctCommand() != null) list.add(cmds.getCctCommand());
        if (cmds.getDimCommand() != null && (list.isEmpty() || !cmds.getDimCommand().equals(cmds.getCctCommand()))) {
            list.add(cmds.getDimCommand());
        }
        if (cmds.getDimcctCommand() != null) list.add(cmds.getDimcctCommand());
        if (!list.isEmpty()) {
            safeSend(sessionId, sn, list, "MANUAL"); // 如果 safeSend 必须要 sessionId，就把 sessionId 传进来（见 B）
        	eventRepo.save(event(
        	        sessionId,
        	        "STAGE_ENTER",
        	        "enter stage ",
        	        "{\"stageNo\": 1"
        	                + ",\"dimP\":" + (dimP != null ? dimP : "null")
        	                + ",\"sumDimP\":" + (sumDimP != null ? sumDimP : "null")
        	                + ",\"skyDimP\":" + (skyDimP != null ? skyDimP : "null")
        	                + ",\"cct\":" + (cctK != null ? cctK : "null")
                            + ",\"skycct\":" + (skycctK != null ? skycctK : "null")
        	                + "}"
        	));
        }
    }

    

    /* ======================= 状态落库 ======================= */
    private void finishSession(long sessionId) {
        TreatmentSession s = sessionRepo.findById(sessionId).orElse(null);
        if (s == null) return;

        s.setStatus(TreatmentSessionStatus.FINISHED);
        s.setEndTime(LocalDateTime.now());
        sessionRepo.save(s);

        eventService.record(
            sessionId,
            EventType.SESSION_DONE,
            null
        );
    }
    private void markFailed(long sessionId, String reason) {
        TreatmentSession s = sessionRepo.findById(sessionId).orElse(null);
        if (s == null) return;

        s.setStatus(TreatmentSessionStatus.CANCELLED);
        s.setEndTime(LocalDateTime.now());
        sessionRepo.save(s);

        eventService.record(
            sessionId,
            EventType.SESSION_FAILED,
            reason
        );
    }

//    private void finishSession(long sessionId) {
//        TreatmentSession s = sessionRepo.findById(sessionId).orElse(null);
//        if (s == null) return;
//        s.setStatus(TreatmentSessionStatus.FINISHED);
//        s.setUpdatedAt(LocalDateTime.now());
//        sessionRepo.save(s);
////        eventRepo.save(event(sessionId, "DONE", "scheme completed"));
//        eventRepo.save(event(sessionId, "DONE", "scheme completed", "{\"result\":\"ok\"}"));
//    }

//    private void markFailed(long sessionId, String reason) {
//        TreatmentSession s = sessionRepo.findById(sessionId).orElse(null);
//        if (s == null) return;
//        s.setStatus(TreatmentSessionStatus.CANCELLED);
//        s.setUpdatedAt(LocalDateTime.now());
//        sessionRepo.save(s);
////        eventRepo.save(event(sessionId, "FAILED", String.valueOf(reason)));
//        // 建议把 reason 放 detailJson，message 留短一点
//        eventRepo.save(event(sessionId, "FAILED", "scheme failed", "{\"reason\":\"" + escapeJson(reason) + "\"}"));
//    }
    private TreatmentEvent event(long sessionId, String type, String msg) {
        return event(sessionId, type, msg, null);
    }
    private TreatmentEvent event(long sessionId, String type, String msg,String detailJson) {
        TreatmentEvent e = new TreatmentEvent();
        e.setSessionId(sessionId);
        e.setEventType(type);
        e.setMessage(msg);
        e.setEventTime(LocalDateTime.now());
        e.setDetailJson(detailJson);
        return e;
    }

    private List<String> getDeviceSnsFromSnapshot(long sessionId) {
        return tsdRepo.findBySessionId(sessionId).stream()
                .map(TreatmentSessionDevice::getDeviceSn)
                .filter(sn -> sn != null && !sn.trim().isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

}
