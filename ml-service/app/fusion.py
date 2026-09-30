"""
Fusion Module: Kết hợp kết quả giữa Face Recognition và Body Person Re-ID.
- Gắn kết Face và Body (Containment Check & Center Distance Association)
- Suy luận trạng thái Fusion (CONFIRMED, FACE_MATCH_BODY_MISMATCH, FACE_CANDIDATE, BODY_CANDIDATE, UNKNOWN)
- Tuân thủ nguyên tắc: Face Recognition có độ ưu tiên cao nhất, Re-ID bổ trợ.
"""

import math
from typing import List, Dict, Any, Optional


def is_face_inside_body(face_bbox: List[int], body_bbox: List[int], margin_ratio: float = 0.1) -> bool:
    """
    Kiểm tra khuôn mặt có nằm trong vùng thân người hay không.
    Hỗ trợ một biên độ dung sai margin nhỏ (mặc định 10% kích thước face) để tránh sai số bbox.
    """
    fx1, fy1, fx2, fy2 = face_bbox
    bx1, by1, bx2, by2 = body_bbox

    fw = fx2 - fx1
    fh = fy2 - fy1

    # Kiểm tra tâm khuôn mặt có nằm trong body hay không
    fcx = (fx1 + fx2) / 2.0
    fcy = (fy1 + fy2) / 2.0

    if bx1 <= fcx <= bx2 and by1 <= fcy <= by2:
        return True

    # Kiểm tra containment với margin
    margin_x = fw * margin_ratio
    margin_y = fh * margin_ratio

    return (fx1 >= bx1 - margin_x and fy1 >= by1 - margin_y and
            fx2 <= bx2 + margin_x and fy2 <= by2 + margin_y)


def center_distance(bbox1: List[int], bbox2: List[int]) -> float:
    """Tính khoảng cách Euclid giữa tâm của hai bounding box"""
    cx1 = (bbox1[0] + bbox1[2]) / 2.0
    cy1 = (bbox1[1] + bbox1[3]) / 2.0
    cx2 = (bbox2[0] + bbox2[2]) / 2.0
    cy2 = (bbox2[1] + bbox2[3]) / 2.0
    return math.sqrt((cx1 - cx2) ** 2 + (cy1 - cy2) ** 2)


def fuse_results(
    face_matched: bool,
    face_person_id: Optional[int],
    body_matched: bool,
    body_person_id: Optional[int]
) -> Dict[str, Any]:
    """
    Kết hợp kết quả giữa Face Recognition và Body Re-ID.
    NGUYÊN TẮC: Face Recognition được ưu tiên tuyệt đối. Re-ID chỉ bổ trợ.
    Không hủy nhận diện khi mâu thuẫn body, mà ưu tiên Face và đính kèm cảnh báo nghi vấn.
    """
    # 1. Face nhận diện thành công (ưu tiên tuyệt đối)
    if face_matched and face_person_id is not None:
        if body_matched and body_person_id is not None:
            if face_person_id == body_person_id:
                return {
                    "status": "CONFIRMED",
                    "person_id": face_person_id,
                    "body_warning": False,
                    "message": f"Xác nhận trùng khớp cả khuôn mặt và dáng người (Hồ sơ #{face_person_id})"
                }
            else:
                # Face khớp P1 nhưng Body khớp P2 -> Ưu tiên Face (P1), cảnh báo nghi vấn Body (P2)
                return {
                    "status": "FACE_MATCH_BODY_MISMATCH",
                    "person_id": face_person_id,
                    "body_warning": True,
                    "message": (
                        f"Nhận dạng thành công theo khuôn mặt (Hồ sơ #{face_person_id}). "
                        f"CẢNH BÁO NGHI VẤN: Dáng người/trang phục khớp với hồ sơ #{body_person_id} "
                        f"(nghi vấn đối tượng đã thay đổi trang phục hoặc đi cùng người khác)."
                    )
                }
        else:
            return {
                "status": "FACE_CANDIDATE",
                "person_id": face_person_id,
                "body_warning": False,
                "message": f"Nhận dạng theo khuôn mặt (Hồ sơ #{face_person_id}). Dáng người chưa được xác nhận."
            }

    # 2. Không nhận diện được mặt (quay lưng/che mặt), chỉ nhận diện theo body
    elif body_matched and body_person_id is not None:
        return {
            "status": "BODY_CANDIDATE",
            "person_id": body_person_id,
            "body_warning": False,
            "message": f"Phát hiện nghi vấn qua dáng người/trang phục (Hồ sơ #{body_person_id}). Chưa xác nhận được khuôn mặt."
        }

    # 3. Không nhận diện được ai
    else:
        return {
            "status": "UNKNOWN",
            "person_id": None,
            "body_warning": False,
            "message": "Không nhận diện được đối tượng."
        }


