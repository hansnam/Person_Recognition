# HƯỚNG DẪN KHỞI CHẠY & VẬN HÀNH HỆ THỐNG FINDME AI
## Hệ Thống Thông Báo Phát Hiện Người Mất Tích Bằng AI Đa Phương Thức (Face & Body Re-ID)

---

### Tổng Quan Về Hệ Thống (System Overview)

Hệ thống **FINDME AI** là giải pháp giám sát và tìm kiếm người mất tích tự động, ứng dụng thị giác máy tính (Computer Vision) và học sâu (Deep Learning) kết hợp sinh trắc học đa phương thức (**Multimodal Biometrics: Face Recognition + Body Person Re-Identification**) theo thời gian thực nhằm hỗ trợ gia đình và lực lượng chức năng phát hiện người mất tích qua camera an ninh hoặc ảnh chụp hiện trường.

### Các Tính Năng Trọng Tâm:
1. **Quản trị hồ sơ người mất tích:** Khai báo thông tin cá nhân, khu vực mất tích, thông tin liên hệ gia đình; tự động trích xuất và đồng bộ cả vector đặc trưng khuôn mặt (Face) và dáng người/trang phục (Body Re-ID) vào cơ sở dữ liệu vector FAISS.
2. **Nhận diện đa phương thức thời gian thực (Multimodal Real-time Fusion):**
   - **Khuôn mặt (Face Recognition):** Phát hiện qua YOLOv8n-Face $\rightarrow$ Căn chỉnh 5 điểm mốc ArcFace (112×112) $\rightarrow$ Trích xuất vector 512 chiều bằng MobileFaceNet.
   - **Trang phục & Dáng người (Body Person Re-ID):** Phát hiện qua YOLOv8n $\rightarrow$ Căn chuẩn kích thước 256×128 RGB $\rightarrow$ Trích xuất vector 2048 chiều bằng ResNet-50 CUHK03 (BNNeck).
   - **Cơ chế Hợp nhất (Fusion Engine):** Liên kết Face - Body bằng kiểm tra hình học bao hàm (Containment Check), suy luận trạng thái kết hợp theo nguyên tắc **Ưu tiên Khuôn mặt (Face-Priority & Body-Assist)**.
3. **Cảnh báo khẩn cấp tức thì (Emergency Alert):** Kích hoạt âm thanh cảnh báo trực tiếp trên giao diện giám sát, hiển thị banner khẩn cấp đối soát ảnh thực tế và tự động gửi email thông báo kèm ảnh bằng chứng tới người thân.
4. **Lưu trữ & Truy vết lịch sử (Audit Log):** Ghi nhận chi tiết nhật ký các sự kiện nhận diện (thời gian, hình ảnh chụp, `face_similarity`, `body_similarity`, `fusion_status`, `body_warning`).

> 📖 **XEM CHI TIẾT TẠI:** **[ARCHITECTURE.md](ARCHITECTURE.md)**  
> *(Tài liệu chứa toàn bộ sơ đồ kiến trúc vi dịch vụ, luồng xử lý chi tiết Face/Body/Fusion, mô hình dữ liệu MySQL, FAISS index, và tài liệu các REST API endpoints).*

---

### Kiến Trúc Hệ Thống (System Architecture)

Hệ thống được thiết kế theo kiến trúc **Microservices** phân tán gồm 3 thành phần chính và cơ sở dữ liệu MySQL:

```text
┌─────────────────────────┐       ┌─────────────────────────┐       ┌─────────────────────────┐
│        FRONTEND         │       │      CORE SERVICE       │       │       ML SERVICE        │
│    React.js + Vite      │ ────▶ │    Java Spring Boot     │ ────▶ │     Python FastAPI      │
│  http://localhost:5173  │       │  http://localhost:8080  │       │  http://localhost:8000  │
└─────────────────────────┘       └────────────┬────────────┘       └─────────────────────────┘
                                               │
                                         ┌─────▼─────┐
                                         │   MySQL   │
                                         │ Port 3306 │
                                         └───────────┘
```

---

## 1. Yêu Cầu Môi Trường (Prerequisites)

- **Cơ sở dữ liệu**: MySQL Server 8.0+ (đang chạy cổng `3306`).
- **Python**: Python 3.10 – 3.13 (đã cài đặt thư viện vào thư mục `ml-service/venv`).
- **Java**: JDK 17+ (khuyến nghị JDK 21 hoặc JDK 24).
- **Node.js**: Phiên bản 18+ và `npm`.

---

## 2. Thứ Tự Khởi Chạy (Khuyến nghị)

Để các dịch vụ liên kết thông suốt, hãy mở **3 cửa sổ Terminal (PowerShell)** riêng biệt và chạy theo thứ tự:

1. **MySQL** (Đảm bảo database service đang chạy).
2. **ML Service** (Cổng 8000).
3. **Core Service** (Cổng 8080).
4. **Frontend** (Cổng 5173).

---

## 3. Chi Tiết Các Bước Khởi Chạy

