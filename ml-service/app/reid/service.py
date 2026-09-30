"""
Body Re-ID Service.
Điều phối toàn bộ quy trình: Phát hiện dáng người -> Trích xuất đặc trưng Re-ID -> Tìm kiếm trong FAISS.
"""

from typing import List, Dict, Any
import numpy as np

from .body_detector import YOLOv8BodyDetector
from .embedder import get_body_embedder
from .vector_store_body import body_vector_store

_detector = None


def get_body_detector() -> YOLOv8BodyDetector:
    global _detector
    if _detector is None:
        _detector = YOLOv8BodyDetector()
    return _detector


def run_body_pipeline(image: np.ndarray, body_threshold: float = 0.65) -> List[Dict[str, Any]]:
    """
    Chạy pipeline Body Re-ID độc lập trên ảnh/khung hình:
    1. Phát hiện tất cả người trong khung hình bằng YOLOv8n
    2. Với mỗi người: Cắt ảnh -> Resize 256x128 -> Trích xuất vector 2048 chiều bằng ResNet-50 CUHK03
    3. Tìm kiếm vector gần nhất trong Body FAISS
    4. Trả về danh sách kết quả đối soát body
    """
    detector = get_body_detector()
    embedder = get_body_embedder()

    detected_bodies = detector.detect_bodies(image, conf_threshold=0.4)
    results = []

    for body in detected_bodies:
        bbox = body["bbox"]
        try:
            emb = embedder.extract_embedding(image, bbox=bbox)
            search_res = body_vector_store.search(emb, top_k=1, threshold=body_threshold)

            is_match = search_res["matched"]
            results.append({
                "bbox": bbox,
                "confidence": body["confidence"],
                "matched": is_match,
                "person_id": search_res["vector_id"] if is_match else None,
                "similarity": search_res["similarity"]
            })
        except Exception as e:
            print(f"[Body Re-ID] Lỗi khi xử lý crop {bbox}: {e}")
            results.append({
                "bbox": bbox,
                "confidence": body["confidence"],
                "matched": False,
                "person_id": None,
                "similarity": 0.0
            })

    return results
