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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "order-desk.auth.admin-self-signup=true",
        "spring.datasource.url=jdbc:h2:mem:local_admin_signup;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
class LocalAdminSelfSignupTest {
    private static final String PASSWORD = "mockmvc-password-for-test";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void administratorCanSignUpAndSignInWithExistingProfiles() throws Exception {
        mvc.perform(post("/api/auth/setup-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountRequest("existing-admin")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register-staff/ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountRequest("new-admin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("new-admin"))
                .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"));

        var login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "new-admin", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = assertInstanceOf(MockHttpSession.class, login.getRequest().getSession(false));
        mvc.perform(get("/api/admin/dashboard").session(session))
                .andExpect(status().isOk());
    }

    private String accountRequest(String username) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", PASSWORD,
                "confirmPassword", PASSWORD
        ));
    }
}
