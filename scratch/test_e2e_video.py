import sys
import os
import requests
import json
import cv2
import numpy as np

# Force UTF-8 stdout
sys.stdout.reconfigure(encoding='utf-8')

CORE_URL = "http://localhost:8080/api"
ML_URL = "http://localhost:8000/ml"
BASE_DIR = os.path.dirname(os.path.dirname(__file__))
SAMPLE_IMG = os.path.join(BASE_DIR, "ml-service", "test_samples", "person_a.jpg")
TEST_VIDEO = os.path.join(BASE_DIR, "scratch", "test_video_e2e.mp4")

def main():
    print("--- 1. ML Service Health ---")
    try:
        r = requests.get(f"{ML_URL}/health", timeout=5)
        print("ML Service Health:", r.status_code, r.json())
    except Exception as e:
        print("ML Service error:", e)

    print("\n--- 2. Core Service NguoiMatTich List ---")
    try:
        r = requests.get(f"{CORE_URL}/nguoi-mat-tich", timeout=5)
        print("Core Service NguoiMatTich status:", r.status_code)
        if r.status_code == 200:
            profiles = r.json()
            print(f"Total profiles: {len(profiles)}")
            for p in profiles:
                print(f" - ID: {p.get('id')}, Name: {p.get('hoTen')}, VectorID: {p.get('vectorIdFaiss')}")
    except Exception as e:
        print("Core Service error:", e)

    print("\n--- 3. Create Test Video 3s (75 frames) ---")
    img = cv2.imread(SAMPLE_IMG)
    if img is not None:
        h, w = 480, 640
        img_resized = cv2.resize(img, (w, h))
        black_frame = np.zeros((h, w, 3), dtype=np.uint8)
        fourcc = cv2.VideoWriter_fourcc(*'mp4v')
        out = cv2.VideoWriter(TEST_VIDEO, fourcc, 25, (w, h))
        for i in range(75):
            out.write(black_frame if i < 25 else img_resized)
        out.release()
        print(f"Created test video at: {TEST_VIDEO}")

        print("\n--- 4. Send Video to Core Service POST /api/detection/match-video ---")
        try:
            with open(TEST_VIDEO, "rb") as vf:
                files = {"file": ("test_video_e2e.mp4", vf, "video/mp4")}
                r = requests.post(f"{CORE_URL}/detection/match-video?threshold=0.45&frameInterval=1.0", files=files, timeout=60)
                print("Core Service Response Status:", r.status_code)
                try:
                    resp_json = r.json()
                    print("Response JSON:")
                    print(" - Video URL:", resp_json.get("videoUrl"))
                    print(" - Duration:", resp_json.get("durationSeconds"))
                    print(" - Total Frames:", resp_json.get("totalFrames"))
                    print(" - Processed Frames:", resp_json.get("processedFrames"))
                    print(" - Total Faces:", resp_json.get("totalFacesDetected"))
                    print(" - Matched:", resp_json.get("matched"))
                    print(" - Total Matches:", resp_json.get("totalMatches"))
                    print(" - Message:", resp_json.get("message"))
                    print(" - Unique Persons:", len(resp_json.get("uniquePersons", [])))
                    print(" - Timeline Events:", len(resp_json.get("timeline", [])))
                except Exception:
                    print("Response text:", r.text)
        except Exception as e:
            print("Send video error:", e)

        if os.path.exists(TEST_VIDEO):
            os.remove(TEST_VIDEO)
            print("Cleaned up test video.")

if __name__ == "__main__":
    main()
