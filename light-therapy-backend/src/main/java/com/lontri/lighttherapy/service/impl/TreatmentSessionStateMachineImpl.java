package com.lontri.lighttherapy.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.entity.SurveyResult;
import com.lontri.lighttherapy.entity.TreatmentSession;
import com.lontri.lighttherapy.enums.EventType;
import com.lontri.lighttherapy.enums.TreatmentSessionStatus;
import com.lontri.lighttherapy.executor.TreatmentExecutor;
import com.lontri.lighttherapy.repository.SurveyResultRepository;
import com.lontri.lighttherapy.repository.TreatmentSessionRepository;
import com.lontri.lighttherapy.service.SurveyTemplateService;
import com.lontri.lighttherapy.service.TreatmentEventService;
import com.lontri.lighttherapy.service.TreatmentSessionStateMachine;

@Service
public class TreatmentSessionStateMachineImpl implements TreatmentSessionStateMachine {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(TreatmentSessionStateMachineImpl.class);

    private final TreatmentSessionRepository sessionRepo;
    private final TreatmentEventService eventService;
    private final SurveyTemplateService surveyTemplateService;
    private final TreatmentExecutor treatmentExecutor;

    @Value("${lighttherapy.manual-scheme-id:1}")
    private Long manualSchemeId;

    private final com.lontri.lighttherapy.service.SurveyService surveyService;
    private final com.lontri.lighttherapy.repository.SubjectRepository subjectRepo;
    private final com.lontri.lighttherapy.repository.ExperimentGroupRepository experimentGroupRepo;

    public TreatmentSessionStateMachineImpl(TreatmentSessionRepository sessionRepo,
            TreatmentEventService eventService,
            SurveyTemplateService surveyTemplateService,
            TreatmentExecutor treatmentExecutor,
            com.lontri.lighttherapy.service.SurveyService surveyService,
            com.lontri.lighttherapy.repository.SubjectRepository subjectRepo,
            com.lontri.lighttherapy.repository.ExperimentGroupRepository experimentGroupRepo) {
        this.sessionRepo = sessionRepo;
        this.eventService = eventService;
        this.surveyTemplateService = surveyTemplateService;
        this.treatmentExecutor = treatmentExecutor;
        this.surveyService = surveyService;
        this.subjectRepo = subjectRepo;
        this.experimentGroupRepo = experimentGroupRepo;
    }

