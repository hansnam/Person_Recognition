package com.example.coreservice;

import com.example.coreservice.service.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    @DisplayName("Kiểm tra gửi email cảnh báo đúng người nhận, tiêu đề và nội dung HTML")
    void testSendMissingPersonAlert() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendMissingPersonAlert(
                "nguoithan@example.com",
                "Nguyễn Văn A",
                0.94f,
                LocalDateTime.of(2026, 3, 16, 10, 30),
                "/uploads/captured_face.jpg"
        );

        // Chờ 500ms vì chạy async trong ThreadPool
        Thread.sleep(500);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage);
        assertEquals(1, sentMessage.getAllRecipients().length);
        assertEquals("nguoithan@example.com", sentMessage.getAllRecipients()[0].toString());
        assertTrue(sentMessage.getSubject().contains("Nguyễn Văn A"));
    }

    @Test
    @DisplayName("Kiểm tra không gửi email nếu địa chỉ email rỗng hoặc không hợp lệ")
    void testInvalidEmailDoesNotSend() throws Exception {
        emailService.sendMissingPersonAlert("", "Nguyễn Văn A", 0.94f, LocalDateTime.now(), "/uploads/test.jpg");
        emailService.sendMissingPersonAlert(null, "Nguyễn Văn A", 0.94f, LocalDateTime.now(), "/uploads/test.jpg");
        emailService.sendMissingPersonAlert("invalid-email-format", "Nguyễn Văn A", 0.94f, LocalDateTime.now(), "/uploads/test.jpg");

        Thread.sleep(300);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
