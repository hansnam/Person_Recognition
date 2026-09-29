"""
ML Service FastAPI Application
Cung cấp các API nội bộ phục vụ Core Service (Spring Boot):
- POST /ml/register-face: Nhận ảnh chân dung -> Detect -> Align -> Embed -> Lưu FAISS -> Trả về {vector_id, embedding_dim}
- POST /ml/detect-and-match: Nhận ảnh giám sát -> Detect -> Align -> Embed -> Tìm FAISS -> Trả về {matched, vector_id, similarity, bbox}
- DELETE /ml/face/{vector_id}: Xoá vector khỏi FAISS
- GET /ml/health: Kiểm tra trạng thái service và số lượng vector trong FAISS
"""

import os
import io
import tempfile
import shutil
import base64
from typing import Optional, List, Dict, Any
import cv2
import numpy as np
from fastapi import FastAPI, File, UploadFile, Form, HTTPException, Query, status
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from app.detection import get_detector
from app.alignment import align_face_5point
from app.embedding import get_embedder
from app.vector_store import vector_store

app = FastAPI(
    title="Missing Persons ML Service",
    description="Microservice Python thực hiện phát hiện (YOLOv8-Face), căn chỉnh (ArcFace 112x112), trích xuất đặc trưng (MobileFaceNet 512-d) và tìm kiếm vector (FAISS IndexFlatIP)",
    version="1.0.0"
)

# Cho phép Core Service gọi vào
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def read_image_from_upload(upload_file: UploadFile) -> np.ndarray:
    """Đọc dữ liệu file upload thành ảnh OpenCV (BGR)"""
    contents = upload_file.file.read()
    nparr = np.frombuffer(contents, np.uint8)
    image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if image is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="File ảnh không hợp lệ hoặc bị lỗi định dạng."
        )
    return image


def encode_image_to_base64(image: np.ndarray, quality: int = 80) -> str:
    """Mã hoá ảnh OpenCV thành chuỗi Base64 Data URL"""
    encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), quality]
    success, buffer = cv2.imencode('.jpg', image, encode_param)
    if not success:
        return ""
    b64_str = base64.b64encode(buffer).decode('utf-8')
    return f"data:image/jpeg;base64,{b64_str}"


def annotate_frame(image: np.ndarray, bbox: list, label: str, similarity: float) -> np.ndarray:
    """Vẽ bounding box và nhãn cảnh báo lên khung hình snapshot"""
    annotated = image.copy()
    x1, y1, x2, y2 = bbox
    # Khung đỏ đậm
    cv2.rectangle(annotated, (x1, y1), (x2, y2), (0, 0, 238), 2)
    # Nhãn trên hộp
    text = f"{label} ({similarity * 100:.1f}%)"
    font = cv2.FONT_HERSHEY_SIMPLEX
    font_scale = 0.55
    thickness = 1
    (tw, th), baseline = cv2.getTextSize(text, font, font_scale, thickness)
    badge_y1 = max(0, y1 - th - 8)
    cv2.rectangle(annotated, (x1, badge_y1), (x1 + tw + 10, y1), (0, 0, 200), -1)
    cv2.putText(annotated, text, (x1 + 5, y1 - 4), font, font_scale, (255, 255, 255), thickness, cv2.LINE_AA)
    return annotated


@app.get("/ml/health")
def health_check():
    """Kiểm tra sức khoẻ của service và thống kê số lượng vector trong FAISS"""
    return {
        "status": "UP",
        "service": "ML Service (FastAPI)",
        "models": {
            "detector": "YOLOv8-Face (Ultralytics)",
            "embedder": "MobileFaceNet (ArcFace 512-d)",
            "vector_store": "FAISS IndexFlatIP + IndexIDMap2"
        },
        "total_vectors_in_faiss": vector_store.get_total()
    }


