package com.example.coreservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@timnguoimattich.vn}")
    private String mailFrom;

    @Value("${app.mail.app-base-url:http://localhost:8080}")
    private String appBaseUrl;

    /**
     * Gửi email cảnh báo khẩn cấp tới người thân khi phát hiện khuôn mặt trùng khớp.
     * Chạy bất đồng bộ trong background thread (@Async) để không làm chậm camera API.
     */
    @Async("mailTaskExecutor")
    public void sendMissingPersonAlert(String toEmail, String hoTen, Float similarity,
                                       LocalDateTime thoiGian, String anhChupUrl) {
        if (!mailEnabled) {
            log.info("[EmailService] Tính năng gửi email đang tắt (app.mail.enabled=false). Bỏ qua gửi tới: {}", toEmail);
            return;
        }

        if (toEmail == null || toEmail.trim().isEmpty() || !toEmail.contains("@")) {
            log.warn("[EmailService] Địa chỉ email người thân không hợp lệ: '{}'. Bỏ qua gửi email.", toEmail);
            return;
        }

        if (mailSender == null) {
            log.warn("[EmailService] JavaMailSender chưa được cấu hình. Bỏ qua gửi email cảnh báo.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(toEmail.trim());
            helper.setSubject("🚨 [CẢNH BÁO KHẨN CẤP] Phát hiện người thân: " + hoTen);

            String formattedTime = thoiGian != null
                    ? thoiGian.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            String percentStr = similarity != null
                    ? String.format("%.1f%%", similarity * 100)
                    : "Không xác định";

            String fullImageUrl = anhChupUrl != null && !anhChupUrl.startsWith("http")
                    ? appBaseUrl + anhChupUrl
                    : (anhChupUrl != null ? anhChupUrl : "#");

            String htmlContent = buildAlertHtml(hoTen, formattedTime, percentStr, fullImageUrl);
            helper.setText(htmlContent, true);

            log.info("[EmailService] Đang gửi email cảnh báo tới: {} cho người mất tích: {}", toEmail, hoTen);
            mailSender.send(message);
            log.info("[EmailService] Đã gửi email cảnh báo thành công tới: {}", toEmail);

        } catch (MessagingException e) {
            log.error("[EmailService] Lỗi soạn thảo email gửi tới {}: {}", toEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("[EmailService] Lỗi kết nối SMTP server khi gửi email tới {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildAlertHtml(String hoTen, String thoiGian, String doTinCay, String imageUrl) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family: Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px;'>"
                + "  <div style='max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                + "    <div style='background-color: #d32f2f; padding: 20px; text-align: center; color: #ffffff;'>"
                + "      <h1 style='margin: 0; font-size: 22px;'>🚨 THÔNG BÁO PHÁT HIỆN NGƯỜI THÂN</h1>"
                + "      <p style='margin: 5px 0 0; font-size: 14px;'>Hệ thống nhận diện tự động bằng trí tuệ nhân tạo</p>"
                + "    </div>"
                + "    <div style='padding: 24px; color: #333333; line-height: 1.6;'>"
                + "      <p>Kính gửi gia đình,</p>"
                + "      <p>Hệ thống camera giám sát vừa phát hiện một trường hợp có đặc điểm khuôn mặt trùng khớp với người trong hồ sơ tìm kiếm của bạn:</p>"
                + "      <div style='background-color: #fff3f3; border-left: 4px solid #d32f2f; padding: 15px; margin: 15px 0; border-radius: 4px;'>"
                + "        <p style='margin: 4px 0;'><strong>👤 Họ và tên:</strong> <span style='font-size: 16px; color: #d32f2f; font-weight: bold;'>" + hoTen + "</span></p>"
                + "        <p style='margin: 4px 0;'><strong>⏰ Thời gian phát hiện:</strong> " + thoiGian + "</p>"
                + "        <p style='margin: 4px 0;'><strong>🎯 Độ tin cậy tương đồng:</strong> <span style='background-color: #e8f5e9; color: #2e7d32; padding: 2px 8px; border-radius: 4px; font-weight: bold;'>" + doTinCay + "</span></p>"
                + "      </div>"
                + "      <p>Bạn có thể bấm vào liên kết dưới đây để xem trực tiếp ảnh chụp từ camera tại thời điểm phát hiện:</p>"
                + "      <div style='text-align: center; margin: 25px 0;'>"
                + "        <a href='" + imageUrl + "' target='_blank' style='background-color: #1976d2; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 5px; font-weight: bold; display: inline-block;'>📸 Xem ảnh chụp tại camera</a>"
                + "      </div>"
                + "      <p style='font-size: 13px; color: #666666;'><em>* Lưu ý: Đây là email tự động từ hệ thống hỗ trợ tìm kiếm người mất tích. Hãy liên hệ với cơ quan chức năng hoặc đội cứu hộ để xác minh trực tiếp.</em></p>"
                + "    </div>"
                + "    <div style='background-color: #eceff1; padding: 12px; text-align: center; font-size: 12px; color: #78909c;'>"
                + "      Hệ thống thông báo phát hiện người mất tích © 2026"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }
}
