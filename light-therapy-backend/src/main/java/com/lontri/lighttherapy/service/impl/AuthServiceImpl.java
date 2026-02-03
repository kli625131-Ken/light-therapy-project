package com.lontri.lighttherapy.service.impl;

import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.config.JwtTokenProvider;
import com.lontri.lighttherapy.dto.AuthDtos;
import com.lontri.lighttherapy.dto.AuthDtos.LoginResp;
import com.lontri.lighttherapy.entity.ExperimentGroup;
import com.lontri.lighttherapy.entity.Subject;
import com.lontri.lighttherapy.entity.User;
import com.lontri.lighttherapy.repository.SubjectRepository;
import com.lontri.lighttherapy.repository.UserRepository;
import com.lontri.lighttherapy.repository.ExperimentGroupRepository;
import com.lontri.lighttherapy.service.AuthService;
import com.lontri.lighttherapy.service.SubjectSnapshotService;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final JwtTokenProvider tokenProvider;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SubjectRepository subjectRepo;
    private final ExperimentGroupRepository groupRepo;
    private final SubjectSnapshotService subjectSnapshotService;

    public AuthServiceImpl(UserRepository userRepo, JwtTokenProvider tokenProvider,
            SubjectRepository subjectRepo, ExperimentGroupRepository groupRepo,
            SubjectSnapshotService subjectSnapshotService) {
        this.userRepo = userRepo;
        this.tokenProvider = tokenProvider;
        this.subjectRepo = subjectRepo;
        this.groupRepo = groupRepo;
        this.subjectSnapshotService = subjectSnapshotService;
    }

    @Override
    public AuthDtos.LoginResp login(AuthDtos.LoginReq req) {
        User u = userRepo.findByUsername(req.username)
                .orElseThrow(() -> new BizException(40100, "invalid username or password", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) {
            throw new BizException(40300, "user disabled", HttpStatus.FORBIDDEN);
        }
        if (!encoder.matches(req.password, u.getPasswordHash())) {
            throw new BizException(40100, "invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        LoginResp resp = new LoginResp();
        resp.userId = u.getId();
        resp.role = u.getRole();
        resp.token = tokenProvider.createToken(u.getId(), u.getRole());
        // ✅ 仅 SUBJECT 才附带受试者信息与治疗快照
        if ("SUBJECT".equalsIgnoreCase(u.getRole())) {
            Subject subject = subjectRepo.findByUserId(u.getId())
                    .orElse(null);
            if (subject != null) {
                AuthDtos.SubjectDto sd = new AuthDtos.SubjectDto();
            sd.id = subject.getId();
            sd.code = subject.getSubjectCode();
            sd.name = subject.getDisplayName();
            sd.gender = subject.getGender();
            sd.age = subject.getAge();
            sd.contactInfo = subject.getContactInfo();
            sd.enrollmentDate = subject.getEnrollmentDate();
            sd.diagnosisResult = subject.getDiagnosisResult();
            sd.subjectName = subject.getSubjectName();
            // 获取分组名称
            if (subject.getGroupId() != null) {
                Optional<ExperimentGroup> groupOpt = groupRepo.findById(subject.getGroupId());
                sd.groupName = groupOpt.map(ExperimentGroup::getName).orElse("无分组");
                sd.groupid = groupOpt.map(ExperimentGroup::getId).map(Object::toString).orElse("无分组");
            }
                resp.subject = sd;
                try {
                    resp.snapshot = subjectSnapshotService.build(subject.getId());
                } catch (Exception e) {
                    // 快照构建失败不影响登录
                    resp.snapshot = new AuthDtos.SubjectSnapshot();
                }
            }
        }
        return resp;
    }
}