@app.post("/ml/register-face")
async def register_face(
    file: UploadFile = File(...),
    vector_id: Optional[int] = Form(None)
):
    """
    Đăng ký khuôn mặt người mất tích vào cơ sở dữ liệu vector FAISS:
    1. Phát hiện khuôn mặt bằng YOLOv8-Face
    2. Căn chỉnh bằng Affine transform theo 5 landmarks về 112x112
    3. Trích xuất vector đặc trưng 512 chiều bằng MobileFaceNet
    4. Lưu vào FAISS index với vector_id chỉ định (hoặc tự sinh)
    5. Trả về vector_id và embedding_dim (512)
    """
    image = read_image_from_upload(file)
    detector = get_detector()
    embedder = get_embedder()

    detected_faces = detector.detect_faces(image, conf_threshold=0.45)
    if len(detected_faces) == 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Không phát hiện được khuôn mặt nào trong ảnh đăng ký. Vui lòng cung cấp ảnh chân dung rõ mặt."
        )

    # Nếu có nhiều khuôn mặt, chọn khuôn mặt có diện tích bounding box lớn nhất (chủ thể chính)
    best_face = max(
        detected_faces,
        key=lambda f: (f["bbox"][2] - f["bbox"][0]) * (f["bbox"][3] - f["bbox"][1])
    )

    # Face Alignment 112x112
    aligned_face = align_face_5point(image, best_face["landmarks"])

    # Extract 512-dim embedding (L2 normalized)
    embedding = embedder.extract_embedding(aligned_face)

    # Nếu không truyền vector_id từ Core Service, tự sinh ID tăng dần
    if vector_id is None:
        vector_id = vector_store.get_total() + 1

    # Lưu vào FAISS
    saved_id = vector_store.add_vector(embedding, vector_id)

    return {
        "vector_id": saved_id,
        "embedding_dim": 512,
        "confidence": best_face["confidence"],
        "bbox": best_face["bbox"],
        "message": f"Đăng ký khuôn mặt thành công vào FAISS với id={saved_id}."
    }


@app.post("/ml/detect-and-match")
async def detect_and_match(
    file: UploadFile = File(...),
    threshold: float = Query(0.45, ge=0.0, le=1.0, description="Ngưỡng Cosine Similarity để xác định khớp")
):
    """
    Phát hiện và so khớp khuôn mặt từ ảnh/khung hình camera:
    1. Phát hiện tất cả khuôn mặt trong ảnh
    2. Với mỗi khuôn mặt: Align 112x112 -> Trích xuất embedding 512 chiều -> Tìm trong FAISS
    3. Trả về kết quả khớp tốt nhất
    """
    image = read_image_from_upload(file)
    detector = get_detector()
    embedder = get_embedder()

    detected_faces = detector.detect_faces(image, conf_threshold=0.4)
    if len(detected_faces) == 0:
        return {
            "matched": False,
            "vector_id": None,
            "similarity": 0.0,
            "bbox": None,
            "total_faces_detected": 0,
            "message": "Không phát hiện khuôn mặt trong khung hình."
        }

    best_match = {
        "matched": False,
        "vector_id": None,
        "similarity": 0.0,
        "bbox": None,
        "total_faces_detected": len(detected_faces),
        "all_detections": []
    }

    highest_sim = -1.0

    for face in detected_faces:
        aligned = align_face_5point(image, face["landmarks"])
        emb = embedder.extract_embedding(aligned)

        search_result = vector_store.search(emb, top_k=1, threshold=threshold)
        sim = search_result["similarity"]
        is_match = search_result["matched"]

        detection_info = {
            "bbox": face["bbox"],
            "confidence": face["confidence"],
            "matched": is_match,
            "vector_id": search_result["vector_id"] if is_match else None,
            "similarity": sim
        }
        best_match["all_detections"].append(detection_info)

        if sim > highest_sim:
            highest_sim = sim
            if is_match:
                best_match["matched"] = True
                best_match["vector_id"] = search_result["vector_id"]
                best_match["similarity"] = sim
                best_match["bbox"] = face["bbox"]

    if not best_match["matched"] and highest_sim >= 0:
        best_match["similarity"] = highest_sim
        for d in best_match["all_detections"]:
            if d["similarity"] == highest_sim:
                best_match["bbox"] = d["bbox"]
                break

    return best_match


