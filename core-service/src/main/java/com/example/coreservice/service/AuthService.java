package com.example.coreservice.service;

import com.example.coreservice.dto.LoginRequest;
import com.example.coreservice.dto.LoginResponse;
import com.example.coreservice.entity.NguoiDung;
import com.example.coreservice.repository.NguoiDungRepository;
import com.example.coreservice.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(NguoiDungRepository nguoiDungRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Xác thực đăng nhập và cấp phát JWT token
     */
    public LoginResponse login(LoginRequest request) {
        NguoiDung user = nguoiDungRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getMatKhau(), user.getMatKhauHash())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác");
        }

        String token = jwtUtils.generateToken(user.getEmail(), user.getVaiTro().name());

        return new LoginResponse(token, user.getEmail(), user.getVaiTro());
    }
}
