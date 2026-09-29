import sys
import os
import requests
import json
import cv2
import numpy as np

sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080/api"
BASE_DIR = os.path.dirname(os.path.dirname(__file__))
SAMPLE_IMG = os.path.join(BASE_DIR, "ml-service", "test_samples", "person_a.jpg")
TEST_VIDEO = os.path.join(BASE_DIR, "scratch", "test_video_person_a.mp4")

def main():
    print("=== TEST NHẬN DIỆN NGƯỜI THÂN TRONG VIDEO QUA CORE SERVICE ===")
    # 1. Login Admin
    print("\n1. Đăng nhập Admin...")
    r = requests.post(f"{BASE_URL}/auth/login", json={"email": "admin@gmail.com", "matKhau": "123456"})
    if r.status_code != 200:
        print("Login failed:", r.status_code, r.text)
        return
    token = r.json().get("token")
    headers = {"Authorization": f"Bearer {token}"}
    print("Đăng nhập thành công! Token:", token[:20] + "...")

    # 2. Lấy danh sách hồ sơ hiện tại
    r = requests.get(f"{BASE_URL}/nguoi-mat-tich", headers=headers)
    profiles = r.json()
    print(f"Số hồ sơ hiện có: {len(profiles)}")

    person_a_profile = None
    for p in profiles:
        if "Nguyễn Văn Test" in p.get("hoTen", ""):
            person_a_profile = p
            break

    # Nếu chưa có hồ sơ Person A, tạo mới
    if not person_a_profile:
        print("Chưa có hồ sơ Person A, tiến hành đăng ký mới...")
        with open(SAMPLE_IMG, "rb") as f:
            files = {"file": ("person_a.jpg", f, "image/jpeg")}
            data = {
                "hoTen": "Nguyễn Văn Test",
                "ngayMatTich": "2026-09-20",
                "khuVuc": "Hà Nội - Hoàn Kiếm",
                "lienHeNguoiThan": "family.test@gmail.com"
            }
            r = requests.post(f"{BASE_URL}/nguoi-mat-tich", headers=headers, files=files, data=data)
            if r.status_code in [200, 201]:
                person_a_profile = r.json()
                print("Đăng ký thành công hồ sơ:", person_a_profile)
            else:
                print("Đăng ký thất bại:", r.status_code, r.text)
                return
    else:
        print("Đã có hồ sơ:", person_a_profile.get("hoTen"), "Vector ID:", person_a_profile.get("vectorIdFaiss"))

    # 3. Tạo video test có Person A
    print("\n2. Tạo video test 4 giây có Person A...")
    img = cv2.imread(SAMPLE_IMG)
    h, w = 480, 640
    img_resized = cv2.resize(img, (w, h))
    black_frame = np.zeros((h, w, 3), dtype=np.uint8)
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(TEST_VIDEO, fourcc, 25, (w, h))
    for i in range(100):  # 4s x 25fps = 100 frames
        # 1s đen, 3s có mặt Person A
        out.write(black_frame if i < 25 else img_resized)
    out.release()
    print("Đã tạo video test tại:", TEST_VIDEO)

    # 4. Gửi video tới POST /api/detection/match-video
    print("\n3. Gửi video tới Core Service POST /api/detection/match-video...")
    with open(TEST_VIDEO, "rb") as vf:
        files = {"file": ("test_video_person_a.mp4", vf, "video/mp4")}
        r = requests.post(f"{BASE_URL}/detection/match-video?threshold=0.45&frameInterval=1.0", files=files, timeout=60)
        print("Status code:", r.status_code)
        resp = r.json()
        print("\n--- KẾT QUẢ TRẢ VỀ CHO FRONTEND ---")
        print("Video URL:", resp.get("videoUrl"))
        print("Duration (giây):", resp.get("durationSeconds"))
        print("Tổng số khung hình:", resp.get("totalFrames"))
        print("Khung hình đã quét:", resp.get("processedFrames"))
        print("Trùng khớp (Matched):", resp.get("matched"))
        print("Số lượt khớp:", resp.get("totalMatches"))
        print("Thông điệp:", resp.get("message"))
        print(f"Số người mất tích tìm thấy: {len(resp.get('uniquePersons', []))}")
        for up in resp.get("uniquePersons", []):
            print(f" - Họ tên: {up.get('person', {}).get('hoTen')}")
            print(f"   Độ tương đồng cao nhất: {up.get('maxSimilarity') * 100:.1f}%")
            print(f"   Số lần xuất hiện: {up.get('occurrencesCount')} lần")
            print(f"   Mốc thời gian: {up.get('timestamps')}")
            print(f"   Ảnh snapshot tốt nhất: {up.get('bestSnapshotUrl')}")
            print(f"   Log ID: #{up.get('logId')}")

        print(f"\nDòng thời gian sự kiện (Timeline): {len(resp.get('timeline', []))} mốc")
        for ev in resp.get("timeline", []):
            print(f" - Mốc [{ev.get('timestampFormatted')}] ({ev.get('timestamp')}s): {ev.get('person', {}).get('hoTen')} ({ev.get('similarity') * 100:.1f}%), Snapshot: {ev.get('snapshotUrl')}")

    # 5. Dọn dẹp video test
    if os.path.exists(TEST_VIDEO):
        os.remove(TEST_VIDEO)
        print("\nĐã dọn dẹp file video test.")

if __name__ == "__main__":
    main()
