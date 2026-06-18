package com.sunxin.knowledge.auth.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.PermissionManagementService;
import com.sunxin.knowledge.auth.api.dto.AddMemberRequest;
import com.sunxin.knowledge.auth.api.dto.CreateRoleRequest;
import com.sunxin.knowledge.auth.api.dto.MemberDto;
import com.sunxin.knowledge.auth.api.dto.PolicyDto;
import com.sunxin.knowledge.auth.api.dto.RoleDto;
import com.sunxin.knowledge.common.api.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionManagementService permissionManagementService;

    public PermissionController(PermissionManagementService permissionManagementService) {
        this.permissionManagementService = permissionManagementService;
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleDto>> getRoles(CurrentUser currentUser) {
        return ApiResponse.ok(permissionManagementService.getRoles(currentUser.tenantId()));
    }

    @PostMapping("/roles")
    public ApiResponse<RoleDto> createRole(
            CurrentUser currentUser,
            @Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.ok(permissionManagementService.createRole(currentUser.tenantId(), request));
    }

    @DeleteMapping("/roles/{roleId}")
    public ApiResponse<Void> deleteRole(
            CurrentUser currentUser,
            @PathVariable Long roleId) {
        permissionManagementService.deleteRole(currentUser.tenantId(), roleId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/members")
    public ApiResponse<List<MemberDto>> getMembers(
            CurrentUser currentUser,
            @RequestParam(required = false) Long spaceId) {
        return ApiResponse.ok(permissionManagementService.getMembers(currentUser.tenantId()));
    }

    @PostMapping("/members")
    public ApiResponse<Void> addMember(
            CurrentUser currentUser,
            @Valid @RequestBody AddMemberRequest request) {
        permissionManagementService.addMember(currentUser.tenantId(), request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/policies")
    public ApiResponse<List<PolicyDto>> getPolicies(
            CurrentUser currentUser,
            @RequestParam(required = false) Long spaceId) {
        return ApiResponse.ok(permissionManagementService.getPolicies(currentUser.tenantId(), spaceId));
    }
}
