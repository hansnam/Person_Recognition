"""
Feature Extraction Module (Face Embedding)
Sử dụng mô hình MobileFaceNet (InsightFace ArcFace) để trích xuất vector đặc trưng 512 chiều từ ảnh khuôn mặt 112x112 đã qua căn chỉnh (Alignment).
"""

import os
import cv2
import numpy as np
import onnxruntime as ort

DEFAULT_WEIGHT_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "weights", "w600k_mbf.onnx")


class MobileFaceNetEmbedder:
    def __init__(self, model_path: str = DEFAULT_WEIGHT_PATH):
        self.model_path = model_path
        if not os.path.exists(self.model_path):
            # Fallback nếu chưa copy về thư mục weights
            alt_path = os.path.expanduser("~/.insightface/models/buffalo_sc/w600k_mbf.onnx")
            if os.path.exists(alt_path):
                self.model_path = alt_path
            else:
                raise FileNotFoundError(f"Không tìm thấy trọng số MobileFaceNet tại {self.model_path} hoặc {alt_path}")

        print(f"[Embedding] Nạp mô hình MobileFaceNet ONNX từ: {self.model_path}")
        session_options = ort.SessionOptions()
        session_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        self.session = ort.InferenceSession(
            self.model_path,
            sess_options=session_options,
            providers=["CPUExecutionProvider"]
        )
        self.input_name = self.session.get_inputs()[0].name
        self.output_name = self.session.get_outputs()[0].name
        print(f"[Embedding] Input name: {self.input_name}, Output name: {self.output_name}")

    def extract_embedding(self, aligned_face: np.ndarray) -> np.ndarray:
        """
        Trích xuất vector embedding 512 chiều từ ảnh 112x112 đã align.
        :param aligned_face: np.ndarray (112, 112, 3) BGR từ OpenCV
        :return: np.ndarray (512,) đã L2-normalize
        """
        assert aligned_face.shape == (112, 112, 3), f"Kích thước ảnh phải là (112, 112, 3), nhận được: {aligned_face.shape}"

        # BGR sang RGB
        face_rgb = cv2.cvtColor(aligned_face, cv2.COLOR_BGR2RGB)

        # Chuyển HWC sang CHW và chuẩn hoá (chuẩn ArcFace: (pixel - 127.5) / 127.5)
        face_tensor = face_rgb.astype(np.float32)
        face_tensor = (face_tensor - 127.5) / 127.5
        face_tensor = np.transpose(face_tensor, (2, 0, 1))  # (3, 112, 112)
        face_tensor = np.expand_dims(face_tensor, axis=0)   # (1, 3, 112, 112)

        # Chạy suy luận qua ONNX Runtime
        outputs = self.session.run([self.output_name], {self.input_name: face_tensor})
        embedding = outputs[0].flatten()  # (512,)

        # Chuẩn hoá L2-norm (||v||_2 = 1)
        norm = np.linalg.norm(embedding)
        if norm > 0:
            embedding = embedding / norm

        assert embedding.shape == (512,), f"Embedding vector phải có 512 chiều, thực tế: {embedding.shape}"
        return embedding


# Singleton instance
embedder = None

def get_embedder() -> MobileFaceNetEmbedder:
    global embedder
    if embedder is None:
        embedder = MobileFaceNetEmbedder()
    return embedder
