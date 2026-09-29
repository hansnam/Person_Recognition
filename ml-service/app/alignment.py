"""
Face Alignment Module
Sử dụng Affine Transformation theo 5 điểm mốc (landmarks) để chuẩn hoá khuôn mặt về kích thước 112x112 (chuẩn ArcFace).
5 điểm mốc tương ứng: Mắt trái, Mắt phải, Mũi, Khóe miệng trái, Khóe miệng phải.
"""

import cv2
import numpy as np

# Toạ độ 5 điểm mốc chuẩn trên ảnh 112x112 (ArcFace standard template)
ARCFACE_REFERENCE_POINTS = np.array([
    [38.2946, 51.6963],  # Mắt trái
    [73.5318, 51.5014],  # Mắt phải
    [56.0252, 71.7366],  # Mũi
    [41.5493, 92.3655],  # Khóe miệng trái
    [70.7299, 92.2041]   # Khóe miệng phải
], dtype=np.float32)


def align_face_5point(image: np.ndarray, landmarks: np.ndarray, crop_size: tuple = (112, 112)) -> np.ndarray:
    """
    Căn chỉnh khuôn mặt bằng phép biến đổi Affine (Similarity Transform)
    dựa trên 5 điểm mốc và chuẩn hoá về kích thước (112, 112).

    :param image: Ảnh gốc (BGR, dạng numpy array)
    :param landmarks: Ma trận (5, 2) toạ độ 5 điểm mốc
    :param crop_size: Kích thước đầu ra chuẩn, mặc định (112, 112)
    :return: Ảnh khuôn mặt đã được align và crop (112, 112, 3)
    """
    assert landmarks.shape == (5, 2), f"Landmarks phải có shape (5, 2), hiện tại là {landmarks.shape}"
    src_pts = landmarks.astype(np.float32)
    dst_pts = ARCFACE_REFERENCE_POINTS.copy()

    if crop_size != (112, 112):
        dst_pts[:, 0] = dst_pts[:, 0] * (crop_size[0] / 112.0)
        dst_pts[:, 1] = dst_pts[:, 1] * (crop_size[1] / 112.0)

    # Ước lượng ma trận Affine (similarity transform: xoay, tịnh tiến, tỉ lệ đồng dạng)
    transform_matrix, _ = cv2.estimateAffinePartial2D(src_pts, dst_pts, method=cv2.LMEDS)

    if transform_matrix is None:
        # Fallback nếu ma trận không tính được (suy biến)
        transform_matrix = cv2.getAffineTransform(src_pts[:3], dst_pts[:3])

    # Áp dụng phép biến đổi Affine
    aligned_face = cv2.warpAffine(
        image,
        transform_matrix,
        crop_size,
        flags=cv2.INTER_LINEAR,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=0
    )

    return aligned_face
