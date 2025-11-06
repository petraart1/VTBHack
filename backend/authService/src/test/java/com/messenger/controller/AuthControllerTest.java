package com.messenger.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository repo;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @BeforeAll
    public static void setup() {
    }

    @BeforeEach
    public void setUp() {

    }

    @AfterEach
    public void tearDown() {
        repo.deleteAll();
    }

    @Test
    public void goodRegisterUser() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "username",
                "email@yandex.ru",
                "1234567"
        );
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    public void badRegisterUserWithoutUsername() throws Exception {
        String json = """
        {
            "email": "username@yandex.ru",
            "password": "1234567"
        }
        """;

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    /*@Test
    void badRegisterUserWithoutPassword() throws Exception {
        userCreds.password("");

        MvcResult result = mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String error = objectMapper.readTree(response).get("error").asText();

        assertEquals(error, "Invalid input");
    }

    @Test
    void badRegisterUserWithoutEmail() throws Exception {
        userCreds.email("");

        MvcResult result = mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String error = objectMapper.readTree(response).get("error").asText();

        assertEquals(error, "Invalid input");
    }

    @Test
    void badRegisterUserTestWithoutAll() throws Exception {
        userCreds.email("");
        userCreds.password("");

        MvcResult result = mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String error = objectMapper.readTree(response).get("error").asText();

        assertEquals(error, "Invalid input");
    }

    @Test
    void goodAuthPost() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authToken").exists());
    }

    @Test
    void badAuthPostWithoutRegister() throws Exception {
        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void badAuthPostWithWrongEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isOk());

        userCreds.email("a@email.com");

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void badAuthPostWithWrongPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isOk());

        userCreds.password("passws");

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void badAuthPostWithDisabledUser() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isOk());

        UserDAO user = repo.findFirstByEmail(userCreds.getEmail()).orElseThrow();
        user.setEnabled(false);
        repo.save(user);

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void goodDeleteAccount() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authToken").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String authToken = objectMapper.readTree(response).get("authToken").asText();

        mockMvc.perform(delete("/account")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isOk());
    }

    @Test
    void badDeleteAccount() throws Exception {
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreds)))
                .andExpect(status().isUnauthorized());
    }*/

}
