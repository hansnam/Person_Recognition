"""
Face Detection Module sử dụng YOLOv8-Face (Ultralytics)
Trích xuất Bounding Box (hộp bao) và 5 điểm mốc khuôn mặt (5 facial landmarks):
- Mắt trái (Left Eye)
- Mắt phải (Right Eye)
- Mũi (Nose)
- Khóe miệng trái (Left Mouth)
- Khóe miệng phải (Right Mouth)
"""

import os
from typing import List, Dict, Any
import cv2
import numpy as np
from ultralytics import YOLO

DEFAULT_MODEL_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "weights", "yolov8n-face.pt")


class YOLOv8FaceDetector:
    def __init__(self, model_path: str = DEFAULT_MODEL_PATH):
        self.model_path = model_path
        if not os.path.exists(self.model_path):
            raise FileNotFoundError(f"Không tìm thấy trọng số YOLOv8-Face tại: {self.model_path}")

        print(f"[Detector] Nạp mô hình YOLOv8-Face từ: {self.model_path}")
        self.model = YOLO(self.model_path)

    def detect_faces(self, image: np.ndarray, conf_threshold: float = 0.5) -> List[Dict[str, Any]]:
        """
        Phát hiện các khuôn mặt trong ảnh kèm 5 điểm mốc.
        :param image: Ảnh đầu vào dạng numpy array (BGR)
        :param conf_threshold: Ngưỡng tin cậy tối thiểu của bounding box
        :return: Danh sách các khuôn mặt phát hiện được:
                 [
                   {
                     "bbox": [x1, y1, x2, y2],
                     "confidence": float,
                     "landmarks": np.ndarray (5, 2)
                   }, ...
                 ]
        """
        results = self.model.predict(
            source=image,
            conf=conf_threshold,
            verbose=False
        )

        detected_faces = []
        if len(results) == 0:
            return detected_faces

        res = results[0]
        if res.boxes is None or len(res.boxes) == 0:
            return detected_faces

        boxes = res.boxes.xyxy.cpu().numpy()  # (N, 4)
        confs = res.boxes.conf.cpu().numpy()  # (N,)

        # Trích xuất keypoints (N, 5, 2)
        if res.keypoints is not None and res.keypoints.xy is not None:
            keypoints = res.keypoints.xy.cpu().numpy()
        else:
            keypoints = None

        h, w = image.shape[:2]

        for i in range(len(boxes)):
            box = boxes[i]
            x1, y1, x2, y2 = [int(round(coord)) for coord in box]
            x1, y1 = max(0, x1), max(0, y1)
            x2, y2 = min(w, x2), min(h, y2)
            conf = float(confs[i])

            if keypoints is not None and i < len(keypoints):
                kpts = keypoints[i].astype(np.float32)  # (5, 2)
            else:
                # Fallback ước lượng 5 điểm nếu model không xuất keypoint
                bw = x2 - x1
                bh = y2 - y1
                kpts = np.array([
                    [x1 + bw * 0.3, y1 + bh * 0.38],
                    [x1 + bw * 0.7, y1 + bh * 0.38],
                    [x1 + bw * 0.5, y1 + bh * 0.55],
                    [x1 + bw * 0.35, y1 + bh * 0.75],
                    [x1 + bw * 0.65, y1 + bh * 0.75]
                ], dtype=np.float32)

            detected_faces.append({
                "bbox": [x1, y1, x2, y2],
                "confidence": round(conf, 4),
                "landmarks": kpts
            })

        return detected_faces

    def draw_detections(self, image: np.ndarray, detected_faces: List[Dict[str, Any]]) -> np.ndarray:
        """
        Vẽ Bounding box và 5 Landmark lên ảnh phục vụ kiểm tra trực quan.
        """
        vis_img = image.copy()
        landmark_colors = [
            (0, 0, 255),    # Mắt trái: Đỏ
            (0, 255, 0),    # Mắt phải: Xanh lá
            (255, 0, 0),    # Mũi: Xanh dương
            (0, 255, 255),  # Khóe miệng trái: Vàng
            (255, 0, 255)   # Khóe miệng phải: Tím
        ]

        for face in detected_faces:
            x1, y1, x2, y2 = face["bbox"]
            conf = face["confidence"]

            # Bounding box
            cv2.rectangle(vis_img, (x1, y1), (x2, y2), (0, 255, 0), 2)
            label = f"Face: {conf:.2f}"
            cv2.putText(vis_img, label, (x1, max(y1 - 10, 15)), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

            # 5 Landmarks
            landmarks = face["landmarks"]
            for idx, pt in enumerate(landmarks):
                px, py = int(round(pt[0])), int(round(pt[1]))
                color = landmark_colors[idx % len(landmark_colors)]
                cv2.circle(vis_img, (px, py), 4, color, -1)

        return vis_img


# Singleton instance
detector = None

def get_detector() -> YOLOv8FaceDetector:
    global detector
    if detector is None:
        detector = YOLOv8FaceDetector()
    return detector
