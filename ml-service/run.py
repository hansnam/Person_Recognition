"""
File khởi chạy máy chủ ML Service (FastAPI + Uvicorn) trên cổng 8000
"""

import uvicorn

if __name__ == "__main__":
    print(">>> Đang khởi động ML Service trên cổng 8000...")
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=False)
