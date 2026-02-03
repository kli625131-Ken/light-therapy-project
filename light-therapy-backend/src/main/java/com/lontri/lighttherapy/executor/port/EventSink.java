package com.lontri.lighttherapy.executor.port;

public interface EventSink {

    /**
     * @param sessionId 治疗会话ID
     * @param type 事件类型（ENGINE / ERROR / DEVICE_MSG / STAGE_START / STAGE_END / RESTORE）
     * @param message 人类可读信息
     * @param extraJson 扩展信息（可为空）
     */
    void record(long sessionId, String type, String message, String extraJson);
}
