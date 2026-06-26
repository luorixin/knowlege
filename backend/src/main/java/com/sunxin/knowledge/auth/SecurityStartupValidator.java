package com.sunxin.knowledge.auth;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SecurityStartupValidator implements ApplicationRunner {

    static final String DEFAULT_JWT_SECRET = "KnowledgePlatformMVPSecretKey1234567890";
    private static final Set<String> LOCAL_PROFILES = Set.of("dev", "test", "local");

    private final boolean securityEnabled;
    private final String jwtSecret;
    private final String[] activeProfiles;

    @Autowired
    public SecurityStartupValidator(
            @Value("${knowledge.security.enabled:true}") boolean securityEnabled,
            @Value("${knowledge.jwt.secret:}") String jwtSecret,
            Environment environment
    ) {
        this(securityEnabled, jwtSecret, environment.getActiveProfiles());
    }

    SecurityStartupValidator(boolean securityEnabled, String jwtSecret, String[] activeProfiles) {
        this.securityEnabled = securityEnabled;
        this.jwtSecret = jwtSecret;
        this.activeProfiles = activeProfiles == null ? new String[0] : activeProfiles.clone();
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    void validate() {
        if (!securityEnabled || isLocalProfile()) {
            return;
        }
        if (jwtSecret == null || jwtSecret.isBlank() || DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("JWT secret must be explicitly configured for non-local security-enabled runtime");
        }
    }

    private boolean isLocalProfile() {
        Set<String> profiles = Arrays.stream(activeProfiles)
                .map(SecurityStartupValidator::normalize)
                .collect(Collectors.toSet());
        return profiles.stream().anyMatch(LOCAL_PROFILES::contains);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
