package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.TreatmentDtos;
import com.lontri.lighttherapy.entity.TreatmentEvent;
import com.lontri.lighttherapy.entity.TreatmentSession;
import com.lontri.lighttherapy.entity.Subject;
import com.lontri.lighttherapy.entity.User;
import com.lontri.lighttherapy.repository.SubjectRepository;
import com.lontri.lighttherapy.repository.UserRepository;
import com.lontri.lighttherapy.service.TreatmentService;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/api/treatments", "/api/v1/treatments"})
public class TreatmentController {
    private final TreatmentService treatmentService;
    private final UserRepository userRepo;
    private final SubjectRepository subjectRepo;

    public TreatmentController(TreatmentService treatmentService, UserRepository userRepo, SubjectRepository subjectRepo) {
        this.treatmentService = treatmentService;
        this.userRepo = userRepo;
        this.subjectRepo = subjectRepo;
    }

    @PostMapping
    public ApiResponse<TreatmentSession> create(@RequestBody @Valid TreatmentDtos.CreateSessionReq req) {
        return ApiResponse.ok(treatmentService.create(req));
    }

    // ✅ 推荐：手动模式一键开始
    @PostMapping("/manual/start")
    public ApiResponse<TreatmentSession> manualStart(@RequestBody @Valid TreatmentDtos.ManualStartReq req) {
        return ApiResponse.ok(treatmentService.manualStart(req));
    }
    
    @PostMapping("/manual/control")
    public ApiResponse<Void> manualControl(@RequestBody @Valid TreatmentDtos.ManualControlReq req) {
        treatmentService.manualControl(req);
        return ApiResponse.ok(null);
    }
    
    // 研究者执行场景
    @PostMapping("/researcher/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER')")
    public ApiResponse<Void> researcherExecute(@RequestBody @Valid TreatmentDtos.ResearcherExecuteReq req) {
        treatmentService.researcherExecute(req);
        return ApiResponse.ok(null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUBJECT')")
    public ApiResponse<org.springframework.data.domain.Page<TreatmentSession>> list(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        // Auto-resolve Subject ID for SUBJECT role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.lontri.lighttherapy.config.UserPrincipal) {
            com.lontri.lighttherapy.config.UserPrincipal principal = (com.lontri.lighttherapy.config.UserPrincipal) auth.getPrincipal();
            Long userId = principal.getUserId();
            Optional<User> currentUser = userRepo.findById(userId);
            if (currentUser.isPresent() && "SUBJECT".equalsIgnoreCase(currentUser.get().getRole())) {
                Optional<Subject> subject = subjectRepo.findByUserId(currentUser.get().getId());
                if (subject.isPresent()) {
                    subjectId = subject.get().getId();
                    LoggerFactory.getLogger(TreatmentController.class)
                        .info("Subject view detected. UserID: {} -> ResolvedSubjectID: {}", currentUser.get().getId(), subjectId);
                } else {
                    LoggerFactory.getLogger(TreatmentController.class)
                        .warn("Subject profile NOT found for UserID: {}. Forcing subjectId=-1 to return empty list.", currentUser.get().getId());
                    subjectId = -1L;
                }
            }
        }
        return ApiResponse.ok(treatmentService.list(subjectId, status, startDate, endDate, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<TreatmentSession> get(@PathVariable Long id) {
        return ApiResponse.ok(treatmentService.get(id));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<TreatmentSession> start(@PathVariable Long id) {
        return ApiResponse.ok(treatmentService.start(id));
    }

    @PostMapping("/{id}/end")
    public ApiResponse<TreatmentSession> end(@PathVariable Long id) {
        return ApiResponse.ok(treatmentService.end(id));
    }

    @PostMapping("/{id}/fail")
    public ApiResponse<TreatmentSession> fail(@PathVariable Long id,
                                              @RequestParam(required = false, defaultValue = "failed") String reason) {
        return ApiResponse.ok(treatmentService.fail(id, reason));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<TreatmentSession> cancel(@PathVariable Long id,
                                                @RequestParam(required = false, defaultValue = "cancelled") String reason) {
        return ApiResponse.ok(treatmentService.cancel(id, reason));
    }

    @PostMapping("/{id}/events")
    public ApiResponse<TreatmentEvent> addEvent(@PathVariable Long id,
                                                @RequestBody @Valid TreatmentDtos.AddEventReq req) {
        return ApiResponse.ok(treatmentService.addEvent(id, req));
    }

    @GetMapping("/{id}/events")
    public ApiResponse<List<TreatmentEvent>> listEvents(@PathVariable Long id) {
        return ApiResponse.ok(treatmentService.listEvents(id));
    }
    @PostMapping("/start-now")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUBJECT')")
    public ApiResponse<TreatmentSession> createAndStart(@RequestBody TreatmentDtos.CreateSessionReq req) {
        // Auto-resolve Subject ID for SUBJECT role
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth != null && auth.getPrincipal() instanceof com.lontri.lighttherapy.config.UserPrincipal) {
        //     com.lontri.lighttherapy.config.UserPrincipal principal = (com.lontri.lighttherapy.config.UserPrincipal) auth.getPrincipal();
        //     Long userId = principal.getUserId();
        //     Optional<User> currentUser = userRepo.findById(userId);
        //     if (currentUser.isPresent() && "SUBJECT".equalsIgnoreCase(currentUser.get().getRole())) {
        //         Optional<Subject> subject = subjectRepo.findByUserId(currentUser.get().getId());
        //         if (subject.isPresent()) {
        //             req.subjectId = subject.get().getId();
        //         }
        //     }
        // }
        return ApiResponse.ok(treatmentService.createAndStart(req));
    }
    @PostMapping("/{id}/pause")
    public ApiResponse<TreatmentSession> pause(@PathVariable Long id) {
        return ApiResponse.ok(treatmentService.pause(id));
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<TreatmentSession> resume(@PathVariable Long id) {
        return ApiResponse.ok(treatmentService.resume(id));
    }

    @GetMapping("/subject/{subjectId}")
    public ApiResponse<org.springframework.data.domain.Page<TreatmentSession>> listBySubject(
            @PathVariable Long subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        return ApiResponse.ok(treatmentService.list(subjectId, status, startDate, endDate, pageable));
    }

}