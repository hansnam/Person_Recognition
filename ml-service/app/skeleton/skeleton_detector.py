"""
Skeleton / Pose Detection Module sử dụng YOLOv8-pose (yolo26n-pose.pt).
Phát hiện người và trích xuất 17 điểm khớp xương (COCO keypoints).
Hoàn toàn độc lập với module Body Re-ID và Face Detection.
"""

import os
from typing import List, Dict, Any, Optional
import numpy as np
import cv2
from ultralytics import YOLO

DEFAULT_POSE_MODEL_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "weights", "yolo26n-pose.pt"
)

# 17 COCO Keypoints names
KEYPOINT_NAMES = [
    "nose", "left_eye", "right_eye", "left_ear", "right_ear",
    "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
    "left_wrist", "right_wrist", "left_hip", "right_hip",
    "left_knee", "right_knee", "left_ankle", "right_ankle"
]

# Định nghĩa các cạnh nối khung xương (COCO Skeleton connections)
SKELETON_EDGES = [
    (15, 13), (13, 11), (16, 14), (14, 12), (11, 12),  # Thân dưới & hông
    (5, 11), (6, 12), (5, 6),                           # Thân trên & vai
    (5, 7), (7, 9), (6, 8), (8, 10),                    # Cánh tay
    (1, 2), (0, 1), (0, 2), (1, 3), (2, 4), (3, 5), (4, 6) # Vùng đầu & mặt
]


class SkeletonDetector:
    def __init__(self, model_path: str = DEFAULT_POSE_MODEL_PATH):
        self.model_path = model_path
        if not os.path.exists(self.model_path):
            raise FileNotFoundError(f"Không tìm thấy trọng số pose model tại: {self.model_path}")

        print(f"[Skeleton Detector] Nạp mô hình Pose từ: {self.model_path}")
        self.model = YOLO(self.model_path)

    def detect_skeletons(self, image: np.ndarray, conf_threshold: float = 0.35) -> List[Dict[str, Any]]:
        """
        Phát hiện người và trích xuất khung xương từ ảnh.
        :param image: Ảnh numpy array (BGR)
        :param conf_threshold: Ngưỡng tin cậy phát hiện
        :return: Danh sách các đối tượng phát hiện:
                 [
                   {
                     "bbox": [x1, y1, x2, y2],
                     "confidence": float,
                     "keypoints": [
                        {"id": 0, "name": "nose", "x": float, "y": float, "conf": float},
                        ... (17 điểm)
                     ]
                   }, ...
                 ]
        """
        results = self.model.predict(
            source=image,
            conf=conf_threshold,
            classes=[0],  # Chỉ person
            verbose=False
        )

        detected_skeletons = []
        if len(results) == 0:
            return detected_skeletons

        res = results[0]
        if res.boxes is None or len(res.boxes) == 0:
            return detected_skeletons

        boxes = res.boxes.xyxy.cpu().numpy().astype(int)
        confidences = res.boxes.conf.cpu().numpy().astype(float)

        # Trích xuất keypoints nếu có
        kpts_data = None
        if hasattr(res, "keypoints") and res.keypoints is not None:
            # shape: (N, 17, 3) -> [x, y, conf] hoặc (N, 17, 2)
            kpts_data = res.keypoints.data.cpu().numpy()

        for idx, box in enumerate(boxes):
            bbox = [int(box[0]), int(box[1]), int(box[2]), int(box[3])]
            conf = float(confidences[idx])

            keypoints_list = []
            if kpts_data is not None and idx < len(kpts_data):
                kpts = kpts_data[idx]
                for kp_idx, pt in enumerate(kpts):
                    name = KEYPOINT_NAMES[kp_idx] if kp_idx < len(KEYPOINT_NAMES) else f"pt_{kp_idx}"
                    x = float(pt[0])
                    y = float(pt[1])
                    kp_conf = float(pt[2]) if len(pt) > 2 else 1.0

                    keypoints_list.append({
                        "id": kp_idx,
                        "name": name,
                        "x": round(x, 2),
                        "y": round(y, 2),
                        "conf": round(kp_conf, 3)
                    })

            detected_skeletons.append({
                "bbox": bbox,
                "confidence": round(conf, 4),
                "keypoints": keypoints_list
            })

        return detected_skeletons

    def draw_skeleton(self, image: np.ndarray, detected_skeletons: List[Dict[str, Any]], kpt_conf_thresh: float = 0.4) -> np.ndarray:
        """
        Vẽ khung xương và bounding box lên bản sao của ảnh để hiển thị trực quan.
        """
        canvas = image.copy()

        for skeleton in detected_skeletons:
            bbox = skeleton["bbox"]
            cv2.rectangle(canvas, (bbox[0], bbox[1]), (bbox[2], bbox[3]), (0, 255, 0), 2)
            cv2.putText(
                canvas, f"Person {skeleton['confidence']:.2f}",
                (bbox[0], max(15, bbox[1] - 8)),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2
            )

            kpts = skeleton.get("keypoints", [])
            if not kpts:
                continue

            # Vẽ các đường nối xương (Edges)
            for p1_idx, p2_idx in SKELETON_EDGES:
                if p1_idx < len(kpts) and p2_idx < len(kpts):
                    kp1 = kpts[p1_idx]
                    kp2 = kpts[p2_idx]
                    if kp1["conf"] >= kpt_conf_thresh and kp2["conf"] >= kpt_conf_thresh:
                        pt1 = (int(kp1["x"]), int(kp1["y"]))
                        pt2 = (int(kp2["x"]), int(kp2["y"]))
                        cv2.line(canvas, pt1, pt2, (255, 128, 0), 2)

            # Vẽ các khớp điểm (Joints)
            for kp in kpts:
                if kp["conf"] >= kpt_conf_thresh:
                    pt = (int(kp["x"]), int(kp["y"]))
                    cv2.circle(canvas, pt, 4, (0, 0, 255), -1)

        return canvas


# Singleton accessor
_skeleton_detector_instance: Optional[SkeletonDetector] = None


def get_skeleton_detector() -> SkeletonDetector:
    global _skeleton_detector_instance
    if _skeleton_detector_instance is None:
        _skeleton_detector_instance = SkeletonDetector()
    return _skeleton_detector_instance