### Bước 1: Chuẩn bị Cơ Sở Dữ Liệu (MySQL)
- Đảm bảo MySQL Server đang chạy trên máy (`localhost:3306`).
- Mặc định hệ thống dùng tài khoản:
  - **Username**: `root`
  - **Password**: `123456`
  *(Nếu mật khẩu MySQL máy bạn khác, hãy sửa tại file: `core-service/src/main/resources/application.yml`)*
- Spring Boot đã bật sẵn `createDatabaseIfNotExist=true`, do đó CSDL `face` và các bảng sẽ được tự động tạo khi chạy Core Service.

---

### Bước 2: Khởi chạy ML Service (Python FastAPI)

Mở **Terminal 1**:
```powershell
# Di chuyển vào thư mục ML Service
cd ml-service

# Kích hoạt môi trường ảo Python đã cài sẵn thư viện
.\venv\Scripts\activate

# Khởi động server FastAPI bằng Uvicorn
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

- **Kiểm tra hoạt động**:
  - Truy cập: [http://localhost:8000/ml/health](http://localhost:8000/ml/health)
  - Nếu hiển thị `{"status":"UP", ...}` là server AI đã sẵn sàng.
  - Xem tài liệu API Swagger: [http://localhost:8000/docs](http://localhost:8000/docs)

---

### Bước 3: Khởi chạy Core Service (Spring Boot)

Mở **Terminal 2**:
```powershell
# Di chuyển vào thư mục Core Service
cd core-service

# Chạy Spring Boot bằng Maven Wrapper
.\mvnw.cmd spring-boot:run
```
*(Trên Linux/macOS: `./mvnw spring-boot:run`)*

- **Kiểm tra hoạt động**:
  - Khi thấy thông báo `Tomcat started on port 8080 (http) with context path '/'` và `Started CoreServiceApplication` là thành công.
  - Endpoint backend: `http://localhost:8080`

---

### Bước 4: Khởi chạy Giao Diện Người Dùng (Frontend React)

Mở **Terminal 3**:
```powershell
# Di chuyển vào thư mục Frontend
cd frontend

# Cài đặt dependency (nếu lần đầu chạy)
npm install

# Khởi chạy dev server Vite
npm run dev
```

- **Truy cập ứng dụng**:
  - Mở trình duyệt và truy cập: **[http://localhost:5173](http://localhost:5173)**

---

## 4. Tài Khoản Đăng Nhập Mặc Định

Hệ thống tự động khởi tạo tài khoản Quản trị viên (ADMIN) khi Core Service khởi chạy lần đầu:

| Thông tin | Giá trị mặc định |
| :--- | :--- |
| **Email** | `admin@gmail.com` |
| **Mật khẩu** | `123456` |
| **Vai trò** | `ADMIN` (Toàn quyền quản trị hồ sơ và lịch sử) |

---

## 5. Hướng Dẫn Kiểm Thử Luồng Hoạt Động (End-to-End)

1. **Đăng nhập**: 
   - Truy cập `http://localhost:5173`, nhập tài khoản `admin@gmail.com` / `123456`.
2. **Đăng ký hồ sơ người mất tích**:
   - Vào mục **"Hồ sơ người mất tích"** -> Chọn **"Thêm hồ sơ"**.
   - Nhập họ tên, ngày mất tích, khu vực, email người thân để nhận cảnh báo.
   - Tải lên 1 ảnh chân dung rõ mặt (ảnh sẽ được gửi sang ML Service để trích xuất 512-d embedding và lưu vào index FAISS).
3. **Thực hiện nhận diện / Tìm kiếm**:
   - Vào mục **"Nhận diện / Camera"**.
   - Tải lên ảnh chụp giám sát hoặc sử dụng luồng camera trực tiếp.
   - Hệ thống sẽ phát hiện khuôn mặt bằng YOLOv8-Face, so khớp vector đặc trưng qua FAISS:
     - Nếu độ tương đồng >= ngưỡng (mặc định 0.45): Báo **"Khớp hồ sơ"**, vẽ bounding box, lưu bản ghi vào CSDL và tự động gửi email cảnh báo tới người thân.
     - Nếu không khớp: Thông báo không tìm thấy trong CSDL người mất tích (mô hình Open-set).
4. **Xem lịch sử phát hiện**:
   - Vào mục **"Lịch sử phát hiện"** để xem lại các sự kiện đối sánh đã diễn ra kèm ảnh chụp và độ tin cậy.

---

## 6. Xử Lý Sự Cố Thường Gặp (Troubleshooting)

| Lỗi | Nguyên nhân | Cách khắc phục |
| :--- | :--- | :--- |
| `Communications link failure` / `Access denied for user 'root'` | Chưa bật MySQL hoặc sai mật khẩu | Mở MySQL service trong Services Windows hoặc sửa lại mật khẩu đúng trong file `core-service/src/main/resources/application.yml`. |
| `Không thể kết nối tới ML Service (FastAPI)` | Chưa khởi chạy `ml-service` | Chạy lệnh `uvicorn` ở Bước 2 và đảm bảo truy cập được `http://localhost:8000/ml/health`. |
| `Port 8080/8000/5173 already in use` | Có tiến trình khác chiếm cổng | Dùng lệnh `netstat -ano \| findstr :8080` (hoặc cổng tương ứng) và `taskkill /F /PID <PID>` để giải phóng cổng. |
