package com.lontri.lighttherapy.service.impl;

import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.dto.UserDtos;
import com.lontri.lighttherapy.entity.ExperimentGroup;
import com.lontri.lighttherapy.entity.Subject;
import com.lontri.lighttherapy.dto.SubjectListResp;
import com.lontri.lighttherapy.entity.User;
import com.lontri.lighttherapy.repository.ExperimentGroupRepository;
import com.lontri.lighttherapy.repository.SubjectRepository;
import com.lontri.lighttherapy.repository.UserRepository;
import com.lontri.lighttherapy.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final SubjectRepository subjectRepo;
    private final ExperimentGroupRepository groupRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserRepository userRepo, SubjectRepository subjectRepo,
            ExperimentGroupRepository groupRepo) {
        this.userRepo = userRepo;
        this.subjectRepo = subjectRepo;
        this.groupRepo = groupRepo;
    }

    @Override
    public Page<UserDtos.UserDTO> list(Pageable pageable) {
        return userRepo.findAll(pageable).map(this::toDto);
    }

    @Override
    public UserDtos.UserDTO update(Long id, UserDtos.UpdateUserReq req) {
        User u = userRepo.findById(id)
                .orElseThrow(() -> new BizException(40400, "user not found", HttpStatus.NOT_FOUND));
        u.setRole(req.role);
        u.setStatus(req.status);
        return toDto(userRepo.save(u));
    }

    @Override
    public void disable(Long id) {
        User u = userRepo.findById(id)
                .orElseThrow(() -> new BizException(40400, "user not found", HttpStatus.NOT_FOUND));
        u.setStatus("DISABLED");
        userRepo.save(u);
    }

    @Override
    @Transactional
    public void createSubject(UserDtos.CreateSubjectReq req) {
        if (userRepo.findByUsername(req.username).isPresent()) {
            throw new BizException(40901, "username already exists", HttpStatus.CONFLICT);
        }
        User u = new User();
        u.setUsername(req.username);
        u.setPasswordHash(encoder.encode(req.password));
        u.setRole("SUBJECT");
        u.setStatus("ACTIVE");
        u = userRepo.save(u);

        Subject s = new Subject();
        s.setUserId(u.getId());
        s.setSubjectCode(req.subjectCode);
        s.setDisplayName(req.displayName);
        s.setGender(req.gender);
        s.setAge(req.age);
        s.setGroupId(req.groupId);
        s.setContactInfo(req.contactInfo);
        s.setDiagnosisResult(req.diagnosisResult);
        if (req.subjectName != null)
        s.setSubjectName(req.subjectName);
        if (req.enrollmentDate != null)
            s.setEnrollmentDate(req.enrollmentDate);
        subjectRepo.save(s);
    }

    @Override
    public Page<SubjectListResp> listSubjects(Pageable pageable) {
        return subjectRepo.findAll(pageable).map(subject -> {
            ExperimentGroup group = groupRepo.findById(subject.getGroupId()).orElse(null);
            String groupName = group != null ? group.getName() : null;
            return new SubjectListResp(subject.getId(), subject.getSubjectCode(), subject.getGender(), 
                                      subject.getAge(), subject.getGroupId(), groupName,
                                      subject.getDisplayName(), subject.getStatus(), subject.getSubjectName(),
                                      subject.getContactInfo(), subject.getEnrollmentDate(), subject.getDiagnosisResult());
        });
    }

    @Override
    public void updateSubject(Long id, UserDtos.UpdateSubjectReq req) {
        Subject s = subjectRepo.findById(id)
                .orElseThrow(() -> new BizException(40400, "subject not found", HttpStatus.NOT_FOUND));
        if (req.age != null)
            s.setAge(req.age);
        if (req.gender != null)
            s.setGender(req.gender);
        if (req.displayName != null)
            s.setDisplayName(req.displayName);
        if (req.groupId != null)
            s.setGroupId(req.groupId);
        if (req.subjectCode != null && !req.subjectCode.isEmpty())
            s.setSubjectCode(req.subjectCode);
        if (req.contactInfo != null)
            s.setContactInfo(req.contactInfo);
        if (req.diagnosisResult != null)
            s.setDiagnosisResult(req.diagnosisResult);
        if (req.enrollmentDate != null)
            s.setEnrollmentDate(req.enrollmentDate);
        if (req.subjectName != null)
            s.setSubjectName(req.subjectName);
        subjectRepo.save(s);
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        Subject s = subjectRepo.findById(id)
                .orElseThrow(() -> new BizException(40400, "subject not found", HttpStatus.NOT_FOUND));
        // Disable associated user
        userRepo.findById(s.getUserId()).ifPresent(u -> {
            u.setStatus("DISABLED");
            userRepo.save(u);
        });
        subjectRepo.delete(s);
    }

    private UserDtos.UserDTO toDto(User u) {
        UserDtos.UserDTO dto = new UserDtos.UserDTO();
        dto.id = u.getId();
        dto.username = u.getUsername();
        dto.role = u.getRole();
        dto.status = u.getStatus();

        if ("SUBJECT".equals(u.getRole())) {
            subjectRepo.findByUserId(u.getId()).ifPresent(sub -> {
                dto.displayName = sub.getDisplayName();
                dto.gender = sub.getGender();
                dto.age = sub.getAge();
                dto.subjectCode = sub.getSubjectCode();
                dto.contactInfo = sub.getContactInfo();
                dto.diagnosisResult = sub.getDiagnosisResult();
                dto.subjectName = sub.getSubjectName();
                if (sub.getGroupId() != null) {
                    groupRepo.findById(sub.getGroupId()).ifPresent(g -> dto.group_name = g.getName());
                }
            });
        }
        return dto;
    }
}
