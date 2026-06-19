package com.sunxin.knowledge.auth;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.auth.api.dto.AddMemberRequest;
import com.sunxin.knowledge.auth.api.dto.CreateRoleRequest;
import com.sunxin.knowledge.auth.api.dto.MemberDto;
import com.sunxin.knowledge.auth.api.dto.PolicyDto;
import com.sunxin.knowledge.auth.api.dto.RoleDto;
import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;
import com.sunxin.knowledge.persistence.entity.KbRole;
import com.sunxin.knowledge.persistence.entity.KbUser;
import com.sunxin.knowledge.persistence.entity.KbUserRole;
import com.sunxin.knowledge.persistence.repository.KbPermissionPolicyRepository;
import com.sunxin.knowledge.persistence.repository.KbRoleRepository;
import com.sunxin.knowledge.persistence.repository.KbUserRepository;
import com.sunxin.knowledge.persistence.repository.KbUserRoleRepository;

@Service
public class PermissionManagementService {

    private final KbRoleRepository roleRepository;
    private final KbUserRepository userRepository;
    private final KbUserRoleRepository userRoleRepository;
    private final KbPermissionPolicyRepository policyRepository;

    public PermissionManagementService(
            KbRoleRepository roleRepository,
            KbUserRepository userRepository,
            KbUserRoleRepository userRoleRepository,
            KbPermissionPolicyRepository policyRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.policyRepository = policyRepository;
    }

    @Cacheable(value = "roles", key = "#tenantId")
    public List<RoleDto> getRoles(Long tenantId) {
        List<KbRole> roles = roleRepository.findByTenantId(tenantId);
        return roles.stream().map(role -> {
            boolean isSystem = role.getCode().startsWith("SYS_");
            return new RoleDto(
                role.getId().toString(),
                role.getName(),
                role.getDescription(),
                0, // Mocked memberCount for MVP
                0, // Mocked policyCount for MVP
                isSystem
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "roles", key = "#tenantId")
    public RoleDto createRole(Long tenantId, CreateRoleRequest req) {
        KbRole role = new KbRole();
        role.setTenantId(tenantId);
        role.setCode("CUSTOM_" + System.currentTimeMillis());
        role.setName(req.name());
        role.setDescription(req.description());
        role.setStatus("ACTIVE");
        role = roleRepository.save(role);
        return new RoleDto(role.getId().toString(), role.getName(), role.getDescription(), 0, 0, false);
    }

    @Transactional
    @CacheEvict(value = "roles", key = "#tenantId")
    public void deleteRole(Long tenantId, Long roleId) {
        KbRole role = roleRepository.findByTenantIdAndIdAndStatus(tenantId, roleId, "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Role not found"));
        if (role.getCode().startsWith("SYS_")) {
            throw new BadRequestException("System roles cannot be deleted");
        }
        role.setStatus("DELETED");
        roleRepository.save(role);
    }

    @Cacheable(value = "members", key = "#tenantId")
    public List<MemberDto> getMembers(Long tenantId) {
        List<KbUserRole> userRoles = userRoleRepository.findByTenantId(tenantId);
        List<KbUser> users = userRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
        List<KbRole> roles = roleRepository.findByTenantId(tenantId);

        return users.stream()
            .map(u -> {
                String roleName = userRoles.stream()
                    .filter(ur -> ur.getUserId().equals(u.getId()) && "ACTIVE".equals(ur.getStatus()))
                    .map(ur -> roles.stream()
                        .filter(r -> r.getId().equals(ur.getRoleId()))
                        .map(KbRole::getName)
                        .findFirst().orElse("Unknown"))
                    .findFirst().orElse("None");

                return new MemberDto(
                    u.getId().toString(),
                    u.getUsername(),
                    u.getDisplayName(),
                    roleName,
                    u.getStatus()
                );
            }).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "members", key = "#tenantId")
    public void addMember(Long tenantId, AddMemberRequest req) {
        KbUser user = userRepository.findByTenantIdAndUsername(tenantId, req.username())
            .orElseThrow(() -> new BadRequestException("User not found: " + req.username()));
        Long roleId = Long.parseLong(req.roleId());
        
        // Disable existing roles for simplicity
        List<KbUserRole> existing = userRoleRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");
        existing.forEach(ur -> ur.setStatus("INACTIVE"));
        userRoleRepository.saveAll(existing);

        KbUserRole newRole = new KbUserRole();
        newRole.setTenantId(tenantId);
        newRole.setUserId(user.getId());
        newRole.setRoleId(roleId);
        newRole.setStatus("ACTIVE");
        userRoleRepository.save(newRole);
    }

    @Cacheable(value = "policies", key = "#tenantId + ':' + #spaceId")
    public List<PolicyDto> getPolicies(Long tenantId, Long spaceId) {
        List<KbPermissionPolicy> policies = policyRepository.findByTenantIdAndSpaceId(tenantId, spaceId);
        return policies.stream().map(p -> new PolicyDto(
            p.getId().toString(),
            "Policy_" + p.getId(), // Mock name if missing
            p.getResourceType() + ":" + (p.getResourceId() != null ? p.getResourceId() : "*"),
            List.of(p.getActions().split(",")),
            false
        )).collect(Collectors.toList());
    }
}