@app.post("/ml/detect-and-match-video")
async def detect_and_match_video(
    file: UploadFile = File(...),
    threshold: float = Query(0.45, ge=0.0, le=1.0, description="Ngưỡng Cosine Similarity để xác định khớp"),
    frame_interval_seconds: float = Query(1.0, ge=0.1, le=10.0, description="Khoảng thời gian (giây) giữa các khung hình được quét")
):
    """
    Nhận diện khuôn mặt từ video tải lên:
    1. Lưu tệp video tạm và mở bằng OpenCV VideoCapture
    2. Lấy mẫu khung hình định kỳ theo frame_interval_seconds (mặc định 1.0 giây/khung hình)
    3. Trích xuất đặc trưng và so khớp với FAISS
    4. Trả về timeline các mốc phát hiện và tóm tắt theo từng đối tượng
    """
    suffix = os.path.splitext(file.filename or ".mp4")[1]
    if not suffix:
        suffix = ".mp4"

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp_file:
        tmp_path = tmp_file.name
        shutil.copyfileobj(file.file, tmp_file)

    try:
        cap = cv2.VideoCapture(tmp_path)
        if not cap.isOpened():
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Không thể mở tệp video hoặc định dạng video không được hỗ trợ."
            )

        fps = cap.get(cv2.CAP_PROP_FPS)
        if fps <= 0 or np.isnan(fps):
            fps = 25.0
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        duration_seconds = round(total_frames / fps, 2) if total_frames > 0 else 0.0

        frame_step = max(1, int(round(fps * frame_interval_seconds)))

        timeline = []
        unique_persons_map = {}
        processed_frames = 0
        total_faces_detected = 0

        detector = get_detector()
        embedder = get_embedder()

        current_frame_idx = 0
        while True:
            if current_frame_idx % frame_step == 0:
                ret, frame = cap.read()
                if not ret:
                    break

                processed_frames += 1
                timestamp_sec = round(current_frame_idx / fps, 2)
                timestamp_str = f"{int(timestamp_sec // 60):02d}:{int(timestamp_sec % 60):02d}"

                detected_faces = detector.detect_faces(frame, conf_threshold=0.4)
                if detected_faces:
                    total_faces_detected += len(detected_faces)
                    for face in detected_faces:
                        aligned = align_face_5point(frame, face["landmarks"])
                        emb = embedder.extract_embedding(aligned)
                        search_result = vector_store.search(emb, top_k=1, threshold=threshold)

                        if search_result["matched"]:
                            vec_id = search_result["vector_id"]
                            sim = round(float(search_result["similarity"]), 4)
                            bbox = face["bbox"]

                            # Vẽ bounding box và xuất snapshot
                            annotated = annotate_frame(frame, bbox, f"ID #{vec_id}", sim)
                            h, w = annotated.shape[:2]
                            if w > 1280:
                                scale = 1280.0 / w
                                annotated = cv2.resize(annotated, (1280, int(h * scale)))

                            snapshot_b64 = encode_image_to_base64(annotated, quality=75)

                            match_item = {
                                "timestamp": timestamp_sec,
                                "timestamp_formatted": timestamp_str,
                                "frame_index": current_frame_idx,
                                "vector_id": vec_id,
                                "similarity": sim,
                                "bbox": bbox,
                                "snapshot_base64": snapshot_b64
                            }
                            timeline.append(match_item)

                            if vec_id not in unique_persons_map:
                                unique_persons_map[vec_id] = {
                                    "vector_id": vec_id,
                                    "max_similarity": sim,
                                    "occurrences_count": 1,
                                    "first_seen": timestamp_str,
                                    "first_seen_seconds": timestamp_sec,
                                    "last_seen": timestamp_str,
                                    "last_seen_seconds": timestamp_sec,
                                    "best_snapshot_base64": snapshot_b64,
                                    "timestamps": [timestamp_str]
                                }
                            else:
                                p = unique_persons_map[vec_id]
                                p["occurrences_count"] += 1
                                p["last_seen"] = timestamp_str
                                p["last_seen_seconds"] = timestamp_sec
                                if timestamp_str not in p["timestamps"]:
                                    p["timestamps"].append(timestamp_str)
                                if sim > p["max_similarity"]:
                                    p["max_similarity"] = sim
                                    p["best_snapshot_base64"] = snapshot_b64
            else:
                ret = cap.grab()
                if not ret:
                    break

            current_frame_idx += 1

        cap.release()
    finally:
        if os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except Exception:
                pass

    unique_persons = list(unique_persons_map.values())
    unique_persons.sort(key=lambda x: x["max_similarity"], reverse=True)

    return {
        "duration_seconds": duration_seconds,
        "total_frames": total_frames,
        "processed_frames": processed_frames,
        "total_faces_detected": total_faces_detected,
        "matched": len(unique_persons) > 0,
        "total_matches": len(timeline),
        "unique_persons": unique_persons,
        "timeline": timeline
    }


@app.delete("/ml/face/{vector_id}")
def delete_face(vector_id: int):
    """
    Xoá vector đặc trưng khỏi FAISS khi hồ sơ người mất tích bị xoá ở Core Service.
    """
    success = vector_store.delete_vector(vector_id)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Không tìm thấy hoặc không thể xoá vector id={vector_id} trong FAISS."
        )

    return {
        "success": True,
        "deleted_id": vector_id,
        "remaining_total": vector_store.get_total(),
        "message": f"Đã xoá thành công vector id={vector_id} khỏi FAISS."
    }
