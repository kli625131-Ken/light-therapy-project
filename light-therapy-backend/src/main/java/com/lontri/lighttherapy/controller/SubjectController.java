package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.UserDtos;
import com.lontri.lighttherapy.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping({ "/api/subjects", "/api/v1/subjects" })
public class SubjectController {

    private final UserService userService;

    public SubjectController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        return ApiResponse.ok(userService.listSubjects(pageable));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody @Valid UserDtos.CreateSubjectReq req) {
        userService.createSubject(req);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid UserDtos.UpdateSubjectReq req) {
        userService.updateSubject(id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.deleteSubject(id);
        return ApiResponse.ok();
    }
}
