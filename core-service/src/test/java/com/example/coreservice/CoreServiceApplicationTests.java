package com.example.coreservice;

import com.example.coreservice.entity.LogPhatHien;
import com.example.coreservice.entity.NguoiDung;
import com.example.coreservice.entity.NguoiMatTich;
import com.example.coreservice.entity.VaiTro;
import com.example.coreservice.repository.LogPhatHienRepository;
import com.example.coreservice.repository.NguoiDungRepository;
import com.example.coreservice.repository.NguoiMatTichRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CoreServiceApplicationTests {

	@Autowired
	private NguoiMatTichRepository nguoiMatTichRepository;

	@Autowired
	private LogPhatHienRepository logPhatHienRepository;

	@Autowired
	private NguoiDungRepository nguoiDungRepository;

	@Test
	@DisplayName("Kiểm tra Spring Boot Context tải thành công và kết nối MySQL thông suốt")
	void contextLoads() {
		assertNotNull(nguoiMatTichRepository);
		assertNotNull(logPhatHienRepository);
		assertNotNull(nguoiDungRepository);
	}

	@Test
	@DisplayName("Kiểm tra CRUD Entity NguoiMatTich và ánh xạ vector_id_faiss")
	void testNguoiMatTichCrud() {
		long testFaissId = 999001L;
		// Xoá bản ghi cũ nếu có từ lần chạy trước
		nguoiMatTichRepository.findByVectorIdFaiss(testFaissId).ifPresent(n -> {
			logPhatHienRepository.findByNguoiMatTichIdOrderByThoiGianDesc(n.getId())
					.forEach(logPhatHienRepository::delete);
			nguoiMatTichRepository.delete(n);
		});

		NguoiMatTich nguoi = new NguoiMatTich();
		nguoi.setHoTen("Nguyễn Văn Test");
		nguoi.setAnhDaiDienUrl("http://localhost:8080/uploads/test.jpg");
		nguoi.setVectorIdFaiss(testFaissId);
		nguoi.setNgayMatTich(LocalDate.of(2026, 1, 15));
		nguoi.setKhuVuc("Hà Nội");
		nguoi.setLienHeNguoiThan("nguoithan@example.com");

		NguoiMatTich saved = nguoiMatTichRepository.save(nguoi);
		assertNotNull(saved.getId(), "ID phải được tự động sinh (AUTO_INCREMENT)");
		assertNotNull(saved.getCreatedAt(), "CreatedAt phải được tự động gán");

		// Tìm theo vectorIdFaiss
		Optional<NguoiMatTich> found = nguoiMatTichRepository.findByVectorIdFaiss(testFaissId);
		assertTrue(found.isPresent());
		assertEquals("Nguyễn Văn Test", found.get().getHoTen());

		// Test LogPhatHien liên kết NguoiMatTich
		LogPhatHien log = new LogPhatHien();
		log.setNguoiMatTich(saved);
		log.setThoiGian(LocalDateTime.now());
		log.setDoTinCay(0.88f);
		log.setAnhChupUrl("http://localhost:8080/uploads/cam_test.jpg");

		LogPhatHien savedLog = logPhatHienRepository.save(log);
		assertNotNull(savedLog.getId());

		List<LogPhatHien> logs = logPhatHienRepository.findByNguoiMatTichIdOrderByThoiGianDesc(saved.getId());
		assertFalse(logs.isEmpty());
		assertEquals(0.88f, logs.get(0).getDoTinCay(), 0.001f);

		// Dọn dẹp bản ghi test
		logPhatHienRepository.delete(savedLog);
		nguoiMatTichRepository.delete(saved);
	}

	@Test
	@DisplayName("Kiểm tra Entity NguoiDung và VaiTro (ADMIN/STAFF)")
	void testNguoiDungCrud() {
		String testEmail = "admin_test@system.local";
		nguoiDungRepository.findByEmail(testEmail).ifPresent(nguoiDungRepository::delete);

		NguoiDung user = new NguoiDung();
		user.setEmail(testEmail);
		user.setMatKhauHash("$2a$10$hashedpasswordhere");
		user.setVaiTro(VaiTro.ADMIN);

		NguoiDung savedUser = nguoiDungRepository.save(user);
		assertNotNull(savedUser.getId());
		assertEquals(VaiTro.ADMIN, savedUser.getVaiTro());

		assertTrue(nguoiDungRepository.existsByEmail(testEmail));

		nguoiDungRepository.delete(savedUser);
	}
}
