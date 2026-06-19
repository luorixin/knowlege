package com.sunxin.knowledge.common.health;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = OAuth2ResourceServerAutoConfiguration.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsBackendHealthAndReservedModuleNames() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("knowledge-backend"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.modules", hasItems(
                        "auth",
                        "knowledge-base",
                        "document",
                        "retrieval",
                        "qa",
                        "task",
                        "audit"
                )));
    }
}
