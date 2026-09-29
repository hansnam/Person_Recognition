package com.example.coreservice;

import com.example.coreservice.client.MlServiceClient;
import com.example.coreservice.dto.MlMatchResponse;
import com.example.coreservice.dto.MlRegisterResponse;
import com.example.coreservice.entity.NguoiMatTich;
import com.example.coreservice.repository.LogPhatHienRepository;
import com.example.coreservice.repository.NguoiMatTichRepository;
import com.example.coreservice.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
class Step5EndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NguoiMatTichRepository nguoiMatTichRepository;

    @Autowired
    private LogPhatHienRepository logPhatHienRepository;

    @MockitoBean
    private MlServiceClient mlServiceClient;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setup() {
        logPhatHienRepository.deleteAll();
        nguoiMatTichRepository.deleteAll();
    }

    @Test
    @DisplayName("Test 1: Đăng ký hồ sơ người mất tích qua POST /api/nguoi-mat-tich")
    void testRegisterMissingPerson() throws Exception {
        // Giả lập ML Service đăng ký khuôn mặt trả về vector_id = 501
        MlRegisterResponse mockMlReg = new MlRegisterResponse();
        mockMlReg.setVectorId(501L);
        mockMlReg.setEmbeddingDim(512);
        mockMlReg.setConfidence(0.95f);
        mockMlReg.setBbox(List.of(100, 100, 200, 200));
        mockMlReg.setMessage("Đăng ký khuôn mặt thành công.");

        Mockito.when(mlServiceClient.registerFace(any(), any())).thenReturn(mockMlReg);

        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "person_a.jpg", "image/jpeg", "fake image bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/nguoi-mat-tich")
                        .file(imageFile)
                        .param("hoTen", "Nguyễn Văn A")
                        .param("ngayMatTich", "2026-03-01")
                        .param("khuVuc", "Hà Nội")
                        .param("lienHeNguoiThan", "nguoithan@example.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.hoTen", is("Nguyễn Văn A")))
                .andExpect(jsonPath("$.vectorIdFaiss", is(501)))
                .andExpect(jsonPath("$.anhDaiDienUrl", containsString("/uploads/")));

        // Xác nhận dữ liệu trong MySQL
        List<NguoiMatTich> danhSach = nguoiMatTichRepository.findAll();
        assertEquals(1, danhSach.size());
        assertEquals("Nguyễn Văn A", danhSach.get(0).getHoTen());
        assertEquals(501L, danhSach.get(0).getVectorIdFaiss());
    }

    @Test
    @DisplayName("Test 2: Nhận diện khuôn mặt khớp hồ sơ qua POST /api/detection/match")
    void testDetectAndMatchSuccess() throws Exception {
        // Tạo trước 1 hồ sơ trong MySQL với vectorIdFaiss = 777
        NguoiMatTich nguoi = new NguoiMatTich();
        nguoi.setHoTen("Trần Thị B");
        nguoi.setAnhDaiDienUrl("/uploads/person_b.jpg");
        nguoi.setVectorIdFaiss(777L);
        nguoi.setNgayMatTich(LocalDate.of(2026, 2, 20));
        nguoi.setKhuVuc("Đà Nẵng");
        nguoi.setLienHeNguoiThan("me_b@gmail.com");
        nguoi = nguoiMatTichRepository.save(nguoi);

        // Giả lập ML Service so khớp tìm thấy vector_id = 777
        MlMatchResponse mockMatch = new MlMatchResponse();
        mockMatch.setMatched(true);
        mockMatch.setVectorId(777L);
        mockMatch.setSimilarity(0.92f);
        mockMatch.setBbox(List.of(50, 50, 150, 150));
        mockMatch.setTotalFacesDetected(1);

        Mockito.when(mlServiceClient.detectAndMatch(any(), any())).thenReturn(mockMatch);

        MockMultipartFile camFile = new MockMultipartFile(
                "file", "camera_frame.jpg", "image/jpeg", "frame bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/detection/match")
                        .file(camFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched", is(true)))
                .andExpect(jsonPath("$.similarity", closeTo(0.92, 0.01)))
                .andExpect(jsonPath("$.hoSo.hoTen", is("Trần Thị B")))
                .andExpect(jsonPath("$.logId").isNumber());

        // Xác nhận log đã tự động lưu vào MySQL
        assertEquals(1, logPhatHienRepository.findAll().size());
        assertEquals(0.92f, logPhatHienRepository.findAll().get(0).getDoTinCay(), 0.01f);

        // Xác nhận ĐÃ KÍCH HOẠT GỬI EMAIL CẢNH BÁO tới đúng người thân
        Mockito.verify(emailService, Mockito.times(1))
                .sendMissingPersonAlert(eq("me_b@gmail.com"), eq("Trần Thị B"), any(), any(), any());
    }

    @Test
    @DisplayName("Test 3: Nhận diện người lạ không khớp qua POST /api/detection/match")
    void testDetectAndMatchNoMatch() throws Exception {
        // Giả lập ML Service không tìm thấy hồ sơ nào vượt ngưỡng
        MlMatchResponse mockNoMatch = new MlMatchResponse();
        mockNoMatch.setMatched(false);
        mockNoMatch.setVectorId(null);
        mockNoMatch.setSimilarity(0.25f);
        mockNoMatch.setTotalFacesDetected(1);
        mockNoMatch.setMessage("Không khớp với hồ sơ nào.");

        Mockito.when(mlServiceClient.detectAndMatch(any(), any())).thenReturn(mockNoMatch);

        MockMultipartFile camFile = new MockMultipartFile(
                "file", "stranger.jpg", "image/jpeg", "stranger bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/detection/match")
                        .file(camFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched", is(false)))
                .andExpect(jsonPath("$.hoSo").doesNotExist());

        // Xác nhận KHÔNG ghi log vào MySQL khi không khớp (open-set)
        assertEquals(0, logPhatHienRepository.findAll().size());

        // Xác nhận KHÔNG gửi email khi không khớp
        Mockito.verify(emailService, Mockito.never())
                .sendMissingPersonAlert(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Test 4: Xem lịch sử phát hiện qua GET /api/logs")
    void testGetLogs() throws Exception {
        NguoiMatTich nguoi = new NguoiMatTich();
        nguoi.setHoTen("Lê Văn C");
        nguoi.setVectorIdFaiss(888L);
        nguoi.setLienHeNguoiThan("contact@example.com");
        nguoi = nguoiMatTichRepository.save(nguoi);

        // Gọi phát hiện để sinh log
        MlMatchResponse mockMatch = new MlMatchResponse();
        mockMatch.setMatched(true);
        mockMatch.setVectorId(888L);
        mockMatch.setSimilarity(0.85f);
        Mockito.when(mlServiceClient.detectAndMatch(any(), any())).thenReturn(mockMatch);

        MockMultipartFile camFile = new MockMultipartFile(
                "file", "frame.jpg", "image/jpeg", "bytes".getBytes()
        );
        mockMvc.perform(multipart("/api/detection/match").file(camFile)).andExpect(status().isOk());

        // Lấy danh sách log
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].hoTenNguoiMatTich", is("Lê Văn C")));
    }

    @Test
    @DisplayName("Test 5: Xoá hồ sơ qua DELETE /api/nguoi-mat-tich/{id} (đồng bộ MySQL + FAISS)")
    void testDeleteMissingPerson() throws Exception {
        NguoiMatTich nguoi = new NguoiMatTich();
        nguoi.setHoTen("Phạm Văn D");
        nguoi.setVectorIdFaiss(999L);
        nguoi.setLienHeNguoiThan("family@gmail.com");
        nguoi = nguoiMatTichRepository.save(nguoi);

        mockMvc.perform(delete("/api/nguoi-mat-tich/" + nguoi.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Xác nhận đã gọi xoá vector 999 ở ML Service
        Mockito.verify(mlServiceClient, Mockito.times(1)).deleteFace(eq(999L));

        // Xác nhận đã xoá trong MySQL
        assertTrue(nguoiMatTichRepository.findById(nguoi.getId()).isEmpty());
    }
}
