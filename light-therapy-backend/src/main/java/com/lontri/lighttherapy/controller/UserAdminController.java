package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.UserDtos;
import com.lontri.lighttherapy.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping({"/api/users", "/api/v1/users"})
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue="0") int page,
                               @RequestParam(defaultValue="20") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        return ApiResponse.ok(userService.list(pageable));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserDtos.UserDTO> update(@PathVariable Long id,
                                                @RequestBody @Valid UserDtos.UpdateUserReq req) {
        return ApiResponse.ok(userService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.disable(id);
        return ApiResponse.ok();
    }

    @PostMapping("/subjects")
    public ApiResponse<Void> createSubject(@RequestBody @Valid UserDtos.CreateSubjectReq req) {
        userService.createSubject(req);
        return ApiResponse.ok();
    }
}
