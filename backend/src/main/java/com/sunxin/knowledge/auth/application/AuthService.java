package com.sunxin.knowledge.auth.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.auth.api.dto.LoginRequest;
import com.sunxin.knowledge.auth.api.dto.LoginResponse;
import com.sunxin.knowledge.common.error.UnauthorizedException;
import com.sunxin.knowledge.persistence.entity.KbUser;
import com.sunxin.knowledge.persistence.repository.KbUserRepository;
import com.sunxin.knowledge.persistence.repository.KbUserRoleRepository;

@Service
public class AuthService {

    private final KbUserRepository userRepository;
    private final KbUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthService(KbUserRepository userRepository, KbUserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        KbUser user = userRepository.findFirstByUsernameAndStatus(request.username(), "ACTIVE")
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        java.util.Set<String> roles = userRoleRepository.findActiveRoleCodes(user.getTenantId(), user.getId());

        Instant now = Instant.now();
        Instant expiresAt = now.plus(24, ChronoUnit.HOURS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("knowledge-platform")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("tenantId", user.getTenantId())
                .claim("roles", roles)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", 86400);
    }
}
