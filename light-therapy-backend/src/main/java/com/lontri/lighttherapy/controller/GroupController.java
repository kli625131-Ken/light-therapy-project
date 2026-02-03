package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.dto.GroupDtos;
import com.lontri.lighttherapy.entity.ExperimentGroup;
import com.lontri.lighttherapy.service.GroupService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping({"/api/groups", "/api/v1/groups"})
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue="0") int page,
                               @RequestParam(defaultValue="20") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        return ApiResponse.ok(groupService.list(pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ExperimentGroup> create(@RequestBody @Valid GroupDtos.CreateGroupReq req) {
        return ApiResponse.ok(groupService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ExperimentGroup> update(@PathVariable Long id, @RequestBody @Valid GroupDtos.UpdateGroupReq req) {
        return ApiResponse.ok(groupService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        groupService.delete(id);
        return ApiResponse.ok();
    }
}
