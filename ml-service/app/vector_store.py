"""
Vector Store Module sử dụng FAISS (IndexFlatIP + IndexIDMap2)
Thực hiện tìm kiếm tương đồng vector (Cosine Similarity) cho các vector đặc trưng 512 chiều.
"""

import os
import threading
import numpy as np
import faiss

INDEX_FILE_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data", "faiss_index.bin")


class FaissVectorStore:
    def __init__(self, dim: int = 512, index_file: str = INDEX_FILE_PATH):
        self.dim = dim
        self.index_file = index_file
        self.lock = threading.Lock()
        
        # Đảm bảo thư mục lưu index tồn tại
        os.makedirs(os.path.dirname(self.index_file), exist_ok=True)

        if os.path.exists(self.index_file):
            print(f"[FAISS] Nạp index hiện có từ: {self.index_file}")
            self.index = faiss.read_index(self.index_file)
        else:
            print(f"[FAISS] Khởi tạo IndexFlatIP mới (dim={self.dim}) bọc trong IndexIDMap2...")
            base_index = faiss.IndexFlatIP(self.dim)
            self.index = faiss.IndexIDMap2(base_index)

    def _save(self):
        """Lưu trạng thái FAISS index xuống đĩa"""
        try:
            faiss.write_index(self.index, self.index_file)
        except Exception as e:
            print(f"[FAISS] Lỗi khi lưu index: {e}")

    def get_total(self) -> int:
        """Trả về tổng số vector đang được lưu trữ"""
        with self.lock:
            return self.index.ntotal

    def add_vector(self, vector: np.ndarray, vector_id: int) -> int:
        """
        Thêm một vector 512 chiều đã được L2-normalize kèm vector_id vào FAISS.
        :param vector: np.ndarray có shape (512,) hoặc (1, 512)
        :param vector_id: ID số nguyên tương ứng (được ánh xạ với MySQL)
        :return: vector_id đã lưu
        """
        with self.lock:
            vec = vector.astype(np.float32).reshape(1, self.dim)
            # Đảm bảo chuẩn hoá L2 (norm = 1.0) để IndexFlatIP tương đương Cosine Similarity
            faiss.normalize_L2(vec)

            ids = np.array([vector_id], dtype=np.int64)
            self.index.add_with_ids(vec, ids)
            self._save()
            print(f"[FAISS] Đã lưu vector id={vector_id}. Tổng số vector hiện tại: {self.index.ntotal}")
            return vector_id

    def search(self, query_vector: np.ndarray, top_k: int = 1, threshold: float = 0.45):
        """
        Tìm kiếm vector gần nhất dựa trên Cosine Similarity (IndexFlatIP).
        :param query_vector: np.ndarray (512,) hoặc (1, 512)
        :param top_k: Số lượng kết quả gần nhất lấy ra
        :param threshold: Ngưỡng tương đồng cosine tối thiểu để coi là khớp
        :return: dict(matched: bool, vector_id: int|None, similarity: float)
        """
        with self.lock:
            if self.index.ntotal == 0:
                return {
                    "matched": False,
                    "vector_id": None,
                    "similarity": 0.0,
                    "message": "Cơ sở dữ liệu vector rỗng."
                }

            q_vec = query_vector.astype(np.float32).reshape(1, self.dim)
            faiss.normalize_L2(q_vec)

            # similarities: ma trận khoảng cách cosine; indices: danh sách vector_id
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
                print(f"[FAISS] Đã xoá {removed_count} vector có id={vector_id}. Còn lại: {self.index.ntotal}")
                return removed_count > 0
            except Exception as e:
                print(f"[FAISS] Lỗi khi xoá vector id={vector_id}: {e}")
                return False


# Singleton instance toàn cục
vector_store = FaissVectorStore()