def associate_face_body(
    face_results: List[Dict[str, Any]],
    body_results: List[Dict[str, Any]]
) -> List[Dict[str, Any]]:
    """
    Ghép đôi Face và Body trong một khung hình:
    - Nếu Face nằm trong Body: Ghép lại thành 1 detection với outer bbox = body bbox
    - Nếu Face không nằm trong Body nào: Detection chỉ có face (face-only)
    - Nếu Body không chứa Face nào: Detection chỉ có body (body-only)
    """
    detections = []
    used_body_indices = set()
    detection_id_counter = 0

    # Ghép face với body phù hợp nhất
    for face in face_results:
        f_bbox = face["bbox"]
        f_matched = face.get("matched", False)
        f_pid = face.get("person_id") if "person_id" in face else face.get("vector_id")
        f_sim = face.get("similarity", 0.0)

        face_info = {
            "matched": f_matched,
            "person_id": f_pid,
            "similarity": f_sim,
            "bbox": f_bbox
        }

        best_body_idx = None
        min_dist = float("inf")

        for idx, body in enumerate(body_results):
            if idx in used_body_indices:
                continue

            b_bbox = body["bbox"]
            if is_face_inside_body(f_bbox, b_bbox):
                dist = center_distance(f_bbox, b_bbox)
                if dist < min_dist:
                    min_dist = dist
                    best_body_idx = idx

        if best_body_idx is not None:
            used_body_indices.add(best_body_idx)
            matched_body = body_results[best_body_idx]

            b_matched = matched_body.get("matched", False)
            b_pid = matched_body.get("person_id")
            b_sim = matched_body.get("similarity", 0.0)

            body_info = {
                "matched": b_matched,
                "person_id": b_pid,
                "similarity": b_sim,
                "bbox": matched_body["bbox"]
            }

            fusion_info = fuse_results(f_matched, f_pid, b_matched, b_pid)

            detections.append({
                "detection_id": detection_id_counter,
                "bbox": matched_body["bbox"],  # outer bbox là body bbox
                "face": face_info,
                "body": body_info,
                "fusion": fusion_info
            })
        else:
            # Face đứng đơn lẻ (không detect được body)
            fusion_info = fuse_results(f_matched, f_pid, False, None)
            detections.append({
                "detection_id": detection_id_counter,
                "bbox": f_bbox,
                "face": face_info,
                "body": None,
                "fusion": fusion_info
            })

        detection_id_counter += 1

    # Thêm các body còn lại chưa được ghép với face nào (quay lưng / che mặt)
    for idx, body in enumerate(body_results):
        if idx not in used_body_indices:
            b_matched = body.get("matched", False)
            b_pid = body.get("person_id")
            b_sim = body.get("similarity", 0.0)

            body_info = {
                "matched": b_matched,
                "person_id": b_pid,
                "similarity": b_sim,
                "bbox": body["bbox"]
            }

            fusion_info = fuse_results(False, None, b_matched, b_pid)

            detections.append({
                "detection_id": detection_id_counter,
                "bbox": body["bbox"],
                "face": None,
                "body": body_info,
                "fusion": fusion_info
            })
            detection_id_counter += 1

    return detections
