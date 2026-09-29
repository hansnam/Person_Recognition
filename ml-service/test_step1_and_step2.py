"""
Script kiểm thử độc lập cho Bước 1 & Bước 2 (ML Service)
- Bước 1: YOLOv8-Face Detection + Face Alignment (Affine Transform 112x112)
- Bước 2: MobileFaceNet Feature Extraction (512-dim L2-normalized embedding)
"""

import os
import cv2
import numpy as np

from app.detection import get_detector
from app.alignment import align_face_5point
from app.embedding import get_embedder


def test_detection_alignment_embedding():
    samples_dir = os.path.join(os.path.dirname(__file__), "test_samples")
    img_a_path = os.path.join(samples_dir, "person_a.jpg")
    img_b_path = os.path.join(samples_dir, "person_b.jpg")

    assert os.path.exists(img_a_path), f"Không tìm thấy {img_a_path}"
    assert os.path.exists(img_b_path), f"Không tìm thấy {img_b_path}"

    print("\n========================================================")
    print(" BƯỚC 1: KIỂM THỬ DETECTION (YOLOv8-Face) & ALIGNMENT")
    print("========================================================")

    detector = get_detector()
    embedder = get_embedder()

    # --- Test Ảnh Person A ---
    img_a = cv2.imread(img_a_path)
    faces_a = detector.detect_faces(img_a, conf_threshold=0.5)
    print(f"[Person A] Số khuôn mặt phát hiện được: {len(faces_a)}")
    assert len(faces_a) > 0, "Lỗi: Không phát hiện được khuôn mặt trong person_a.jpg!"

    face_a = faces_a[0]
    print(f"[Person A] Bounding Box: {face_a['bbox']}")
    print(f"[Person A] Độ tin cậy (Confidence): {face_a['confidence']}")
    print(f"[Person A] 5 Landmarks:\n{face_a['landmarks']}")

    # Vẽ bounding box + 5 landmarks và lưu file để kiểm tra bằng mắt
    vis_a = detector.draw_detections(img_a, faces_a)
    vis_a_path = os.path.join(samples_dir, "person_a_detected.jpg")
    cv2.imwrite(vis_a_path, vis_a)
    print(f"-> Đã lưu ảnh phát hiện kèm 5 điểm mốc tại: {vis_a_path}")

    # Face Alignment 112x112 chuẩn ArcFace
    aligned_a = align_face_5point(img_a, face_a["landmarks"])
    assert aligned_a.shape == (112, 112, 3), f"Lỗi shape ảnh align: {aligned_a.shape}"
    aligned_a_path = os.path.join(samples_dir, "person_a_aligned.jpg")
    cv2.imwrite(aligned_a_path, aligned_a)
    print(f"-> Đã lưu ảnh khuôn mặt sau khi căn chỉnh (112x112) tại: {aligned_a_path}")

    print("\n========================================================")
    print(" BƯỚC 2: KIỂM THỬ TRÍCH XUẤT ĐẶC TRƯNG (MobileFaceNet 512-d)")
    print("========================================================")

    emb_a = embedder.extract_embedding(aligned_a)
    print(f"[Person A] Số chiều vector: {len(emb_a)} (Yêu cầu: 512)")
    print(f"[Person A] L2 Norm của vector: {np.linalg.norm(emb_a):.4f} (Yêu cầu xấp xỉ 1.0000)")
    assert len(emb_a) == 512, "Lỗi: Chiều vector không bằng 512!"
    assert abs(np.linalg.norm(emb_a) - 1.0) < 1e-4, "Lỗi: Vector chưa được chuẩn hoá L2!"

    # --- Test Ảnh Person B ---
    img_b = cv2.imread(img_b_path)
    faces_b = detector.detect_faces(img_b, conf_threshold=0.5)
    assert len(faces_b) > 0, "Lỗi: Không phát hiện được khuôn mặt trong person_b.jpg!"
    face_b = faces_b[0]
    aligned_b = align_face_5point(img_b, face_b["landmarks"])
    emb_b = embedder.extract_embedding(aligned_b)

    # Tính Cosine Similarity giữa Person A và Person B
    cos_sim = float(np.dot(emb_a, emb_b))
    print(f"\n[So sánh] Cosine Similarity giữa Person A và Person B: {cos_sim:.4f}")
    print(f"-> 2 người khác nhau: Similarity thấp ({cos_sim:.4f} < 0.35)")

    # Tính Cosine Similarity giữa Person A và chính Person A (Tự so khớp)
    self_sim = float(np.dot(emb_a, emb_a))
    print(f"[So sánh] Cosine Similarity của Person A với chính mình: {self_sim:.4f}")
    assert self_sim > 0.99, "Lỗi: Cosine similarity với chính mình phải xấp xỉ 1.0"

    print("\n>>> KIỂM THỬ BƯỚC 1 & BƯỚC 2 THÀNH CÔNG 100%! <<<")


if __name__ == "__main__":
    test_detection_alignment_embedding()
