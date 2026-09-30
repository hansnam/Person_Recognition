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

import time
import concurrent.futures
from app.reid.service import run_body_pipeline, get_body_detector
from app.reid.embedder import get_body_embedder
from app.reid.vector_store_body import body_vector_store
from app.fusion import associate_face_body, fuse_results

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


def run_face_pipeline_internal(image: np.ndarray, face_threshold: float = 0.45) -> List[Dict[str, Any]]:
    """Pipeline nhận diện khuôn mặt nội bộ phục vụ Fusion"""
    detector = get_detector()
    embedder = get_embedder()
    detected_faces = detector.detect_faces(image, conf_threshold=0.4)
    results = []
    for face in detected_faces:
        try:
            aligned = align_face_5point(image, face["landmarks"])
            emb = embedder.extract_embedding(aligned)
            search_res = vector_store.search(emb, top_k=1, threshold=face_threshold)
            results.append({
                "bbox": face["bbox"],
                "confidence": face["confidence"],
                "matched": search_res["matched"],
                "person_id": search_res["vector_id"] if search_res["matched"] else None,
                "similarity": search_res["similarity"]
            })
        except Exception as e:
            print(f"[Face Pipeline] Lỗi khi xử lý face {face.get('bbox')}: {e}")
    return results


@app.get("/ml/health")
def health_check():
    """Kiểm tra sức khoẻ của service và thống kê số lượng vector trong FAISS (Face + Body)"""
    return {
        "status": "UP",
        "service": "ML Service (FastAPI)",
        "models": {
            "detector": "YOLOv8-Face (Ultralytics)",
            "embedder": "MobileFaceNet (ArcFace 512-d)",
            "vector_store": "FAISS IndexFlatIP + IndexIDMap2",
            "body_detector": "YOLOv8n Person (Ultralytics)",
            "body_embedder": "ResNet-50 CUHK03 (BNNeck 2048-d)",
            "body_vector_store": "FAISS IndexFlatIP + IndexIDMap2 (dim=2048)"
        },
        "total_vectors_in_faiss": vector_store.get_total(),
        "total_body_vectors_in_faiss": body_vector_store.get_total()
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


@app.post("/ml/register-body")
async def register_body(
    file: UploadFile = File(...),
    vector_id: int = Form(..., description="ID vector định danh trong FAISS (trùng với vectorIdFaiss của Face)")
):
    """
    Đăng ký đặc trưng cơ thể người mất tích vào cơ sở dữ liệu vector FAISS (Body Re-ID):
    1. Phát hiện người trong ảnh bằng YOLOv8n
    2. Cắt vùng cơ thể lớn nhất và chuyển sang kích thước 256x128 RGB
    3. Trích xuất vector đặc trưng 2048 chiều bằng ResNet-50 CUHK03
    4. Lưu vào Body FAISS index với vector_id chỉ định
    """
    image = read_image_from_upload(file)
    detector = get_body_detector()
    embedder = get_body_embedder()

    detected_bodies = detector.detect_bodies(image, conf_threshold=0.35)
    if len(detected_bodies) == 0:
        # Nếu không detect được body bbox bằng YOLO, fallback dùng toàn bộ ảnh
        best_bbox = [0, 0, image.shape[1], image.shape[0]]
        conf = 1.0
    else:
        # Chọn body có diện tích bounding box lớn nhất (chủ thể chính)
        best_body = max(
            detected_bodies,
            key=lambda b: (b["bbox"][2] - b["bbox"][0]) * (b["bbox"][3] - b["bbox"][1])
        )
        best_bbox = best_body["bbox"]
        conf = best_body["confidence"]

    emb = embedder.extract_embedding(image, bbox=best_bbox)
    saved_id = body_vector_store.add_vector(emb, vector_id)

    return {
        "vector_id": saved_id,
        "embedding_dim": 2048,
        "confidence": conf,
        "bbox": best_bbox,
        "message": f"Đăng ký đặc trưng cơ thể thành công vào FAISS với id={saved_id}."
    }


@app.delete("/ml/body/{vector_id}")
def delete_body(vector_id: int):
    """
    Xoá vector đặc trưng cơ thể khỏi Body FAISS khi hồ sơ người mất tích bị xoá.
    """
    success = body_vector_store.delete_vector(vector_id)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Không tìm thấy hoặc không thể xoá vector body id={vector_id} trong FAISS."
        )

    return {
        "success": True,
        "deleted_id": vector_id,
        "remaining_total": body_vector_store.get_total(),
        "message": f"Đã xoá thành công vector body id={vector_id} khỏi FAISS."
    }


