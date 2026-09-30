"""
Preprocessing module for Body Re-ID.
- Cắt ảnh theo Bounding Box
- Chuyển BGR sang RGB
- Resize về kích thước chuẩn (256, 128) - Height=256, Width=128
- Chuẩn hóa ImageNet: mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]
- Chuyển thành PyTorch Tensor (1, 3, 256, 128)
"""

import cv2
import numpy as np
import torch

REID_IMAGE_HEIGHT = 256
REID_IMAGE_WIDTH = 128

IMAGENET_MEAN = np.array([0.485, 0.456, 0.406], dtype=np.float32)
IMAGENET_STD = np.array([0.229, 0.224, 0.225], dtype=np.float32)


def preprocess_body_crop(image: np.ndarray, bbox: list = None) -> torch.Tensor:
    """
    Tiền xử lý ảnh người / vùng crop cơ thể phục vụ Re-ID embedding.
    :param image: Ảnh gốc (BGR numpy array) hoặc ảnh đã crop
    :param bbox: [x1, y1, x2, y2] (nếu None thì lấy toàn bộ ảnh)
    :return: torch.Tensor có shape (1, 3, 256, 128), float32
    """
    if bbox is not None:
        x1, y1, x2, y2 = bbox
        h, w = image.shape[:2]
        x1, y1 = max(0, x1), max(0, y1)
        x2, y2 = min(w, x2), min(h, y2)
        crop = image[y1:y2, x1:x2]
    else:
        crop = image

    if crop.size == 0:
        raise ValueError("Vùng ảnh cơ thể (crop) rỗng!")

    # 1. Chuyển BGR sang RGB
    rgb = cv2.cvtColor(crop, cv2.COLOR_BGR2RGB)

    # 2. Resize về (256, 128)
    resized = cv2.resize(rgb, (REID_IMAGE_WIDTH, REID_IMAGE_HEIGHT), interpolation=cv2.INTER_LINEAR)

    # 3. Scale về [0.0, 1.0] và chuẩn hóa ImageNet
    norm_img = resized.astype(np.float32) / 255.0
    norm_img = (norm_img - IMAGENET_MEAN) / IMAGENET_STD

    # 4. HWC -> CHW -> (1, C, H, W)
    chw = np.transpose(norm_img, (2, 0, 1))
    tensor = torch.from_numpy(chw).unsqueeze(0).float()
    return tensor
