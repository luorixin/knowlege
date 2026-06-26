package com.sunxin.knowledge.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@ConditionalOnBean(CurrentUserResolver.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserResolver currentUserResolver;
    private final boolean headerFallbackEnabled;

    public WebMvcConfig(
            CurrentUserResolver currentUserResolver,
            @Value("${knowledge.security.header-fallback-enabled:false}") boolean headerFallbackEnabled
    ) {
        this.currentUserResolver = currentUserResolver;
        this.headerFallbackEnabled = headerFallbackEnabled;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver(currentUserResolver, headerFallbackEnabled));
    }
}
