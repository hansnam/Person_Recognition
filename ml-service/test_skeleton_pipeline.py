"""
Script kiểm thử độc lập module Skeleton / Pose (yolo26n-pose.pt).
Chạy trực tiếp trên ảnh mẫu trong test_samples để xác thực mà không cần bật server.
"""

import os
import cv2
import json

from app.skeleton.skeleton_detector import get_skeleton_detector
from app.skeleton.feature_extractor import analyze_skeleton_features

def test_pipeline():
    sample_path = os.path.join(os.path.dirname(__file__), "test_samples", "person_a.jpg")
    output_path = os.path.join(os.path.dirname(__file__), "test_samples", "person_a_skeleton.jpg")

    if not os.path.exists(sample_path):
        print(f"[!] Không tìm thấy ảnh mẫu: {sample_path}")
        return

    print(f"[*] Đang đọc ảnh: {sample_path}")
    image = cv2.imread(sample_path)

    detector = get_skeleton_detector()
    skeletons = detector.detect_skeletons(image, conf_threshold=0.35)

    print(f"[+] Số người phát hiện: {len(skeletons)}")

    for idx, sk in enumerate(skeletons):
        print(f"\n--- Người #{idx + 1} ---")
        print(f"Bbox: {sk['bbox']}, Độ tin cậy: {sk['confidence']}")
        print(f"Tổng số keypoints: {len(sk['keypoints'])}")

        analysis = analyze_skeleton_features(sk['keypoints'])
        print("Kết quả phân tích hình học & góc quay:")
        print(json.dumps(analysis, indent=2, ensure_ascii=False))

    # Vẽ khung xương và lưu ảnh
    canvas = detector.draw_skeleton(image, skeletons)
    cv2.imwrite(output_path, canvas)
    print(f"\n[+] Đã lưu ảnh kết quả khung xương tại: {output_path}")

if __name__ == "__main__":
    test_pipeline()

