"""
Script kiểm thử endpoint nhận diện video /ml/detect-and-match-video
1. Tạo video test bằng cv2.VideoWriter có chứa khuôn mặt person_a.jpg
2. Đăng ký person_a vào FAISS (nếu chưa có)
3. Gọi API POST /ml/detect-and-match-video
4. Kiểm tra các trường dữ liệu: duration, total_frames, timeline, unique_persons, snapshot_base64
5. Dọn dẹp dữ liệu kiểm thử
"""

import os
import io
import cv2
import numpy as np
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

BASE_DIR = os.path.dirname(__file__)
SAMPLES_DIR = os.path.join(BASE_DIR, "test_samples")
PERSON_A_IMG = os.path.join(SAMPLES_DIR, "person_a.jpg")
TEST_VIDEO_PATH = os.path.join(BASE_DIR, "test_sample_video.mp4")
TEST_VECTOR_ID = 999


def create_test_video(output_path: str, duration_sec: int = 3, fps: int = 25):
    """Tạo video giả lập có khuôn mặt person_a"""
    img = cv2.imread(PERSON_A_IMG)
    if img is None:
        raise FileNotFoundError(f"Không tìm thấy ảnh {PERSON_A_IMG}")
    
    h, w = 480, 640
    img_resized = cv2.resize(img, (w, h))
    black_frame = np.zeros((h, w, 3), dtype=np.uint8)

    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(output_path, fourcc, fps, (w, h))

    total_frames = duration_sec * fps
    for i in range(total_frames):
        # Nửa đầu: màn hình đen (không có mặt), Nửa sau: ảnh Person A (có mặt)
        if i < fps:
            out.write(black_frame)
        else:
            out.write(img_resized)

    out.release()
    print(f"[Test] Đã tạo video test tại: {output_path} ({duration_sec}s, {total_frames} frames)")


def run_video_detection_test():
    print("\n========================================================")
    print(" KIỂM THỬ ML SERVICE: VIDEO DETECTION & MATCHING")
    print("========================================================")

    # 1. Đăng ký Person A vào FAISS với ID 999
    with open(PERSON_A_IMG, "rb") as f:
        img_bytes = f.read()

    reg_res = client.post(
        "/ml/register-face",
        files={"file": ("person_a.jpg", io.BytesIO(img_bytes), "image/jpeg")},
        data={"vector_id": TEST_VECTOR_ID}
    )
    assert reg_res.status_code == 200, f"Đăng ký thất bại: {reg_res.text}"
    print(f"1. [Register Face] Đã đăng ký Person A vào FAISS với id={TEST_VECTOR_ID}")

    # 2. Tạo video test
    create_test_video(TEST_VIDEO_PATH, duration_sec=3, fps=25)

    # 3. Gọi endpoint detect-and-match-video
    try:
        with open(TEST_VIDEO_PATH, "rb") as f:
            video_bytes = f.read()

        print("2. [POST /ml/detect-and-match-video] Đang gửi video để nhận diện...")
        res = client.post(
            "/ml/detect-and-match-video?threshold=0.45&frame_interval_seconds=1.0",
            files={"file": ("test_sample_video.mp4", io.BytesIO(video_bytes), "video/mp4")}
        )
        assert res.status_code == 200, f"Gọi endpoint thất bại ({res.status_code}): {res.text}"
        data = res.json()

        print("\n--- KẾT QUẢ NHẬN DIỆN VIDEO ---")
        print(f"Thời lượng: {data['duration_seconds']}s")
        print(f"Tổng số khung hình: {data['total_frames']}")
        print(f"Số khung hình đã quét: {data['processed_frames']}")
        print(f"Tổng số khuôn mặt phát hiện: {data['total_faces_detected']}")
        print(f"Trùng khớp: {data['matched']}")
        print(f"Số lượt phát hiện trùng khớp: {data['total_matches']}")
        print(f"Số đối tượng duy nhất: {len(data['unique_persons'])}")

        assert data["matched"] is True, "Kỳ vọng matched == True"
        assert len(data["unique_persons"]) >= 1, "Kỳ vọng ít nhất 1 người được tìm thấy"
        matched_person = data["unique_persons"][0]
        assert matched_person["vector_id"] == TEST_VECTOR_ID
        print(f"Người phát hiện: Vector ID #{matched_person['vector_id']}")
        print(f"Độ tương đồng cao nhất: {matched_person['max_similarity'] * 100:.1f}%")
        print(f"Số lần xuất hiện: {matched_person['occurrences_count']}")
        print(f"Các mốc thời gian xuất hiện: {matched_person['timestamps']}")

        assert len(data["timeline"]) >= 1, "Kỳ vọng timeline có ít nhất 1 sự kiện"
        event = data["timeline"][0]
        print(f"Sự kiện đầu tiên: lúc {event['timestamp_formatted']} ({event['timestamp']}s), Bbox: {event['bbox']}")
        assert event["snapshot_base64"].startswith("data:image/jpeg;base64,"), "Snapshot base64 không hợp lệ"
        print("Snapshot base64: Hợp lệ (data:image/jpeg;base64,...)")

        print("\n>>> KIỂM THỬ VIDEO DETECTION THÀNH CÔNG RỰC RỠ! <<<")

    finally:
        # Dọn dẹp video test và vector test
        if os.path.exists(TEST_VIDEO_PATH):
            os.remove(TEST_VIDEO_PATH)
            print("[Cleanup] Đã xoá file video test.")

        del_res = client.delete(f"/ml/face/{TEST_VECTOR_ID}")
        print(f"[Cleanup] Đã xoá vector test {TEST_VECTOR_ID}: {del_res.status_code}")


if __name__ == "__main__":
    run_video_detection_test()
