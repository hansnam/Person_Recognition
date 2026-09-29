@echo off
title ML Service (FastAPI) - Port 8000
cd /d %~dp0
echo ========================================================
echo  KHOI DONG ML SERVICE (YOLOv8-Face + MobileFaceNet + FAISS)
echo ========================================================
venv\Scripts\python.exe run.py
pause
