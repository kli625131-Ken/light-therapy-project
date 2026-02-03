package com.lontri.lighttherapy.enums;

public enum TreatmentSessionStatus {
	PLANNED,   // 默认
    RUNNING,   // 运行中（对应你 DB 的 uk_subject_running）
    PAUSED,
    FINISHED,
    CANCELLED,
    DONE;     // 兼容前端可能传递的DONE状态
}