@app.post("/ml/detect-and-match-fusion")
async def detect_and_match_fusion(
    file: UploadFile = File(...),
    face_threshold: float = Query(0.45, ge=0.0, le=1.0, description="Ngưỡng Cosine Similarity khuôn mặt"),
    body_threshold: float = Query(0.65, ge=0.0, le=1.0, description="Ngưỡng Cosine Similarity dáng người/quần áo")
):
    """
    Nhận diện đa phương thức (Multimodal Fusion: Face Recognition + Body Re-ID):
    1. Chạy Face pipeline và Body pipeline song song trong ThreadPoolExecutor (max_workers=2)
    2. Bắt lỗi độc lập cho từng pipeline để đảm bảo Error Isolation
    3. Ghép nối Face và Body bằng Containment Check
    4. Suy luận trạng thái Fusion (CONFIRMED, FACE_MATCH_BODY_MISMATCH, FACE_CANDIDATE, BODY_CANDIDATE, UNKNOWN)
    5. Trả về cấu trúc chi tiết, ưu tiên Face Recognition tuyệt đối.
    """
    start_time = time.time()
    image = read_image_from_upload(file)

    face_results = []
    body_results = []

    def _exec_face():
        try:
            return run_face_pipeline_internal(image, face_threshold)
        except Exception as e:
            print(f"[Fusion Endpoint] Lỗi Face pipeline: {e}")
            return []

    def _exec_body():
        try:
            return run_body_pipeline(image, body_threshold)
        except Exception as e:
            print(f"[Fusion Endpoint] Lỗi Body pipeline: {e}")
            return []

    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        f_future = executor.submit(_exec_face)
        b_future = executor.submit(_exec_body)
        face_results = f_future.result()
        body_results = b_future.result()

    detections = associate_face_body(face_results, body_results)
    elapsed_ms = int((time.time() - start_time) * 1000)

    matched = any(
        d["fusion"]["status"] in ("CONFIRMED", "FACE_MATCH_BODY_MISMATCH", "FACE_CANDIDATE", "BODY_CANDIDATE")
        for d in detections
    )

    return {
        "matched": matched,
        "total_persons": len(detections),
        "total_ms": elapsed_ms,
        "detections": detections
    }


