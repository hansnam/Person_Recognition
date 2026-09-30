"""
Vector Store Module cho Body Re-ID sử dụng FAISS (IndexFlatIP + IndexIDMap2).
Thực hiện tìm kiếm tương đồng vector (Cosine Similarity) cho các vector đặc trưng 2048 chiều.
Lưu trữ độc lập tại data/body_reid_index.bin.
"""

import os
import threading
import numpy as np
import faiss

BODY_INDEX_FILE_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "data", "body_reid_index.bin"
)


class BodyFaissVectorStore:
    def __init__(self, dim: int = 2048, index_file: str = BODY_INDEX_FILE_PATH):
        self.dim = dim
        self.index_file = index_file
        self.lock = threading.Lock()

        os.makedirs(os.path.dirname(self.index_file), exist_ok=True)

        if os.path.exists(self.index_file):
            print(f"[Body FAISS] Nạp index hiện có từ: {self.index_file}")
            self.index = faiss.read_index(self.index_file)
        else:
            print(f"[Body FAISS] Khởi tạo IndexFlatIP mới (dim={self.dim}) bọc trong IndexIDMap2...")
            base_index = faiss.IndexFlatIP(self.dim)
            self.index = faiss.IndexIDMap2(base_index)

    def _save(self):
        """Lưu trạng thái FAISS index xuống đĩa"""
        try:
            faiss.write_index(self.index, self.index_file)
        except Exception as e:
            print(f"[Body FAISS] Lỗi khi lưu index: {e}")

    def get_total(self) -> int:
        """Trả về tổng số vector đang được lưu trữ"""
        with self.lock:
            return self.index.ntotal

    def add_vector(self, vector: np.ndarray, vector_id: int) -> int:
        """
        Thêm một vector 2048 chiều đã được L2-normalize kèm vector_id vào FAISS.
        :param vector: np.ndarray có shape (2048,) hoặc (1, 2048)
        :param vector_id: ID số nguyên tương ứng (ánh xạ với MySQL vectorIdFaiss)
        :return: vector_id đã lưu
        """
        with self.lock:
            vec = vector.astype(np.float32).reshape(1, self.dim)
            faiss.normalize_L2(vec)

            ids = np.array([vector_id], dtype=np.int64)
            self.index.add_with_ids(vec, ids)
            self._save()
            print(f"[Body FAISS] Đã lưu vector id={vector_id}. Tổng số vector hiện tại: {self.index.ntotal}")
            return vector_id

    def search(self, query_vector: np.ndarray, top_k: int = 1, threshold: float = 0.65):
        """
        Tìm kiếm vector gần nhất dựa trên Cosine Similarity (IndexFlatIP).
        :param query_vector: np.ndarray (2048,) hoặc (1, 2048)
        :param top_k: Số lượng kết quả gần nhất lấy ra
        :param threshold: Ngưỡng tương đồng cosine tối thiểu để coi là khớp (mặc định 0.65)
        :return: dict(matched: bool, vector_id: int|None, similarity: float, threshold: float)
        """
        with self.lock:
            if self.index.ntotal == 0:
                return {
                    "matched": False,
                    "vector_id": None,
                    "similarity": 0.0,
                    "threshold": threshold,
                    "message": "Cơ sở dữ liệu vector Re-ID rỗng."
                }

            q_vec = query_vector.astype(np.float32).reshape(1, self.dim)
            faiss.normalize_L2(q_vec)

            similarities, indices = self.index.search(q_vec, top_k)

            best_sim = float(similarities[0][0])
            best_id = int(indices[0][0])

            matched = (best_id != -1) and (best_sim >= threshold)

            return {
                "matched": matched,
                "vector_id": best_id if matched else None,
                "similarity": round(best_sim, 4),
                "threshold": threshold
            }

    def delete_vector(self, vector_id: int) -> bool:
        """
        Xoá vector theo vector_id khỏi FAISS index.
        :param vector_id: ID cần xoá
        :return: True nếu xoá thành công, False nếu lỗi
        """
        with self.lock:
            try:
                ids_to_remove = np.array([vector_id], dtype=np.int64)
                removed_count = self.index.remove_ids(ids_to_remove)
                self._save()
                print(f"[Body FAISS] Đã xoá {removed_count} vector có id={vector_id}. Còn lại: {self.index.ntotal}")
                return removed_count > 0
            except Exception as e:
                print(f"[Body FAISS] Lỗi khi xoá vector id={vector_id}: {e}")
                return False


body_vector_store = BodyFaissVectorStore()
FaissBodyVectorStore = BodyFaissVectorStore

