package com.lontri.lighttherapy.service;

import com.lontri.lighttherapy.dto.SubjectListResp;
import com.lontri.lighttherapy.dto.UserDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserDtos.UserDTO> list(Pageable pageable);
    UserDtos.UserDTO update(Long id, UserDtos.UpdateUserReq req);
    void disable(Long id);
    void createSubject(UserDtos.CreateSubjectReq req);
    Page<SubjectListResp> listSubjects(Pageable pageable);
    void updateSubject(Long id, UserDtos.UpdateSubjectReq req);
    void deleteSubject(Long id);
}
