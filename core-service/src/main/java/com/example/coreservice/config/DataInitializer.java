package com.example.coreservice.config;

import com.example.coreservice.entity.NguoiDung;
import com.example.coreservice.entity.VaiTro;
import com.example.coreservice.repository.NguoiDungRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(NguoiDungRepository nguoiDungRepository, PasswordEncoder passwordEncoder) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String defaultEmail = "admin@gmail.com";
        if (!nguoiDungRepository.existsByEmail(defaultEmail)) {
            NguoiDung admin = new NguoiDung();
            admin.setEmail(defaultEmail);
            admin.setMatKhauHash(passwordEncoder.encode("123456"));
            admin.setVaiTro(VaiTro.ADMIN);
            nguoiDungRepository.save(admin);
            log.info("==================================================================");
            log.info(" [KHỞI TẠO HỆ THỐNG] Đã tạo tài khoản quản trị viên mặc định:     ");
            log.info(" - Email:    {}", defaultEmail);
            log.info(" - Mật khẩu: 123456                                            ");
            log.info(" - Vai trò:  ADMIN                                             ");
            log.info("==================================================================");
        }
    }
}
