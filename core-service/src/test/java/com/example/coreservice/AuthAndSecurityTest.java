package com.example.coreservice;

import com.example.coreservice.client.MlServiceClient;
import com.example.coreservice.dto.MlMatchResponse;
import com.example.coreservice.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MlServiceClient mlServiceClient;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("Test 1: Đăng nhập admin thành công và nhận JWT Token hợp lệ")
    void testLoginSuccessAndAccessProtectedApi() throws Exception {
        String loginPayload = "{\"email\": \"admin@gmail.com\", \"matKhau\": \"123456\"}";

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email", is("admin@gmail.com")))
                .andExpect(jsonPath("$.vaiTro", is("ADMIN")))
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        String token = jsonNode.get("token").asText();
        assertNotNull(token);

        // Dùng token vừa nhận được để truy cập API quản trị /api/nguoi-mat-tich
        mockMvc.perform(get("/api/nguoi-mat-tich")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test 2: Đăng nhập thất bại khi sai mật khẩu")
    void testLoginWrongPassword() throws Exception {
        String loginPayload = "{\"email\": \"admin@gmail.com\", \"matKhau\": \"sai-mat-khau\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("không chính xác")));
    }

    @Test
    @DisplayName("Test 3: Chặn truy cập API quản trị khi không có JWT Token (HTTP 403)")
    void testProtectedApiWithoutTokenReturns403() throws Exception {
        mockMvc.perform(get("/api/nguoi-mat-tich"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test 4: Cho phép API nhận diện camera truy cập công khai không cần token (HTTP 200)")
    void testPublicDetectionApiWithoutTokenReturns200() throws Exception {
        MlMatchResponse mockNoMatch = new MlMatchResponse();
        mockNoMatch.setMatched(false);
        mockNoMatch.setSimilarity(0.1f);
        Mockito.when(mlServiceClient.detectAndMatch(any(), any())).thenReturn(mockNoMatch);

        MockMultipartFile camFile = new MockMultipartFile(
                "file", "camera.jpg", "image/jpeg", "bytes".getBytes()
        );

        // Gửi không kèm header Authorization
        mockMvc.perform(multipart("/api/detection/match").file(camFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched", is(false)));
    }
}
