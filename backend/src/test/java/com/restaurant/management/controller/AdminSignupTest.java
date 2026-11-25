package com.restaurant.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_signup_regression;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
class AdminSignupTest {
    private static final String ADDITIONAL_ADMIN_SIGNUP = "/api/auth/register-additional-admin";
    private static final String PASSWORD = "mockmvc-password-for-test";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void onlySignedInAdminCanCreateAnAdditionalAdmin() throws Exception {
        mvc.perform(get("/api/auth/setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.required").value(true));

        mvc.perform(post("/api/auth/setup-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountRequest("initial-admin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"));

        String additionalAdmin = accountRequest("additional-admin");
        mvc.perform(post("/api/auth/register-staff/ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(additionalAdmin))
                .andExpect(status().isConflict());

        mvc.perform(post(ADDITIONAL_ADMIN_SIGNUP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(additionalAdmin))
                .andExpect(status().is(anyOf(is(401), is(403))));

        mvc.perform(post("/api/auth/register-staff/KITCHEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountRequest("kitchen-staff")))
                .andExpect(status().isCreated());

        mvc.perform(post(ADDITIONAL_ADMIN_SIGNUP)
                        .session(signIn("kitchen-staff"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(additionalAdmin))
                .andExpect(status().isForbidden());

        MockHttpSession originalAdmin = signIn("initial-admin");
        mvc.perform(post(ADDITIONAL_ADMIN_SIGNUP)
                        .session(originalAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(additionalAdmin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("additional-admin"))
                .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"));

        mvc.perform(get("/api/auth/me").session(originalAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("initial-admin"));

        mvc.perform(get("/api/admin/dashboard").session(signIn("additional-admin")))
                .andExpect(status().isOk());
    }

    private MockHttpSession signIn(String username) throws Exception {
        var result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return assertInstanceOf(MockHttpSession.class, result.getRequest().getSession(false));
    }

    private String accountRequest(String username) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", PASSWORD,
                "confirmPassword", PASSWORD
        ));
    }
}
