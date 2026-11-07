"""
Trích xuất đặc trưng hình học & nhân trắc học từ khung xương (Anthropometric Features).
Giúp khắc phục triệt để hạn chế thay đổi trang phục và xác định mức độ che khuất (Occlusion) của Re-ID.
"""

import math
from typing import List, Dict, Any, Tuple


def calculate_distance(pt1: Dict[str, Any], pt2: Dict[str, Any]) -> float:
    """Tính khoảng cách Euclid giữa 2 điểm keypoint."""
    return math.sqrt((pt1["x"] - pt2["x"]) ** 2 + (pt1["y"] - pt2["y"]) ** 2)


def analyze_skeleton_features(keypoints: List[Dict[str, Any]], min_conf: float = 0.3) -> Dict[str, Any]:
    """
    Phân tích khung xương để trích xuất:
    1. Tỉ lệ nhân trắc học (Anthropometric Ratios): Bất biến với màu sắc áo quần
    2. Hướng quay cơ thể (Pose Orientation): FRONT, BACK, LEFT, RIGHT
    3. Đánh giá mức độ che khuất (Occlusion Status): Thân trên / Thân dưới
    """
    if len(keypoints) < 17:
        return {"valid": False, "message": "Không đủ 17 keypoints"}

    kpts_by_name = {kp["name"]: kp for kp in keypoints}

    # Lấy các điểm chính
    nose = kpts_by_name.get("nose")
    l_eye = kpts_by_name.get("left_eye")
    r_eye = kpts_by_name.get("right_eye")
    l_ear = kpts_by_name.get("left_ear")
    r_ear = kpts_by_name.get("right_ear")

    l_shoulder = kpts_by_name.get("left_shoulder")
    r_shoulder = kpts_by_name.get("right_shoulder")
    l_hip = kpts_by_name.get("left_hip")
    r_hip = kpts_by_name.get("right_hip")

    l_knee = kpts_by_name.get("left_knee")
    r_knee = kpts_by_name.get("right_knee")
    l_ankle = kpts_by_name.get("left_ankle")
    r_ankle = kpts_by_name.get("right_ankle")

    # --- 1. Đánh giá mức độ che khuất (Occlusion) ---
    upper_body_pts = [l_shoulder, r_shoulder, l_hip, r_hip]
    lower_body_pts = [l_knee, r_knee, l_ankle, r_ankle]

    upper_visible = sum(1 for p in upper_body_pts if p and p["conf"] >= min_conf)
    lower_visible = sum(1 for p in lower_body_pts if p and p["conf"] >= min_conf)

    is_upper_occluded = upper_visible < 2
    is_lower_occluded = lower_visible < 2

    # --- 2. Xác định hướng cơ thể (Orientation) ---
    orientation = "UNKNOWN"
    if nose and l_ear and r_ear:
        if nose["conf"] >= min_conf:
            if l_ear["conf"] >= min_conf and r_ear["conf"] >= min_conf:
                orientation = "FRONT"
            elif l_ear["conf"] >= min_conf and r_ear["conf"] < min_conf:
                orientation = "SIDE_LEFT"
            elif r_ear["conf"] >= min_conf and l_ear["conf"] < min_conf:
                orientation = "SIDE_RIGHT"
            else:
                orientation = "FRONT"
        else:
            # Mũi không thấy hoặc conf thấp -> Khả năng cao quay lưng
            if (l_ear["conf"] >= min_conf or r_ear["conf"] >= min_conf) and (l_shoulder["conf"] >= min_conf or r_shoulder["conf"] >= min_conf):
                orientation = "BACK"

    # --- 3. Tính toán các tỉ lệ nhân trắc (Anthropometric Ratios) ---
    ratios = {}
    
    # Khoảng cách vai (Shoulder width)
    shoulder_width = None
    if l_shoulder["conf"] >= min_conf and r_shoulder["conf"] >= min_conf:
        shoulder_width = calculate_distance(l_shoulder, r_shoulder)

    # Chiều dài thân (Torso length): trung điểm vai -> trung điểm hông
    torso_length = None
    if (l_shoulder["conf"] >= min_conf and r_shoulder["conf"] >= min_conf and
            l_hip["conf"] >= min_conf and r_hip["conf"] >= min_conf):
        mid_shoulder = {"x": (l_shoulder["x"] + r_shoulder["x"]) / 2, "y": (l_shoulder["y"] + r_shoulder["y"]) / 2}
        mid_hip = {"x": (l_hip["x"] + r_hip["x"]) / 2, "y": (l_hip["y"] + r_hip["y"]) / 2}
        torso_length = calculate_distance(mid_shoulder, mid_hip)

    # Chiều dài chân (Leg length): trung bình chiều dài hông -> mắt cá chân
    leg_length = None
    leg_lengths = []
    if l_hip["conf"] >= min_conf and l_ankle["conf"] >= min_conf:
        leg_lengths.append(calculate_distance(l_hip, l_ankle))
    if r_hip["conf"] >= min_conf and r_ankle["conf"] >= min_conf:
        leg_lengths.append(calculate_distance(r_hip, r_ankle))
    if leg_lengths:
        leg_length = sum(leg_lengths) / len(leg_lengths)

    # Tỉ lệ vai / thân
    if shoulder_width and torso_length and torso_length > 0:
        ratios["shoulder_to_torso_ratio"] = round(shoulder_width / torso_length, 3)

    # Tỉ lệ thân / chân
    if torso_length and leg_length and leg_length > 0:
        ratios["torso_to_leg_ratio"] = round(torso_length / leg_length, 3)

    return {
        "valid": True,
        "orientation": orientation,
        "occlusion": {
            "upper_body_occluded": is_upper_occluded,
            "lower_body_occluded": is_lower_occluded,
            "upper_visible_count": upper_visible,
            "lower_visible_count": lower_visible
        },
        "measurements": {
            "shoulder_width": round(shoulder_width, 2) if shoulder_width else None,
            "torso_length": round(torso_length, 2) if torso_length else None,
            "leg_length": round(leg_length, 2) if leg_length else None,
        },
        "anthropometric_ratios": ratios
    }

