"""
API Router cho Skeleton / Pose Detection.
Prefix: /ml/skeleton
Cung cấp các API độc lập:
- POST /ml/skeleton/detect: Trích xuất 17 keypoints, góc quay và tỉ lệ nhân trắc học
- POST /ml/skeleton/visualize: Trả về ảnh đã vẽ khung xương skeleton dưới dạng base64
"""

import base64
import cv2
import numpy as np
from fastapi import APIRouter, File, UploadFile, HTTPException, Query
from fastapi.responses import JSONResponse

from .skeleton_detector import get_skeleton_detector
from .feature_extractor import analyze_skeleton_features

router = APIRouter(prefix="/ml/skeleton", tags=["Skeleton & Pose Detection"])


def _read_image(upload_file: UploadFile) -> np.ndarray:
    try:
        contents = upload_file.file.read()
        nparr = np.frombuffer(contents, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            raise ValueError()
        return img
    except Exception:
        raise HTTPException(status_code=400, detail="Tệp tải lên không phải là ảnh hợp lệ")


@router.post("/detect")
async def detect_skeleton_api(
    file: UploadFile = File(...),
    conf_threshold: float = Query(0.35, ge=0.1, le=1.0, description="Ngưỡng tin cậy phát hiện dáng người")
):
    """
    Phát hiện người, trích xuất 17 điểm khớp xương (Keypoints) và các đặc tính nhân trắc học.
    """
    img = _read_image(file)
    detector = get_skeleton_detector()

    skeletons = detector.detect_skeletons(img, conf_threshold=conf_threshold)

    results = []
    for sk in skeletons:
        kpts = sk.get("keypoints", [])
        analysis = analyze_skeleton_features(kpts)

        results.append({
            "bbox": sk["bbox"],
            "confidence": sk["confidence"],
            "keypoints": kpts,
            "analysis": analysis
        })

    return {
        "status": "success",
        "total_persons_detected": len(results),
        "results": results
    }


@router.post("/visualize")
async def visualize_skeleton_api(
    file: UploadFile = File(...),
    conf_threshold: float = Query(0.35, ge=0.1, le=1.0)
):
    """
    Nhận ảnh đầu vào và trả về ảnh đã vẽ khung xương (Skeleton) dưới dạng chuỗi base64.
    """
    img = _read_image(file)
    detector = get_skeleton_detector()

    skeletons = detector.detect_skeletons(img, conf_threshold=conf_threshold)
    canvas = detector.draw_skeleton(img, skeletons)

    # Encode sang JPEG base64
    _, buffer = cv2.imencode(".jpg", canvas)
    base64_img = base64.b64encode(buffer).decode("utf-8")

    return {
        "status": "success",
        "total_persons_detected": len(skeletons),
        "image_base64": f"data:image/jpeg;base64,{base64_img}"
    }

