"""
Person Body Detection Module sử dụng YOLOv8n (Ultralytics).
Phát hiện người (class 0: person) và trích xuất Bounding Box toàn thân [x1, y1, x2, y2].
"""

import os
from typing import List, Dict, Any
import numpy as np
from ultralytics import YOLO

DEFAULT_BODY_MODEL_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "weights", "yolov8n.pt"
)


class YOLOv8BodyDetector:
    def __init__(self, model_path: str = DEFAULT_BODY_MODEL_PATH):
        self.model_path = model_path
        if not os.path.exists(self.model_path):
            raise FileNotFoundError(f"Không tìm thấy trọng số YOLOv8 person detector tại: {self.model_path}")

        print(f"[Body Detector] Nạp mô hình YOLOv8n person detector từ: {self.model_path}")
        self.model = YOLO(self.model_path)

    def detect_bodies(self, image: np.ndarray, conf_threshold: float = 0.4) -> List[Dict[str, Any]]:
        """
        Phát hiện người trong ảnh (class 0 = person).
        :param image: Ảnh đầu vào dạng numpy array (BGR)
        :param conf_threshold: Ngưỡng tin cậy phát hiện
        :return: Danh sách các body phát hiện được:
                 [
                   {
                     "bbox": [x1, y1, x2, y2],
                     "confidence": float
                   }, ...
                 ]
        """
        results = self.model.predict(
            source=image,
            conf=conf_threshold,
            classes=[0],  # Chỉ detect class 0: person
            verbose=False
        )

        detected_bodies = []
        if len(results) == 0:
            return detected_bodies

        res = results[0]
        if res.boxes is None or len(res.boxes) == 0:
            return detected_bodies

        boxes = res.boxes.xyxy.cpu().numpy()
        confs = res.boxes.conf.cpu().numpy()

        h, w = image.shape[:2]

        for i in range(len(boxes)):
            box = boxes[i]
            x1, y1, x2, y2 = [int(round(coord)) for coord in box]
            x1, y1 = max(0, x1), max(0, y1)
            x2, y2 = min(w, x2), min(h, y2)

            bw = x2 - x1
            bh = y2 - y1

            # Bỏ qua các box quá nhỏ (nhiễu)
            if bw < 10 or bh < 20:
                continue

            conf = float(confs[i])
            detected_bodies.append({
                "bbox": [x1, y1, x2, y2],
                "confidence": round(conf, 4)
            })

        return detected_bodies