@app.post("/ml/detect-and-match-fusion-video")
async def detect_and_match_fusion_video(
    file: UploadFile = File(...),
    face_threshold: float = Query(0.45, ge=0.0, le=1.0, description="Ngưỡng Cosine Similarity khuôn mặt"),
    body_threshold: float = Query(0.65, ge=0.0, le=1.0, description="Ngưỡng Cosine Similarity dáng người/quần áo"),
    frame_interval_seconds: float = Query(1.0, ge=0.1, le=10.0, description="Khoảng thời gian (giây) giữa các khung hình được quét")
):
    """
    Nhận diện Fusion (Face + Body Re-ID) từ video tải lên:
    1. Lưu tệp video tạm và mở bằng cv2.VideoCapture
    2. Lấy mẫu khung hình định kỳ theo frame_interval_seconds
    3. Thực thi song song Face + Body và Fusion cho từng frame
    4. Trả về timeline và tổng hợp unique_persons (giữ riêng face_similarity và body_similarity)
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
        total_detections_count = 0
        current_frame_idx = 0

        while True:
            if current_frame_idx % frame_step == 0:
                ret, frame = cap.read()
                if not ret:
                    break

                processed_frames += 1
                timestamp_sec = round(current_frame_idx / fps, 2)
                timestamp_str = f"{int(timestamp_sec // 60):02d}:{int(timestamp_sec % 60):02d}"

                with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
                    f_future = executor.submit(lambda: run_face_pipeline_internal(frame, face_threshold))
                    b_future = executor.submit(lambda: run_body_pipeline(frame, body_threshold))
                    f_results = f_future.result()
                    b_results = b_future.result()

                frame_detections = associate_face_body(f_results, b_results)
                total_detections_count += len(frame_detections)

                for det in frame_detections:
                    status_name = det["fusion"]["status"]
                    pid = det["fusion"]["person_id"]

                    if status_name != "UNKNOWN" and pid is not None:
                        face_sim = det["face"]["similarity"] if det["face"] else 0.0
                        body_sim = det["body"]["similarity"] if det["body"] else 0.0

                        annotated = frame.copy()
                        if det["body"] and det["body"]["bbox"]:
                            bx1, by1, bx2, by2 = det["body"]["bbox"]
                            cv2.rectangle(annotated, (bx1, by1), (bx2, by2), (40, 200, 40), 2)
                        if det["face"] and det["face"]["bbox"]:
                            fx1, fy1, fx2, fy2 = det["face"]["bbox"]
                            cv2.rectangle(annotated, (fx1, fy1), (fx2, fy2), (0, 0, 238), 2)

                        snapshot_b64 = encode_image_to_base64(annotated, quality=70)

                        timeline.append({
                            "timestamp": timestamp_sec,
                            "timestamp_formatted": timestamp_str,
                            "frame_index": current_frame_idx,
                            "person_id": pid,
                            "fusion_status": status_name,
                            "face_similarity": face_sim,
                            "body_similarity": body_sim,
                            "bbox": det["bbox"],
                            "body_warning": det["fusion"].get("body_warning", False),
                            "message": det["fusion"].get("message", ""),
                            "snapshot_base64": snapshot_b64
                        })

                        if pid not in unique_persons_map:
                            unique_persons_map[pid] = {
                                "person_id": pid,
                                "fusion_status": status_name,
                                "max_face_similarity": face_sim,
                                "max_body_similarity": body_sim,
                                "occurrences_count": 1,
                                "first_seen": timestamp_str,
                                "first_seen_seconds": timestamp_sec,
                                "last_seen": timestamp_str,
                                "last_seen_seconds": timestamp_sec,
                                "best_snapshot_base64": snapshot_b64,
                                "timestamps": [timestamp_str]
                            }
                        else:
                            p = unique_persons_map[pid]
                            p["occurrences_count"] += 1
                            p["last_seen"] = timestamp_str
                            p["last_seen_seconds"] = timestamp_sec
                            if timestamp_str not in p["timestamps"]:
                                p["timestamps"].append(timestamp_str)
                            if face_sim > p["max_face_similarity"]:
                                p["max_face_similarity"] = face_sim
                                p["best_snapshot_base64"] = snapshot_b64
                            if body_sim > p["max_body_similarity"]:
                                p["max_body_similarity"] = body_sim
                            if status_name == "CONFIRMED":
                                p["fusion_status"] = "CONFIRMED"
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
    unique_persons.sort(key=lambda x: max(x["max_face_similarity"], x["max_body_similarity"]), reverse=True)

    return {
        "duration_seconds": duration_seconds,
        "total_frames": total_frames,
        "processed_frames": processed_frames,
        "total_detections": total_detections_count,
        "matched": len(unique_persons) > 0,
        "total_matches": len(timeline),
        "unique_persons": unique_persons,
        "timeline": timeline
    }

