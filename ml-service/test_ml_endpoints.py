"""
Kiểm thử toàn diện Bước 3 (FAISS Vector Store + FastAPI Endpoints):
- TestClient gọi các endpoint:
  1. GET /ml/health
  2. POST /ml/register-face (Đăng ký Person A -> lưu FAISS)
  3. POST /ml/detect-and-match (Gửi lại ảnh Person A -> Kiểm tra matched = True)
  4. POST /ml/detect-and-match (Gửi ảnh Person B -> Kiểm tra matched = False)
  5. DELETE /ml/face/{id} (Xoá Person A khỏi FAISS)
  6. POST /ml/detect-and-match (Gửi lại ảnh Person A sau khi xoá -> Kiểm tra matched = False)
"""

import os
import io
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)
SAMPLES_DIR = os.path.join(os.path.dirname(__file__), "test_samples")


def run_full_ml_service_test():
    print("\n========================================================")
    print(" BƯỚC 3: KIỂM THỬ FAISS VECTOR STORE & FASTAPI ENDPOINTS")
    print("========================================================")

    # 1. Health Check
    res_health = client.get("/ml/health")
    assert res_health.status_code == 200, f"Health check thất bại: {res_health.text}"
    print(f"1. [GET /ml/health] Thành công: {res_health.json()}")

    # 2. Đăng ký Person A vào FAISS
    person_a_path = os.path.join(SAMPLES_DIR, "person_a.jpg")
    with open(person_a_path, "rb") as f:
        file_bytes = f.read()

    res_reg = client.post(
        "/ml/register-face",
        files={"file": ("person_a.jpg", io.BytesIO(file_bytes), "image/jpeg")},
        data={"vector_id": 101}  # Test gán ID cụ thể
    )
    assert res_reg.status_code == 200, f"Đăng ký thất bại: {res_reg.text}"
    reg_data = res_reg.json()
    vector_id = reg_data["vector_id"]
    dim = reg_data["embedding_dim"]
    print(f"2. [POST /ml/register-face] Đăng ký thành công:")
    print(f"   - vector_id: {vector_id}")
    print(f"   - embedding_dim: {dim} (Yêu cầu: 512)")
    print(f"   - Bounding Box: {reg_data['bbox']}")
    assert vector_id == 101
    assert dim == 512

    # 3. Detect & Match với ảnh Person A (Kỳ vọng: Matched = True, Sim > 0.90)
    res_match_a = client.post(
        "/ml/detect-and-match?threshold=0.45",
        files={"file": ("person_a.jpg", io.BytesIO(file_bytes), "image/jpeg")}
    )
    assert res_match_a.status_code == 200, f"Match thất bại: {res_match_a.text}"
    match_a_data = res_match_a.json()
    print(f"3. [POST /ml/detect-and-match] So khớp với Person A:")
    print(f"   - matched: {match_a_data['matched']}")
    print(f"   - vector_id: {match_a_data['vector_id']}")
    print(f"   - similarity: {match_a_data['similarity']}")
    assert match_a_data["matched"] is True, "Lỗi: Ảnh Person A phải khớp với hồ sơ Person A!"
    assert match_a_data["vector_id"] == 101
    assert match_a_data["similarity"] > 0.90

    # 4. Detect & Match với ảnh Person B (Kỳ vọng: Matched = False, Sim < 0.45)
    person_b_path = os.path.join(SAMPLES_DIR, "person_b.jpg")
    with open(person_b_path, "rb") as f:
        file_bytes_b = f.read()

    res_match_b = client.post(
        "/ml/detect-and-match?threshold=0.45",
        files={"file": ("person_b.jpg", io.BytesIO(file_bytes_b), "image/jpeg")}
    )
    assert res_match_b.status_code == 200
    match_b_data = res_match_b.json()
    print(f"4. [POST /ml/detect-and-match] So khớp với Person B (người lạ):")
    print(f"   - matched: {match_b_data['matched']}")
    print(f"   - vector_id: {match_b_data['vector_id']}")
    print(f"   - similarity: {match_b_data['similarity']}")
    assert match_b_data["matched"] is False, "Lỗi: Người lạ không được khớp!"

    # 5. Xoá Person A khỏi FAISS
    res_del = client.delete(f"/ml/face/{vector_id}")
    assert res_del.status_code == 200
    print(f"5. [DELETE /ml/face/{vector_id}] Xoá vector thành công: {res_del.json()}")

    # 6. So khớp lại Person A sau khi xoá (Kỳ vọng: Matched = False)
    res_match_a_after = client.post(
        "/ml/detect-and-match?threshold=0.45",
        files={"file": ("person_a.jpg", io.BytesIO(file_bytes), "image/jpeg")}
    )
    match_after_data = res_match_a_after.json()
    print(f"6. [POST /ml/detect-and-match] So khớp Person A sau khi xoá:")
    print(f"   - matched: {match_after_data['matched']}")
    print(f"   - vector_id: {match_after_data['vector_id']}")
    assert match_after_data["matched"] is False, "Lỗi: Sau khi xoá khỏi FAISS thì không được khớp!"

    print("\n>>> KIỂM THỬ BƯỚC 3 (FAISS + FASTAPI API) THÀNH CÔNG 100%! <<<")


if __name__ == "__main__":
    run_full_ml_service_test()