    @Override
    @Transactional
    public TreatmentSession apply(Long sessionId, Action action, String reason) {
        log.info("Applying action {} to session {}", action, sessionId);

        // Use pessimistic lock to prevent concurrent modifications (Double Submission)
        TreatmentSession s = sessionRepo.findByIdWithLock(sessionId)
                .orElseThrow(() -> new BizException(40410, "session not found", HttpStatus.NOT_FOUND));

        TreatmentSessionStatus from = s.getStatus();

        // Idempotency Check for END action
        if (action == Action.END && from == TreatmentSessionStatus.FINISHED) {
            log.warn("Session {} is already FINISHED. Ignoring duplicate END request. Returning success.", sessionId);
            return s;
        }

        Transition t = Transition.of(action, from);

        if (!t.allowed) {
            throw new BizException(40901,
                    "illegal transition: " + action + " from " + from,
                    HttpStatus.CONFLICT);
        }

        // START 特殊保护：同一个 subject 只能有一个 RUNNING（配合 uk_subject_running）
        if (action == Action.START) {
            // 允许“重复 start”视为非法（更严谨）
            if (from == TreatmentSessionStatus.RUNNING) {
                throw new BizException(40901, "illegal transition: START from RUNNING", HttpStatus.CONFLICT);
            }
            boolean otherRunning = sessionRepo.existsBySubjectIdAndStatusAndIdNot(
                    s.getSubjectId(),
                    TreatmentSessionStatus.RUNNING,
                    s.getId());
            if (otherRunning) {
                throw new BizException(40902,
                        "subject already has a RUNNING session",
                        HttpStatus.CONFLICT);
            }
        }

        // 时间戳处理
        LocalDateTime now = LocalDateTime.now();

        // 1) 状态变更
        if (t.to != null && t.to != from) {
            s.setStatus(t.to);
        }

        // 2) 按动作写关键时间字段（对齐表：start_time/end_time）
        switch (action) {
            case START:
                if (s.getStartTime() == null) {
                    s.setStartTime(now);
                }
                break;
            case END:
            case CANCEL:
            case FAIL:
                if (s.getEndTime() == null) {
                    s.setEndTime(now);
                }
                break;
            case PAUSE:
            case RESUME:
            default:
                break;
        }

        // 3) 保存
        s = sessionRepo.save(s);

        // 4) 当启动治疗时，启动执行器
        if (action == Action.START && s.getStatus() == TreatmentSessionStatus.RUNNING) {
            // 只有非手动模式才调用startScheme，手动模式在TreatmentServiceImpl.manualStart的afterCommit中处理
            if (!manualSchemeId.equals(s.getSchemeId())) {
                // 创建final变量保存会话ID，用于内部类访问
                final Long session_Id = s.getId();
                // 重要：必须在事务提交后执行，否则执行器无法读取到刚创建的会话和设备关联信息
                if (org.springframework.transaction.support.TransactionSynchronizationManager
                        .isSynchronizationActive()) {
                    org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                            new org.springframework.transaction.support.TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    treatmentExecutor.startScheme(session_Id);
                                }
                            });
                } else {
                    treatmentExecutor.startScheme(sessionId);
                }
            }
        }

        // 4) 当治疗结束时，创建survey result
        if (action == Action.END && s.getStatus() == TreatmentSessionStatus.FINISHED) {
            log.info("=== [Survey Creation] Session {} ended, starting survey result creation ===", s.getId());
            boolean processed = false;
            try {
                // Find subject to get group ID
                com.lontri.lighttherapy.entity.Subject subject = subjectRepo.findById(s.getSubjectId()).orElse(null);
                log.info("[Survey Creation] Subject found: {}, GroupId: {}",
                    subject != null ? subject.getId() : "null",
                    subject != null ? subject.getGroupId() : "null");

                if (subject != null && subject.getGroupId() != null) {
                    // IMPORTANT: Use findByIdWithSurveyTemplates to eagerly load the surveyTemplates collection
                    // Otherwise, the lazy-loaded collection will be empty/null when accessed in createMissingSurveyResults
                    com.lontri.lighttherapy.entity.ExperimentGroup group = experimentGroupRepo
                            .findByIdWithSurveyTemplates(subject.getGroupId()).orElse(null);

                    log.info("[Survey Creation] Group found: {}, SurveyTemplates count: {}",
                        group != null ? group.getId() : "null",
                        group != null && group.getSurveyTemplates() != null ? group.getSurveyTemplates().size() : 0);

                    if (group != null) {
                        // Isolated transaction: any failure here will NOT roll back the session END
                        surveyService.createMissingSurveyResults(group, s);
                        processed = true;
                        log.info("[Survey Creation] Survey results creation completed for session {}", s.getId());
                    } else {
                        log.warn("[Survey Creation] Group not found for groupId: {}", subject.getGroupId());
                    }
                } else {
                    log.warn("[Survey Creation] Subject or GroupId is null for session {}", s.getId());
                }
            } catch (Exception e) {
                log.error("Failed to create group survey results for session {}. Continued to ensure END state.",
                        s.getId(), e);
            }

            if (!processed) {
                try {
                    Long defaultTemplateId = surveyTemplateService.getDefaultTemplate().id;
                    // Isolated transaction
                    surveyService.createDefaultSurveyResult(s, defaultTemplateId);
                } catch (Exception e) {
                    log.error("Failed to create default survey result for session {}.", s.getId(), e);
                }
            }
        }

        // 4) 记事件（强制）
        // reason：cancel/fail 建议传；其他动作可 null
        eventService.record(s.getId(), t.eventType, normalizeReason(reason));

        return s;
    }

    private String normalizeReason(String reason) {
        if (reason == null)
            return null;
        String r = reason.trim();
        return r.isEmpty() ? null : r;
    }

    /**
     * 一次转换定义：是否允许、目标状态、事件类型
     */
    private static class Transition {
        final boolean allowed;
        final TreatmentSessionStatus to; // nullable：不变更也可
        final EventType eventType;

        private Transition(boolean allowed, TreatmentSessionStatus to, EventType eventType) {
            this.allowed = allowed;
            this.to = to;
            this.eventType = eventType;
        }

        static Transition of(Action action, TreatmentSessionStatus from) {
            boolean terminal = (from == TreatmentSessionStatus.FINISHED || from == TreatmentSessionStatus.CANCELLED);

            switch (action) {
                case START:
                    if (terminal)
                        return new Transition(false, null, EventType.SESSION_STARTED);
                    // PLANNED / PAUSED -> RUNNING
                    if (from == TreatmentSessionStatus.PLANNED || from == TreatmentSessionStatus.PAUSED) {
                        return new Transition(true, TreatmentSessionStatus.RUNNING, EventType.SESSION_STARTED);
                    }
                    // RUNNING 不允许重复 start（外层也会挡）
                    return new Transition(false, null, EventType.SESSION_STARTED);

                case PAUSE:
                    if (from != TreatmentSessionStatus.RUNNING)
                        return new Transition(false, null, EventType.SESSION_PAUSED);
                    return new Transition(true, TreatmentSessionStatus.PAUSED, EventType.SESSION_PAUSED);

                case RESUME:
                    if (from != TreatmentSessionStatus.PAUSED)
                        return new Transition(false, null, EventType.SESSION_RESUMED);
                    return new Transition(true, TreatmentSessionStatus.RUNNING, EventType.SESSION_RESUMED);

                case END:
                    if (terminal)
                        return new Transition(false, null, EventType.SESSION_DONE);
                    // RUNNING / PAUSED -> FINISHED
                    if (from == TreatmentSessionStatus.RUNNING || from == TreatmentSessionStatus.PAUSED) {
                        return new Transition(true, TreatmentSessionStatus.FINISHED, EventType.SESSION_DONE);
                    }
                    // PLANNED 不允许直接 END（按严格语义）
                    return new Transition(false, null, EventType.SESSION_DONE);

                case CANCEL:
                    if (terminal)
                        return new Transition(false, null, EventType.SESSION_CANCELLED);
                    // PLANNED / RUNNING / PAUSED -> CANCELLED
                    if (from == TreatmentSessionStatus.PLANNED
                            || from == TreatmentSessionStatus.RUNNING
                            || from == TreatmentSessionStatus.PAUSED) {
                        return new Transition(true, TreatmentSessionStatus.CANCELLED, EventType.SESSION_CANCELLED);
                    }
                    return new Transition(false, null, EventType.SESSION_CANCELLED);

                case FAIL:
                    if (terminal)
                        return new Transition(false, null, EventType.SESSION_FAILED);
                    // 没有 FAILED 状态：失败落 CANCELLED + 事件 SESSION_FAILED
                    if (from == TreatmentSessionStatus.PLANNED
                            || from == TreatmentSessionStatus.RUNNING
                            || from == TreatmentSessionStatus.PAUSED) {
                        return new Transition(true, TreatmentSessionStatus.CANCELLED, EventType.SESSION_FAILED);
                    }
                    return new Transition(false, null, EventType.SESSION_FAILED);

                default:
                    return new Transition(false, null, EventType.EXECUTOR_ERROR);
            }
        }
    }
}