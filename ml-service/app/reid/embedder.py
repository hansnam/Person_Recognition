"""
Feature Extraction Module for Body Person Re-Identification (Body Re-ID).
Sử dụng mô hình CUHK03 ResNet-50 tinh chỉnh với BNNeck để trích xuất vector đặc trưng 2048 chiều từ ảnh cơ thể 256x128.
"""

import os
import numpy as np
import torch
import torch.nn.functional as F

from .model import load_reid_model
from .preprocessing import preprocess_body_crop

DEFAULT_REID_WEIGHT_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "weights", "best_cuhk03_model_rerank.pth"
)


class BodyReIDEmbedder:
    def __init__(self, model_path: str = DEFAULT_REID_WEIGHT_PATH, device: str = None):
        self.model_path = model_path
        if device is None:
            self.device = "cuda" if torch.cuda.is_available() else "cpu"
        else:
            self.device = device

        print(f"[Body Re-ID] Khởi tạo BodyReIDEmbedder trên thiết bị: {self.device}")
        self.model = load_reid_model(self.model_path, device=self.device)

    def extract_embedding(self, image: np.ndarray, bbox: list = None) -> np.ndarray:
        """
        Trích xuất vector embedding 2048 chiều đã L2-normalize từ ảnh người.
        :param image: Ảnh BGR numpy array (ảnh crop hoặc ảnh gốc kèm bbox)
        :param bbox: [x1, y1, x2, y2] (tùy chọn)
        :return: np.ndarray shape (2048,) float32 với norm = 1.0
        """
        tensor = preprocess_body_crop(image, bbox).to(self.device)

        with torch.no_grad():
            feat = self.model(tensor)
            feat = F.normalize(feat, p=2, dim=1)

        embedding = feat.squeeze(0).cpu().numpy().astype(np.float32)

        assert embedding.shape == (2048,), f"Embedding vector phải có 2048 chiều, thực tế: {embedding.shape}"
        return embedding


_body_embedder = None


def get_body_embedder() -> BodyReIDEmbedder:
    global _body_embedder
    if _body_embedder is None:
        _body_embedder = BodyReIDEmbedder()
    return _body_embedder


BodyEmbedder = BodyReIDEmbedder

