package com.lontri.lighttherapy.enums;

public enum EventType {

    // ===== 生命周期 =====
    SESSION_CREATED("SESSION_CREATED"),
    SESSION_STARTED("SESSION_STARTED"),
    SESSION_PAUSED("SESSION_PAUSED"),
    SESSION_RESUMED("SESSION_RESUMED"),
    SESSION_DONE("SESSION_DONE"),
    SESSION_CANCELLED("SESSION_CANCELLED"),
    SESSION_FAILED("SESSION_FAILED"),

    // ===== 执行过程 =====
    STAGE_ENTER("STAGE_ENTER"),
    STAGE_EXIT("STAGE_EXIT"),

    // ===== 手动模式 =====
    MANUAL_STARTED("MANUAL_STARTED"),
    MANUAL_STOPPED("MANUAL_STOPPED"),

    // ===== 执行器 =====
    EXECUTOR_STARTED("EXECUTOR_STARTED"),
    EXECUTOR_STOPPED("EXECUTOR_STOPPED"),
    EXECUTOR_ERROR("EXECUTOR_ERROR");

    private final String code;

    EventType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
