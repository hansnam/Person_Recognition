import React, { useState, useRef, useEffect } from 'react';
import {
  Camera,
  Video,
  VideoOff,
  RefreshCw,
  Upload,
  AlertTriangle,
  X,
  Film,
  Play,
  Clock,
  Sliders,
  Sparkles,
  ChevronRight,
  Eye,
  CheckCircle2,
  Maximize2,
  Radio,
  Scan
} from 'lucide-react';
import { detectFace, detectVideo, getFullImageUrl } from '../services/api';

export default function CameraMonitor({ onNewAlert }) {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const fileInputRef = useRef(null);
  const videoInputRef = useRef(null);
  const uploadedVideoRef = useRef(null);
  const videoCanvasRef = useRef(null);
  const offscreenCanvasRef = useRef(null);
  const lastLiveScanTimeRef = useRef(0);
  const liveScanInProgressRef = useRef(false);
  const lastAlertTimeRef = useRef(0);
  const staticImageRef = useRef(null);

  // Chế độ giám sát: 'webcam' | 'image' | 'video'
  const [monitorMode, setMonitorMode] = useState('webcam');

  // Trạng thái Webcam
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [isAutoScan, setIsAutoScan] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [stream, setStream] = useState(null);
  const [threshold, setThreshold] = useState(0.45);
  const [lastResult, setLastResult] = useState(null);
  const [alertData, setAlertData] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [staticImage, setStaticImage] = useState(null);

  // Trạng thái Video Upload & Quét Trực Tiếp Khi Phát
  const [videoFile, setVideoFile] = useState(null);
  const [videoPreviewUrl, setVideoPreviewUrl] = useState(null);
  const [videoResult, setVideoResult] = useState(null);
  const [isVideoAnalyzing, setIsVideoAnalyzing] = useState(false);
  const [frameInterval, setFrameInterval] = useState(1.0);
  const [videoCurrentTime, setVideoCurrentTime] = useState(0);
  const [videoDuration, setVideoDuration] = useState(0);
  const [selectedTimelineEvent, setSelectedTimelineEvent] = useState(null);
  const [isVideoPlaying, setIsVideoPlaying] = useState(false);
  const [isVideoScanningLive, setIsVideoScanningLive] = useState(false);
  const [videoActiveTab, setVideoActiveTab] = useState('frame'); // 'frame' | 'summary'

  // Tiến độ phân tích video chi tiết
  const [analysisProgress, setAnalysisProgress] = useState(0);
  const [analysisStageText, setAnalysisStageText] = useState('');
  const [analysisDetailText, setAnalysisDetailText] = useState('');
  const [analysisElapsedTime, setAnalysisElapsedTime] = useState(0);

  // Âm thanh cảnh báo bằng Web Audio API
  const playAlertSound = () => {
    try {
      const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, audioCtx.currentTime); // Note A5
      osc.frequency.exponentialRampToValueAtTime(440, audioCtx.currentTime + 0.4);
      gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.4);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.4);
    } catch (e) {
      console.warn('Không thể phát âm thanh:', e);
    }
  };

  // Dọn dẹp URL ảnh tạo bởi createObjectURL
  useEffect(() => {
    return () => {
      if (staticImage?.url) {
        URL.revokeObjectURL(staticImage.url);
      }
    };
  }, [staticImage]);

  // Dọn dẹp URL video tạo bởi createObjectURL
  useEffect(() => {
    return () => {
      if (videoPreviewUrl) {
        URL.revokeObjectURL(videoPreviewUrl);
      }
    };
  }, [videoPreviewUrl]);

  // Chuyển đổi chế độ giám sát
  const switchMode = (newMode) => {
    if (newMode !== 'webcam' && isCameraActive) {
      stopCamera();
    }
    if (newMode !== 'video' && uploadedVideoRef.current) {
      uploadedVideoRef.current.pause();
      setIsVideoPlaying(false);
      setIsVideoScanningLive(false);
      clearVideoCanvas();
    }
    setMonitorMode(newMode);
    setErrorMessage('');
  };

  // Khởi động Camera
  const startCamera = async () => {
    try {
      setErrorMessage('');
      if (staticImage) {
        clearStaticImage();
      }
      setMonitorMode('webcam');
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      });
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
      }
      setStream(mediaStream);
      setIsCameraActive(true);
    } catch (err) {
      setErrorMessage('Không thể mở webcam: ' + (err.message || 'Vui lòng cấp quyền truy cập camera.'));
    }
  };

  // Dừng Camera
  const stopCamera = () => {
    if (stream) {
      stream.getTracks().forEach((track) => track.stop());
      setStream(null);
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setIsCameraActive(false);
    setIsAutoScan(false);
    clearCanvas();
  };

  // Đóng / Xóa ảnh tĩnh
  const clearStaticImage = () => {
    if (staticImage?.url) {
      URL.revokeObjectURL(staticImage.url);
    }
    setStaticImage(null);
    staticImageRef.current = null;
    setLastResult(null);
    setErrorMessage('');
    clearCanvas();
  };

  // Xoá canvas vẽ
  const clearCanvas = () => {
    const canvas = canvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
  };

  // Chụp khung hình từ webcam và gửi nhận diện
  const captureAndDetect = async () => {
    if (!videoRef.current || isProcessing) return;

    const video = videoRef.current;
    if (video.videoWidth === 0 || video.videoHeight === 0) return;

    const canvas = canvasRef.current;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');

    // Chụp khung hình hiện tại
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    canvas.toBlob(async (blob) => {
      if (!blob) return;
      await processImageBlob(blob, canvas.width, canvas.height);
    }, 'image/jpeg', 0.9);
  };

  // Xử lý gửi ảnh nhận diện
  const processImageBlob = async (blob, imgWidth, imgHeight, imgElement = null) => {
    setIsProcessing(true);
    setErrorMessage('');
    try {
      const result = await detectFace(blob, threshold);
      setLastResult(result);

      // Vẽ bounding box nếu có
      drawBoundingBox(result, imgWidth, imgHeight, imgElement || staticImageRef.current);

      // Nếu phát hiện trùng khớp người thân
      if (result.matched && result.hoSo) {
        playAlertSound();
        setAlertData(result);
        if (onNewAlert) onNewAlert(result);
      }
    } catch (err) {
      setErrorMessage('Lỗi xử lý nhận diện: ' + err.message);
    } finally {
      setIsProcessing(false);
    }
  };

  // Xoá canvas phủ trên video
  const clearVideoCanvas = () => {
    const canvas = videoCanvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
  };

  // Vẽ khung khuôn mặt cho tất cả các khuôn mặt được phát hiện trên canvas chỉ định
  const drawBoxesOnTargetCanvas = (targetCanvas, result, width, height, currentImg = null) => {
    if (!targetCanvas) return;
    const ctx = targetCanvas.getContext('2d');

    if (width && height) {
      if (targetCanvas.width !== width || targetCanvas.height !== height) {
        targetCanvas.width = width;
        targetCanvas.height = height;
      }
    }

    ctx.clearRect(0, 0, targetCanvas.width, targetCanvas.height);

    const imgToDraw = currentImg || (targetCanvas === canvasRef.current ? staticImageRef.current : null);
    if (imgToDraw) {
      ctx.drawImage(imgToDraw, 0, 0, targetCanvas.width, targetCanvas.height);
    }

    if (!result) return;

    const detections = result.detections && result.detections.length > 0
      ? result.detections
      : (result.bbox ? [{
          bbox: result.bbox,
          matched: result.matched,
          similarity: result.similarity,
          hoSo: result.hoSo,
        }] : []);

    if (detections.length === 0) return;

    const scale = Math.max(targetCanvas.width, targetCanvas.height) / 1000;
    const lineWidth = Math.max(3, Math.round(3 * scale));
    const fontSize = Math.max(13, Math.round(15 * scale));
    const pad = Math.max(4, Math.round(6 * scale));
    const badgeHeight = fontSize + pad * 2;
    const cornerSize = Math.max(12, Math.round(18 * scale));

    detections.forEach((det, index) => {
      if (!det.bbox || det.bbox.length < 4) return;
      const [x1, y1, x2, y2] = det.bbox;
      const boxW = x2 - x1;
      const boxH = y2 - y1;
      const simPercent = ((det.similarity || 0) * 100).toFixed(1);
      const cLen = Math.min(cornerSize, boxW / 3, boxH / 3);

      if (det.matched) {
        ctx.fillStyle = 'rgba(239, 68, 68, 0.16)';
        ctx.fillRect(x1, y1, boxW, boxH);

        ctx.strokeStyle = '#ef4444';
        ctx.lineWidth = lineWidth;
        ctx.strokeRect(x1, y1, boxW, boxH);

        ctx.strokeStyle = '#ff2222';
        ctx.lineWidth = lineWidth + 2.5;
        ctx.beginPath();
        ctx.moveTo(x1, y1 + cLen); ctx.lineTo(x1, y1); ctx.lineTo(x1 + cLen, y1);
        ctx.moveTo(x2 - cLen, y1); ctx.lineTo(x2, y1); ctx.lineTo(x2, y1 + cLen);
        ctx.moveTo(x1, y2 - cLen); ctx.lineTo(x1, y2); ctx.lineTo(x1 + cLen, y2);
        ctx.moveTo(x2 - cLen, y2); ctx.lineTo(x2, y2); ctx.lineTo(x2, y2 - cLen);
        ctx.stroke();

        const label = `🚨 TRÙNG KHỚP: ${det.hoSo?.hoTen || 'Người thân'} (${simPercent}%)`;
        ctx.font = `bold ${fontSize}px Inter, sans-serif`;
        const textWidth = ctx.measureText(label).width;
        const labelY = Math.max(0, y1 - badgeHeight);

        ctx.fillStyle = '#dc2626';
        ctx.fillRect(x1, labelY, textWidth + pad * 2, badgeHeight);
        ctx.strokeStyle = '#fca5a5';
        ctx.lineWidth = 1;
        ctx.strokeRect(x1, labelY, textWidth + pad * 2, badgeHeight);

        ctx.fillStyle = '#ffffff';
        ctx.fillText(label, x1 + pad, labelY + fontSize + pad - 2);

      } else {
        ctx.fillStyle = 'rgba(6, 182, 212, 0.08)';
        ctx.fillRect(x1, y1, boxW, boxH);

        ctx.strokeStyle = '#06b6d4';
        ctx.lineWidth = lineWidth;
        ctx.strokeRect(x1, y1, boxW, boxH);

        ctx.strokeStyle = '#38bdf8';
        ctx.lineWidth = lineWidth + 1.5;
        ctx.beginPath();
        ctx.moveTo(x1, y1 + cLen); ctx.lineTo(x1, y1); ctx.lineTo(x1 + cLen, y1);
        ctx.moveTo(x2 - cLen, y1); ctx.lineTo(x2, y1); ctx.lineTo(x2, y1 + cLen);
        ctx.moveTo(x1, y2 - cLen); ctx.lineTo(x1, y2); ctx.lineTo(x1 + cLen, y2);
        ctx.moveTo(x2 - cLen, y2); ctx.lineTo(x2, y2); ctx.lineTo(x2, y2 - cLen);
        ctx.stroke();

        const label = `✓ Chưa khớp (#${index + 1} • ${simPercent}%)`;
        ctx.font = `${fontSize}px Inter, sans-serif`;
        const textWidth = ctx.measureText(label).width;
        const labelY = Math.max(0, y1 - badgeHeight);

        ctx.fillStyle = 'rgba(10, 20, 40, 0.92)';
        ctx.fillRect(x1, labelY, textWidth + pad * 2, badgeHeight);
        ctx.strokeStyle = '#06b6d4';
        ctx.lineWidth = 1;
        ctx.strokeRect(x1, labelY, textWidth + pad * 2, badgeHeight);

        ctx.fillStyle = '#38bdf8';
        ctx.fillText(label, x1 + pad, labelY + fontSize + pad - 2);
      }
    });
  };

  const drawBoundingBox = (result, width, height, currentImg = null) => {
    drawBoxesOnTargetCanvas(canvasRef.current, result, width, height, currentImg);
  };

  const drawVideoBoundingBox = (result, width, height) => {
    drawBoxesOnTargetCanvas(videoCanvasRef.current, result, width, height, null);
  };

  // Tự động quét theo chu kỳ (khi bật camera)
  useEffect(() => {
    let intervalId = null;
    if (isCameraActive && isAutoScan && monitorMode === 'webcam') {
      intervalId = setInterval(() => {
        captureAndDetect();
      }, 2000);
    }
    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [isCameraActive, isAutoScan, threshold, monitorMode]);

  // Xử lý tải ảnh tĩnh từ file
  const handleFileUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (isCameraActive) {
      stopCamera();
    }

    const objectUrl = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      staticImageRef.current = img;
      setStaticImage({
        url: objectUrl,
        width: img.width,
        height: img.height,
        file: file,
      });
      setMonitorMode('image');

      const canvas = canvasRef.current;
      if (canvas) {
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);
      }

      processImageBlob(file, img.width, img.height, img);
    };
    img.src = objectUrl;
    e.target.value = '';
  };

  // Quét lại ảnh tĩnh hiện tại
  const handleRescanStaticImage = () => {
    if (!staticImage?.file || !staticImageRef.current || isProcessing) return;
    processImageBlob(
      staticImage.file,
      staticImage.width,
      staticImage.height,
      staticImageRef.current
    );
  };

  // ==========================================
  // XỬ LÝ VIDEO UPLOAD
  // ==========================================
  const handleVideoSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    loadVideoFile(file);
    e.target.value = '';
  };

  const loadVideoFile = (file) => {
    if (file.size > 500 * 1024 * 1024) {
      setErrorMessage('Kích thước video quá lớn (> 500MB). Vui lòng chọn tệp video dưới 500MB.');
      return;
    }
    if (isCameraActive) {
      stopCamera();
    }
    if (staticImage) {
      clearStaticImage();
    }
    if (videoPreviewUrl) {
      URL.revokeObjectURL(videoPreviewUrl);
    }

    const url = URL.createObjectURL(file);
    setVideoFile(file);
    setVideoPreviewUrl(url);
    setVideoResult(null);
    setSelectedTimelineEvent(null);
    setVideoCurrentTime(0);
    setIsVideoPlaying(false);
    setIsVideoScanningLive(false);
    setLastResult(null);
    setVideoActiveTab('frame');
    clearVideoCanvas();
    setMonitorMode('video');
    setErrorMessage('');
  };

  const clearVideo = () => {
    if (videoPreviewUrl) {
      URL.revokeObjectURL(videoPreviewUrl);
    }
    setVideoFile(null);
    setVideoPreviewUrl(null);
    setVideoResult(null);
    setSelectedTimelineEvent(null);
    setVideoCurrentTime(0);
    setIsVideoPlaying(false);
    setIsVideoScanningLive(false);
    setLastResult(null);
    setVideoActiveTab('frame');
    clearVideoCanvas();
    setErrorMessage('');
  };

  // Chụp khung hình từ video và gửi nhận diện thời gian thực
  const captureAndDetectVideoFrame = async () => {
    const video = uploadedVideoRef.current;
    if (!video || liveScanInProgressRef.current) return;
    if (video.videoWidth === 0 || video.videoHeight === 0) return;

    liveScanInProgressRef.current = true;
    setIsVideoScanningLive(true);

    try {
      let offscreen = offscreenCanvasRef.current;
      if (!offscreen) {
        offscreen = document.createElement('canvas');
        offscreenCanvasRef.current = offscreen;
      }
      offscreen.width = video.videoWidth;
      offscreen.height = video.videoHeight;
      const offCtx = offscreen.getContext('2d');
      offCtx.drawImage(video, 0, 0, offscreen.width, offscreen.height);

      offscreen.toBlob(async (blob) => {
        if (!blob) {
          liveScanInProgressRef.current = false;
          setIsVideoScanningLive(false);
          return;
        }

        try {
          const result = await detectFace(blob, threshold);
          setLastResult(result);

          // Vẽ khung nhận diện lên canvas phủ trên video
          drawVideoBoundingBox(result, video.videoWidth, video.videoHeight);

          if (result.matched && result.hoSo) {
            const now = Date.now();
            if (now - lastAlertTimeRef.current > 4000) {
              lastAlertTimeRef.current = now;
              playAlertSound();
              setAlertData({
                matched: true,
                similarity: result.similarity,
                hoSo: result.hoSo,
                anhChupUrl: result.anhChupUrl,
              });
              if (onNewAlert) onNewAlert(result);
            }
          }
        } catch (err) {
          console.warn('Lỗi nhận diện khung hình video:', err);
        } finally {
          liveScanInProgressRef.current = false;
          setIsVideoScanningLive(false);
        }
      }, 'image/jpeg', 0.85);
    } catch (e) {
      liveScanInProgressRef.current = false;
      setIsVideoScanningLive(false);
    }
  };

  // Đồng bộ khung nhận diện từ kết quả quét timeline đã phân tích trước đó
  const syncTimelineBox = (currentTime) => {
    if (!videoResult?.timeline || videoResult.timeline.length === 0) {
      if (!isVideoPlaying) {
        captureAndDetectVideoFrame();
      }
      return;
    }
    const video = uploadedVideoRef.current;
    if (!video || video.videoWidth === 0) return;

    const matchedEvent = videoResult.timeline.find(
      (ev) => Math.abs(ev.timestamp - currentTime) <= 0.6
    );

    if (matchedEvent) {
      const simulatedResult = {
        matched: true,
        similarity: matchedEvent.similarity,
        hoSo: matchedEvent.person,
        message: `Khớp người thân: ${matchedEvent.person?.hoTen || 'Người thân'} (Mốc ${matchedEvent.timestampFormatted || Math.floor(currentTime) + 's'})`,
        detections: [
          {
            bbox: matchedEvent.bbox,
            matched: true,
            similarity: matchedEvent.similarity,
            hoSo: matchedEvent.person,
          },
        ],
      };
      setLastResult(simulatedResult);
      drawVideoBoundingBox(simulatedResult, video.videoWidth, video.videoHeight);
    } else {
      clearVideoCanvas();
      if (!isVideoPlaying) {
        captureAndDetectVideoFrame();
      }
    }
  };

  // Sự kiện cập nhật thời gian phát video
  const handleVideoTimeUpdate = (e) => {
    const curTime = e.target.currentTime;
    setVideoCurrentTime(curTime);

    if (isVideoPlaying) {
      const now = Date.now();
      // Quét chu kỳ 1.0 giây / lần khi video đang chạy
      if (now - lastLiveScanTimeRef.current >= 1000 && !liveScanInProgressRef.current) {
        lastLiveScanTimeRef.current = now;
        captureAndDetectVideoFrame();
      }
    } else if (videoResult?.timeline) {
      syncTimelineBox(curTime);
    }
  };

  const handleVideoPlay = () => {
    setIsVideoPlaying(true);
    lastLiveScanTimeRef.current = Date.now();
    captureAndDetectVideoFrame();
  };

  const handleVideoPause = () => {
    setIsVideoPlaying(false);
    // Tự động quét khung hình ngay lúc người dùng bấm dừng video
    captureAndDetectVideoFrame();
  };

  const handleVideoEnded = () => {
    setIsVideoPlaying(false);
    captureAndDetectVideoFrame();
  };

  const handleVideoSeeked = (e) => {
    const curTime = e.target.currentTime;
    if (videoResult?.timeline) {
      syncTimelineBox(curTime);
    } else {
      captureAndDetectVideoFrame();
    }
  };

  const handleStartVideoAnalysis = async () => {
    if (!videoFile || isVideoAnalyzing) return;
    setIsVideoAnalyzing(true);
    setAnalysisProgress(5);
    setAnalysisStageText('Khởi tạo phiên phân tích AI...');
    setAnalysisDetailText(`Tệp: ${videoFile.name} (${(videoFile.size / (1024 * 1024)).toFixed(1)} MB)`);
    setAnalysisElapsedTime(0);
    setErrorMessage('');

    // Bộ đếm thời gian thực hiện
    const startTime = Date.now();
    const timerInterval = setInterval(() => {
      const elapsed = Math.floor((Date.now() - startTime) / 1000);
      setAnalysisElapsedTime(elapsed);
    }, 1000);

    let aiSimInterval = null;

    try {
      const result = await detectVideo(videoFile, threshold, frameInterval, (prog) => {
        if (prog.phase === 'upload') {
          // Giai đoạn Upload chiếm từ 5% -> 40% tổng tiến trình
          const uploadPct = Math.round(5 + (prog.percent / 100) * 35);
          setAnalysisProgress(uploadPct);
          const loadedMB = (prog.loaded / (1024 * 1024)).toFixed(1);
          const totalMB = (prog.total / (1024 * 1024)).toFixed(1);
          setAnalysisStageText(`Đang tải video lên máy chủ (${prog.percent}%)...`);
          setAnalysisDetailText(`Đã truyền tải: ${loadedMB} MB / ${totalMB} MB`);

          if (prog.percent >= 100) {
            setAnalysisProgress(45);
            setAnalysisStageText('Máy chủ đã nhận tệp. Đang kích hoạt tiến trình AI...');
            setAnalysisDetailText('YOLOv8 & OpenCV đang chuẩn bị trích xuất khung hình');

            // Giai đoạn AI xử lý: 45% -> 95%
            let currentPct = 45;
            aiSimInterval = setInterval(() => {
              if (currentPct < 65) {
                currentPct += 2.5;
                setAnalysisProgress(Math.floor(currentPct));
                setAnalysisStageText('Bước 1/3: Trích xuất khung hình CCTV và phát hiện khuôn mặt...');
                setAnalysisDetailText('Mô hình YOLOv8-Face đang quét toạ độ khuôn mặt từng giây');
              } else if (currentPct < 85) {
                currentPct += 1.8;
                setAnalysisProgress(Math.floor(currentPct));
                setAnalysisStageText('Bước 2/3: Căn chỉnh 5 điểm giải phẫu & Trích xuất đặc trưng...');
                setAnalysisDetailText('MobileFaceNet đang tính toán vector đặc trưng 512 chiều');
              } else if (currentPct < 96) {
                currentPct += 0.8;
                setAnalysisProgress(Math.floor(currentPct));
                setAnalysisStageText('Bước 3/3: So khớp Cosine Similarity với CSDL FAISS...');
                setAnalysisDetailText('Đang đối sánh với toàn bộ hồ sơ người mất tích đã đăng ký');
              }
            }, 600);
          }
        }
      });

      if (aiSimInterval) clearInterval(aiSimInterval);
      clearInterval(timerInterval);

      // Cập nhật hoàn tất 100%
      setAnalysisProgress(100);
      setAnalysisStageText('✓ Hoàn tất phân tích video thành công!');
      const matchCount = result.uniquePersons?.length || 0;
      setAnalysisDetailText(
        result.matched
          ? `Phát hiện ${matchCount} người thân trong video (Tổng ${result.totalMatches || 0} lượt xuất hiện)`
          : 'Đã quét xong toàn bộ video. Không phát hiện người thân nào trùng khớp.'
      );

      // Dừng 600ms để người dùng thấy trạng thái 100% hoàn thành
      await new Promise((r) => setTimeout(r, 650));

      setVideoResult(result);
      setVideoActiveTab('summary');

      if (result.matched && result.uniquePersons && result.uniquePersons.length > 0) {
        playAlertSound();
        const bestPerson = result.uniquePersons[0];
        setAlertData({
          matched: true,
          similarity: bestPerson.maxSimilarity,
          hoSo: bestPerson.person,
          anhChupUrl: bestPerson.bestSnapshotUrl,
        });
        if (onNewAlert) onNewAlert(result);
      }
    } catch (err) {
      if (aiSimInterval) clearInterval(aiSimInterval);
      clearInterval(timerInterval);
      let msg = err.message || 'Không thể kết nối đến máy chủ.';
      if (msg.includes('Failed to fetch') || msg.includes('NetworkError')) {
        msg = 'Không thể kết nối đến máy chủ backend (Port 8080). Hãy đảm bảo Core Service đang chạy hoặc tệp video không vượt quá 500MB.';
      }
      setErrorMessage('Lỗi phân tích video: ' + msg);
    } finally {
      if (aiSimInterval) clearInterval(aiSimInterval);
      clearInterval(timerInterval);
      setIsVideoAnalyzing(false);
    }
  };

  const jumpToTimestamp = (seconds, event = null) => {
    if (uploadedVideoRef.current) {
      uploadedVideoRef.current.currentTime = seconds;
      uploadedVideoRef.current.play().catch(() => {});
    }
    setSelectedTimelineEvent(event);
    if (event && uploadedVideoRef.current) {
      const video = uploadedVideoRef.current;
      const simulatedResult = {
        matched: true,
        similarity: event.similarity,
        hoSo: event.person,
        message: `Phát hiện ${event.person?.hoTen || 'người thân'} tại mốc ${event.timestampFormatted || seconds + 's'}`,
        detections: [
          {
            bbox: event.bbox,
            matched: true,
            similarity: event.similarity,
            hoSo: event.person,
          },
        ],
      };
      setLastResult(simulatedResult);
      drawVideoBoundingBox(simulatedResult, video.videoWidth, video.videoHeight);
    }
  };

  // Helper hiển thị thẻ trạng thái nhận diện thống nhất cho tất cả các chế độ
  const renderDetectionStatusCard = (result, emptyText) => {
    if (!result) {
      return (
        <div className="empty-result">
          <p>{emptyText || 'Chưa có lượt quét nào. Hãy bật camera hoặc tải tệp lên để bắt đầu nhận diện.'}</p>
        </div>
      );
    }

    return (
      <div className="detection-summary-card">
        <div className="flex items-center gap-2 mb-3">
          {result.matched ? (
            <span className="badge badge-red font-bold">⚠️ TRÙNG KHỚP</span>
          ) : (
            <span className="badge badge-blue">
              ✓ {result.detections && result.detections.length > 0
                ? `Phát hiện ${result.detections.length} khuôn mặt (Chưa khớp)`
                : 'Không trùng khớp'}
            </span>
          )}
          <span className="text-xs text-muted">
            Similarity: {((result.similarity || 0) * 100).toFixed(1)}%
          </span>
        </div>

        <p className="text-sm mb-3">{result.message}</p>

        {/* Danh sách phân loại chi tiết từng khuôn mặt */}
        {result.detections && result.detections.length > 0 && (
          <div className="faces-detection-breakdown">
            <div className="breakdown-title">
              Khuôn mặt phát hiện ({result.detections.length})
            </div>
            <div className="faces-list">
              {result.detections.map((det, idx) => (
                <div
                  key={idx}
                  className={`face-list-item ${det.matched ? 'item-matched' : 'item-unmatched'}`}
                >
                  <div className="face-item-header">
                    <span className={`face-status-tag ${det.matched ? 'tag-matched' : 'tag-unmatched'}`}>
                      {det.matched ? '🚨 TRÙNG KHỚP' : '✓ Chưa khớp'}
                    </span>
                    <span className="face-sim-value">
                      Độ tương đồng: {((det.similarity || 0) * 100).toFixed(1)}%
                    </span>
                  </div>
                  {det.matched && det.hoSo ? (
                    <div className="face-match-info">
                      <span className="font-semibold text-white">{det.hoSo.hoTen}</span>
                      <span className="text-muted text-xs"> &bull; Khu vực: {det.hoSo.khuVuc || 'Chưa rõ'}</span>
                    </div>
                  ) : (
                    <div className="text-xs text-muted">
                      Người lạ / Không có trong CSDL người mất tích
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {result.matched && result.hoSo && (
          <div className="matched-person-card">
            <img
              src={getFullImageUrl(result.hoSo.anhDaiDienUrl)}
              alt="Avatar"
              className="matched-avatar"
            />
            <div className="matched-details">
              <div className="matched-name">{result.hoSo.hoTen}</div>
              <div className="matched-sub">Khu vực: {result.hoSo.khuVuc || 'N/A'}</div>
              {result.hoSo.lienHeNguoiThan && (
                <div className="matched-sub">Email: {result.hoSo.lienHeNguoiThan}</div>
              )}
              {result.hoSo.vectorIdFaiss != null && (
                <div className="matched-sub">FAISS ID: #{result.hoSo.vectorIdFaiss}</div>
              )}
            </div>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="camera-monitor-container">
      {/* Thanh chọn chế độ giám sát chuyên nghiệp */}
      <div className="monitor-mode-selector-bar">
        <div className="segmented-control">
          <button
            className={`segmented-tab ${monitorMode === 'webcam' ? 'active' : ''}`}
            onClick={() => switchMode('webcam')}
          >
            <Video size={16} />
            <span>Webcam Trực Tiếp</span>
          </button>
          <button
            className={`segmented-tab ${monitorMode === 'image' ? 'active' : ''}`}
            onClick={() => switchMode('image')}
          >
            <Upload size={16} />
            <span>Ảnh Hiện Trường</span>
          </button>
          <button
            className={`segmented-tab ${monitorMode === 'video' ? 'active' : ''}`}
            onClick={() => switchMode('video')}
          >
            <Film size={16} />
            <span>Video Giám Sát</span>
            <span className="mode-badge-new">AI</span>
          </button>
        </div>
      </div>

      {/* Cảnh báo khẩn cấp dạng Banner khi phát hiện */}
      {alertData && (
        <div className="emergency-banner alert-pulse">
          <div className="emergency-header">
            <div className="flex items-center gap-3">
              <span className="emergency-icon">🚨</span>
              <div>
                <h3 className="emergency-title">PHÁT HIỆN TRÙNG KHỚP NGƯỜI THÂN!</h3>
                <p className="emergency-subtitle">
                  Hệ thống AI vừa nhận diện chính xác <strong>{alertData.hoSo?.hoTen}</strong>
                </p>
              </div>
            </div>
            <button className="btn btn-outline btn-sm" onClick={() => setAlertData(null)}>
              Đóng cảnh báo
            </button>
          </div>

          <div className="emergency-content">
            <div className="comparison-cards">
              <div className="comparison-card">
                <span className="comparison-label">Ảnh Chân Dung Hồ Sơ</span>
                <img
                  src={getFullImageUrl(alertData.hoSo?.anhDaiDienUrl)}
                  alt="Ảnh hồ sơ"
                  className="comparison-img"
                />
              </div>
              <div className="comparison-divider">
                <div className="match-score">{((alertData.similarity || 0) * 100).toFixed(1)}%</div>
                <span className="match-text">Độ tương đồng</span>
              </div>
              <div className="comparison-card">
                <span className="comparison-label">Ảnh Chụp Thời Điểm Phát Hiện</span>
                <img
                  src={getFullImageUrl(alertData.anhChupUrl)}
                  alt="Ảnh phát hiện"
                  className="comparison-img"
                />
              </div>
            </div>

            <div className="emergency-meta">
              <div>📍 <strong>Khu vực mất tích:</strong> {alertData.hoSo?.khuVuc || 'Chưa rõ'}</div>
              <div>✉️ <strong>Email cảnh báo:</strong> {alertData.hoSo?.lienHeNguoiThan} (Đã gửi email thông báo tự động)</div>
            </div>
          </div>
        </div>
      )}

      {/* Main Viewport & Controls Grid */}
      <div className="grid-layout">
        {/* ======================================================== */}
        {/* KHUNG TRÁI: VIEWPORT CAMERA / ẢNH / VIDEO                */}
        {/* ======================================================== */}
        <div className="glass-panel camera-viewport-panel">
          <div className="panel-header">
            <div className="panel-title">
              {monitorMode === 'video' ? (
                <>
                  <Film size={20} className="text-blue-400" />
                  <span>Trình Phát & Quét Video Giám Sát</span>
                </>
              ) : monitorMode === 'image' ? (
                <>
                  <Upload size={20} className="text-purple-400" />
                  <span>Ảnh Hiện Trường Tĩnh</span>
                </>
              ) : (
                <>
                  <Camera size={20} className="text-blue-400" />
                  <span>Luồng Giám Sát Trực Tiếp</span>
                </>
              )}
            </div>
            <div className="flex items-center gap-2">
              {monitorMode === 'video' && videoResult && (
                <div className={`badge ${videoResult.matched ? 'badge-red' : 'badge-blue'}`}>
                  {videoResult.matched ? `🚨 ${videoResult.totalMatches} lượt khớp` : '✓ Không trùng khớp'}
                </div>
              )}
              {monitorMode === 'webcam' && isCameraActive && (
                <div className="badge badge-green">
                  <span className="status-dot"></span> LIVE
                </div>
              )}
              {monitorMode === 'webcam' && isAutoScan && (
                <div className="badge badge-blue">
                  Tự động quét (2s)
                </div>
              )}
              {monitorMode === 'image' && staticImage && (
                <div className="badge badge-purple">
                  📷 Chế độ ảnh tĩnh
                </div>
              )}
            </div>
          </div>

          {/* VIEWPORT BODY */}
          <div className="video-viewport">
            {/* 1. CHẾ ĐỘ VIDEO GIÁM SÁT */}
            {monitorMode === 'video' && (
              videoPreviewUrl ? (
                <div className="uploaded-video-wrapper">
                  <div className="video-player-display-area">
                    <video
                      ref={uploadedVideoRef}
                      src={videoPreviewUrl}
                      controls
                      playsInline
                      className="video-element active video-player-custom"
                      onPlay={handleVideoPlay}
                      onPause={handleVideoPause}
                      onEnded={handleVideoEnded}
                      onSeeked={handleVideoSeeked}
                      onTimeUpdate={handleVideoTimeUpdate}
                      onLoadedMetadata={(e) => {
                        setVideoDuration(e.target.duration);
                        if (videoCanvasRef.current && e.target.videoWidth) {
                          videoCanvasRef.current.width = e.target.videoWidth;
                          videoCanvasRef.current.height = e.target.videoHeight;
                        }
                      }}
                      onLoadedData={() => {
                        setTimeout(() => {
                          captureAndDetectVideoFrame();
                        }, 350);
                      }}
                    />
                    <canvas
                      ref={videoCanvasRef}
                      className="overlay-canvas video-canvas-overlay"
                    />

                    {isVideoScanningLive && <div className="scanning-line"></div>}

                    {/* HUD Status trên Video */}
                    <div className="video-hud-overlay">
                      {isVideoPlaying ? (
                        <div className="video-hud-pill live-scan">
                          <span className="live-radar-dot animate-pulse"></span>
                          <span>AI ĐANG QUÉT TRỰC TIẾP ({Math.floor(videoCurrentTime)}s)</span>
                        </div>
                      ) : (
                        isVideoScanningLive && (
                          <div className="video-hud-pill live-scan">
                            <span className="live-radar-dot animate-pulse"></span>
                            <span>ĐANG QUÉT KHUNG HÌNH TẠI ĐIỂM DỪNG...</span>
                          </div>
                        )
                      )}
                    </div>
                  </div>

                  {/* Thanh timeline tương tác đánh dấu các mốc phát hiện */}
                  {videoResult && videoResult.durationSeconds > 0 && (
                    <div className="video-interactive-timeline">
                      <div className="timeline-info-row">
                        <span className="text-xs text-muted">
                          ⏱️ Mốc thời gian phát hiện trên video ({videoResult.timeline?.length || 0} điểm):
                        </span>
                        <span className="text-xs font-mono text-blue-400">
                          {Math.floor(videoCurrentTime / 60).toString().padStart(2, '0')}:
                          {Math.floor(videoCurrentTime % 60).toString().padStart(2, '0')} / {Math.floor(videoResult.durationSeconds / 60).toString().padStart(2, '0')}:
                          {Math.floor(videoResult.durationSeconds % 60).toString().padStart(2, '0')}
                        </span>
                      </div>

                      <div className="timeline-track">
                        {/* Thanh chỉ vị trí phát hiện tại */}
                        <div
                          className="timeline-playhead"
                          style={{
                            left: `${Math.min(100, (videoCurrentTime / videoResult.durationSeconds) * 100)}%`,
                          }}
                        />

                        {/* Các mốc điểm (pins/markers) */}
                        {videoResult.timeline?.map((ev, idx) => {
                          const pct = Math.min(100, Math.max(0, (ev.timestamp / videoResult.durationSeconds) * 100));
                          const isSelected = selectedTimelineEvent?.timestamp === ev.timestamp;
                          return (
                            <button
                              key={idx}
                              type="button"
                              className={`timeline-marker-pin ${isSelected ? 'active-pin' : ''}`}
                              style={{ left: `${pct}%` }}
                              title={`[${ev.timestampFormatted}] ${ev.person?.hoTen || 'Người mất tích'} (${(ev.similarity * 100).toFixed(1)}%) - Bấm để tua tới`}
                              onClick={() => jumpToTimestamp(ev.timestamp, ev)}
                            >
                              <span className="marker-dot"></span>
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="camera-placeholder video-dropzone-placeholder">
                  <Film size={52} className="text-blue-400 dropzone-icon" />
                  <h4 className="font-bold text-white text-lg">Tải Lên Video Giám Sát Cần Quét</h4>
                  <p className="text-muted text-sm max-w-md">
                    Hỗ trợ định dạng camera an ninh CCTV, điện thoại: <strong>.mp4, .avi, .mov, .webm, .mkv</strong>
                  </p>
                  <div className="flex gap-2 justify-center mt-3">
                    <button
                      className="btn btn-primary"
                      onClick={() => videoInputRef.current?.click()}
                    >
                      <Upload size={18} /> Chọn Tệp Video
                    </button>
                  </div>
                </div>
              )
            )}

            {/* 2. CHẾ ĐỘ WEBCAM & ẢNH TĨNH */}
            {monitorMode !== 'video' && (
              <>
                <video
                  ref={videoRef}
                  autoPlay
                  playsInline
                  muted
                  className={`video-element ${isCameraActive ? 'active' : ''}`}
                />
                <canvas
                  ref={canvasRef}
                  className={`overlay-canvas ${staticImage ? 'static-mode' : ''}`}
                />

                {isProcessing && <div className="scanning-line"></div>}

                {!isCameraActive && !staticImage && (
                  <div className="camera-placeholder">
                    <VideoOff size={48} className="text-muted" />
                    <p>Webcam chưa được kích hoạt</p>
                    <div className="flex gap-2 justify-center mt-3">
                      <button className="btn btn-primary" onClick={startCamera}>
                        <Video size={18} /> Bật Camera
                      </button>
                      <button
                        className="btn btn-outline"
                        onClick={() => fileInputRef.current?.click()}
                      >
                        <Upload size={18} /> Tải Ảnh Lên
                      </button>
                      <button
                        className="btn btn-outline"
                        onClick={() => {
                          setMonitorMode('video');
                          videoInputRef.current?.click();
                        }}
                      >
                        <Film size={18} /> Quét Bằng Video
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}

            {/* Modal tiến độ phân tích video thời gian thực */}
            {isVideoAnalyzing && (
              <div className="video-analyzing-overlay">
                <div className="analysis-progress-card">
                  <div className="progress-header">
                    <div className="flex items-center gap-3">
                      <div className="spinner-medium"></div>
                      <div>
                        <h4 className="font-bold text-white text-base">Đang Phân Tích Video Giám Sát</h4>
                        <p className="text-muted text-xs">Mô hình AI: YOLOv8-Face + MobileFaceNet + FAISS</p>
                      </div>
                    </div>
                    <div className="elapsed-badge">
                      ⏱️ {Math.floor(analysisElapsedTime / 60).toString().padStart(2, '0')}:
                      {(analysisElapsedTime % 60).toString().padStart(2, '0')}s
                    </div>
                  </div>

                  {/* Thanh tiến độ chính */}
                  <div className="progress-bar-container">
                    <div
                      className="progress-bar-fill"
                      style={{ width: `${analysisProgress}%` }}
                    >
                      <div className="progress-bar-glow"></div>
                    </div>
                  </div>

                  <div className="progress-info-row">
                    <span className="progress-stage-text">{analysisStageText}</span>
                    <span className="progress-percent-text">{analysisProgress}%</span>
                  </div>

                  <div className="progress-detail-box">
                    <span className="detail-icon">⚡</span>
                    <span className="detail-text">{analysisDetailText}</span>
                  </div>

                  {/* Các bước pipeline AI */}
                  <div className="pipeline-steps-grid">
                    <div className={`pipeline-step-item ${analysisProgress >= 40 ? 'completed' : analysisProgress > 0 ? 'active' : ''}`}>
                      <span className="step-num">{analysisProgress >= 40 ? '✓' : '1'}</span>
                      <span className="step-label">Tải Lên Video</span>
                    </div>
                    <div className={`pipeline-step-item ${analysisProgress >= 65 ? 'completed' : analysisProgress >= 40 ? 'active' : ''}`}>
                      <span className="step-num">{analysisProgress >= 65 ? '✓' : '2'}</span>
                      <span className="step-label">Trích Khung Hình</span>
                    </div>
                    <div className={`pipeline-step-item ${analysisProgress >= 85 ? 'completed' : analysisProgress >= 65 ? 'active' : ''}`}>
                      <span className="step-num">{analysisProgress >= 85 ? '✓' : '3'}</span>
                      <span className="step-label">YOLOv8 Nhận Diện</span>
                    </div>
                    <div className={`pipeline-step-item ${analysisProgress >= 95 ? 'completed' : analysisProgress >= 85 ? 'active' : ''}`}>
                      <span className="step-num">{analysisProgress >= 95 ? '✓' : '4'}</span>
                      <span className="step-label">Đối Sánh FAISS</span>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* THANH ĐIỀU KHIỂN DƯỚI VIEWPORT */}
          <div className="camera-controls">
            {/* Chế độ Video */}
            {monitorMode === 'video' && (
              <div className="flex gap-2 flex-wrap items-center justify-between w-full">
                <div className="flex gap-2 items-center flex-wrap">
                  <input
                    type="file"
                    ref={videoInputRef}
                    style={{ display: 'none' }}
                    accept="video/mp4,video/webm,video/quicktime,video/x-msvideo,video/*"
                    onChange={handleVideoSelect}
                  />

                  {videoFile ? (
                    <>
                      <button
                        className="btn btn-primary"
                        onClick={handleStartVideoAnalysis}
                        disabled={isVideoAnalyzing}
                        title="Phân tích toàn bộ video để lập bản đồ timeline và thống kê tất cả các lần xuất hiện"
                      >
                        <RefreshCw size={16} className={isVideoAnalyzing ? 'animate-spin' : ''} />
                        {isVideoAnalyzing ? 'Đang phân tích...' : 'Bắt Đầu Quét Video'}
                      </button>

                      <button
                        className="btn btn-outline"
                        onClick={() => videoInputRef.current?.click()}
                        disabled={isVideoAnalyzing}
                      >
                        <Upload size={16} /> Đổi Video Khác
                      </button>

                      <button
                        className="btn btn-danger-ghost"
                        onClick={clearVideo}
                        disabled={isVideoAnalyzing}
                      >
                        <X size={16} /> Đóng Video
                      </button>
                    </>
                  ) : (
                    <button
                      className="btn btn-primary"
                      onClick={() => videoInputRef.current?.click()}
                    >
                      <Upload size={16} /> Chọn Video Tải Lên
                    </button>
                  )}
                </div>

                {videoFile && (
                  <div className="text-xs text-muted flex items-center gap-2">
                    <span>📹 {videoFile.name}</span>
                    <span>({(videoFile.size / (1024 * 1024)).toFixed(1)} MB)</span>
                  </div>
                )}
              </div>
            )}

            {/* Chế độ Webcam & Ảnh tĩnh */}
            {monitorMode !== 'video' && (
              <div className="flex gap-2 flex-wrap items-center justify-between w-full">
                <div className="flex gap-2 flex-wrap items-center">
                  {isCameraActive && (
                    <>
                      <button className="btn btn-danger" onClick={stopCamera}>
                        <VideoOff size={16} /> Tắt Camera
                      </button>
                      <button
                        className="btn btn-primary"
                        onClick={captureAndDetect}
                        disabled={isProcessing}
                      >
                        <RefreshCw size={16} className={isProcessing ? 'animate-spin' : ''} />
                        {isProcessing ? 'Đang nhận diện...' : 'Quét Ngay'}
                      </button>
                      <button
                        className={`btn ${isAutoScan ? 'btn-danger' : 'btn-outline'}`}
                        onClick={() => setIsAutoScan(!isAutoScan)}
                      >
                        {isAutoScan ? 'Dừng Tự Động Quét' : 'Bật Tự Động Quét'}
                      </button>
                    </>
                  )}

                  {staticImage && (
                    <>
                      <button className="btn btn-primary" onClick={startCamera}>
                        <Video size={16} /> Bật Camera
                      </button>
                      <button
                        className="btn btn-outline"
                        onClick={handleRescanStaticImage}
                        disabled={isProcessing}
                        title="Nhận diện lại ảnh với ngưỡng hiện tại"
                      >
                        <RefreshCw size={16} className={isProcessing ? 'animate-spin' : ''} />
                        {isProcessing ? 'Đang nhận diện...' : 'Nhận diện lại'}
                      </button>
                      <button
                        className="btn btn-danger-ghost"
                        onClick={clearStaticImage}
                        disabled={isProcessing}
                      >
                        <X size={16} /> Đóng ảnh
                      </button>
                    </>
                  )}

                  {!isCameraActive && !staticImage && (
                    <button className="btn btn-primary" onClick={startCamera}>
                      <Video size={16} /> Bật Camera
                    </button>
                  )}
                </div>

                <div className="flex gap-2 items-center">
                  <input
                    type="file"
                    ref={fileInputRef}
                    style={{ display: 'none' }}
                    accept="image/*"
                    onChange={handleFileUpload}
                  />
                  <button
                    className="btn btn-outline"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={isProcessing}
                  >
                    <Upload size={16} /> {staticImage ? 'Tải ảnh khác' : 'Thử nghiệm bằng ảnh tĩnh'}
                  </button>
                </div>
              </div>
            )}
          </div>

          {errorMessage && (
            <div className="error-alert">
              <AlertTriangle size={16} />
              <span>{errorMessage}</span>
            </div>
          )}
        </div>

        {/* ======================================================== */}
        {/* KHUNG PHẢI: BẢNG ĐIỀU KHIỂN & KẾT QUẢ NHẬN DIỆN         */}
        {/* ======================================================== */}
        <div className="glass-panel result-panel">
          <div className="panel-header">
            <div className="panel-title">
              <Sliders size={18} className="text-blue-400" />
              <span>
                {monitorMode === 'video' ? 'Trạng Thái & Báo Cáo Video' : 'Trạng Thái Nhận Diện'}
              </span>
            </div>
          </div>

          <div className="panel-body">
            {/* 1. Cấu hình ngưỡng tương đồng */}
            <div className="form-group mb-4">
              <div className="flex justify-between items-center mb-1">
                <label className="form-label">Ngưỡng tương đồng (Threshold):</label>
                <span className="font-bold text-blue-400">{(threshold * 100).toFixed(0)}%</span>
              </div>
              <input
                type="range"
                min="0.30"
                max="0.80"
                step="0.05"
                value={threshold}
                onChange={(e) => setThreshold(parseFloat(e.target.value))}
                className="slider-input"
              />
              <div className="flex justify-between text-xs text-muted">
                <span>30% (Nhạy)</span>
                <span>45% (Chuẩn)</span>
                <span>80% (Khắt khe)</span>
              </div>
            </div>

            {/* 2. Cấu hình chu kỳ quét cho video */}
            {monitorMode === 'video' && (
              <div className="form-group mb-4">
                <label className="form-label mb-1">Chu kỳ trích xuất khung hình:</label>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { value: 0.5, label: '0.5 giây', desc: 'Quét kỹ' },
                    { value: 1.0, label: '1.0 giây', desc: 'Khuyên dùng' },
                    { value: 2.0, label: '2.0 giây', desc: 'Siêu tốc' },
                  ].map((opt) => (
                    <button
                      key={opt.value}
                      type="button"
                      className={`btn btn-sm ${frameInterval === opt.value ? 'btn-primary' : 'btn-outline'}`}
                      onClick={() => setFrameInterval(opt.value)}
                    >
                      <div className="text-center w-full">
                        <div className="font-bold">{opt.label}</div>
                        <div className="text-[10px] opacity-75">{opt.desc}</div>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* ================================================== */}
            {/* KẾT QUẢ CHẾ ĐỘ VIDEO                              */}
            {/* ================================================== */}
            {monitorMode === 'video' && (
              videoResult ? (
                <div>
                  {/* Thanh chuyển Sub-tab khi đã có kết quả quét toàn bộ video */}
                  <div className="video-subtabs-nav mb-3">
                    <button
                      type="button"
                      className={`subtab-btn ${videoActiveTab === 'frame' ? 'active' : ''}`}
                      onClick={() => setVideoActiveTab('frame')}
                    >
                      <Scan size={14} />
                      <span>Khung Hình Video</span>
                      {lastResult?.matched && <span className="tab-alert-badge">!</span>}
                    </button>
                    <button
                      type="button"
                      className={`subtab-btn ${videoActiveTab === 'summary' ? 'active' : ''}`}
                      onClick={() => setVideoActiveTab('summary')}
                    >
                      <Film size={14} />
                      <span>Báo Cáo Toàn Video ({videoResult.uniquePersons?.length || 0})</span>
                    </button>
                  </div>

                  {videoActiveTab === 'frame' ? (
                    <div>
                      <div className="video-current-time-hud mb-2">
                        <span>⏱️ Vị trí khung hình đang xem:</span>
                        <strong className="text-blue-400 font-mono ml-1">
                          {Math.floor(videoCurrentTime / 60).toString().padStart(2, '0')}:
                          {Math.floor(videoCurrentTime % 60).toString().padStart(2, '0')} ({videoCurrentTime.toFixed(1)}s)
                        </strong>
                      </div>
                      {renderDetectionStatusCard(
                        lastResult,
                        'Bấm nút "Phát" hoặc "Tạm dừng" video để nhận diện khuôn mặt tại khung hình đó.'
                      )}
                    </div>
                  ) : (
                    <div className="video-results-container">
                      {/* Tóm tắt thông số quét video */}
                      <div className="video-stats-grid mb-3">
                        <div className="stat-card">
                          <div className="stat-value">{videoResult.durationSeconds?.toFixed(1)}s</div>
                          <div className="stat-label">Thời lượng</div>
                        </div>
                        <div className="stat-card">
                          <div className="stat-value">{videoResult.processedFrames}</div>
                          <div className="stat-label">Khung hình quét</div>
                        </div>
                        <div className="stat-card">
                          <div className="stat-value text-blue-400">{videoResult.totalFacesDetected}</div>
                          <div className="stat-label">Mặt phát hiện</div>
                        </div>
                        <div className="stat-card">
                          <div className={`stat-value ${videoResult.matched ? 'text-red-400 font-bold' : 'text-gray-400'}`}>
                            {videoResult.uniquePersons?.length || 0}
                          </div>
                          <div className="stat-label">Người thân</div>
                        </div>
                      </div>

                      {/* Banner tóm tắt */}
                      <div className="flex items-center gap-2 mb-3">
                        {videoResult.matched ? (
                          <span className="badge badge-red font-bold">
                            ⚠️ PHÁT HIỆN {videoResult.uniquePersons?.length} NGƯỜI THÂN TRONG VIDEO
                          </span>
                        ) : (
                          <span className="badge badge-blue">
                            ✓ Không phát hiện người mất tích trong video
                          </span>
                        )}
                      </div>

                      {/* Danh sách người mất tích tìm thấy */}
                      {videoResult.uniquePersons && videoResult.uniquePersons.length > 0 && (
                        <div className="unique-persons-section mb-4">
                          <div className="breakdown-title mb-2">Người Thân Phát Hiện Được:</div>
                          {videoResult.uniquePersons.map((p, pIdx) => (
                            <div key={pIdx} className="matched-person-card mb-2">
                              <img
                                src={getFullImageUrl(p.person?.anhDaiDienUrl)}
                                alt="Avatar"
                                className="matched-avatar"
                              />
                              <div className="matched-details flex-1">
                                <div className="flex justify-between items-start">
                                  <div className="matched-name">{p.person?.hoTen}</div>
                                  <span className="badge badge-red text-xs">
                                    {(p.maxSimilarity * 100).toFixed(1)}%
                                  </span>
                                </div>
                                <div className="matched-sub">Khu vực: {p.person?.khuVuc || 'N/A'}</div>
                                <div className="matched-sub">Xuất hiện: {p.occurrencesCount} lần</div>

                                {/* Mốc thời gian có thể bấm để tua video */}
                                <div className="timestamps-chips mt-2">
                                  <span className="text-[11px] text-muted mr-1">Tua tới giây:</span>
                                  {p.timestamps?.map((ts, tsIdx) => {
                                    const [m, s] = ts.split(':').map(Number);
                                    const sec = (m || 0) * 60 + (s || 0);
                                    const matchedEv = videoResult.timeline?.find(
                                      (ev) => Math.abs(ev.timestamp - sec) <= 1.0
                                    );
                                    return (
                                      <button
                                        key={tsIdx}
                                        type="button"
                                        className="timestamp-chip"
                                        onClick={() =>
                                          jumpToTimestamp(
                                            sec,
                                            matchedEv || {
                                              timestamp: sec,
                                              timestampFormatted: ts,
                                              similarity: p.maxSimilarity,
                                              person: p.person,
                                              bbox: [0, 0, 0, 0],
                                            }
                                          )
                                        }
                                        title={`Tua video tới ${ts}`}
                                      >
                                        <Clock size={10} />
                                        <span>{ts}</span>
                                      </button>
                                    );
                                  })}
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}

                      {/* Dòng thời gian các sự kiện phát hiện (Timeline Events) */}
                      {videoResult.timeline && videoResult.timeline.length > 0 && (
                        <div className="timeline-events-section">
                          <div className="breakdown-title mb-2">
                            Chi Tiết Từng Lượt Xuất Hiện ({videoResult.timeline.length}):
                          </div>
                          <div className="timeline-events-list">
                            {videoResult.timeline.map((ev, evIdx) => (
                              <div
                                key={evIdx}
                                className={`timeline-event-card ${selectedTimelineEvent === ev ? 'selected-event' : ''}`}
                                onClick={() => jumpToTimestamp(ev.timestamp, ev)}
                              >
                                <div className="event-snapshot-thumb">
                                  <img
                                    src={ev.snapshotUrl ? getFullImageUrl(ev.snapshotUrl) : ev.snapshotBase64}
                                    alt="Snapshot"
                                  />
                                </div>
                                <div className="event-info">
                                  <div className="flex items-center justify-between">
                                    <span className="event-timestamp">
                                      ⏱️ {ev.timestampFormatted} ({ev.timestamp}s)
                                    </span>
                                    <span className="event-sim font-bold text-red-400">
                                      {(ev.similarity * 100).toFixed(1)}%
                                    </span>
                                  </div>
                                  <div className="event-name font-semibold text-white text-xs truncate">
                                    {ev.person?.hoTen || 'Người mất tích'}
                                  </div>
                                  <div className="text-[11px] text-blue-400 flex items-center gap-1 mt-1">
                                    <Play size={10} /> Bấm để tua video tới mốc này
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ) : (
                <div>
                  <div className="video-current-time-hud mb-2">
                    <span>⏱️ Vị trí khung hình đang xem:</span>
                    <strong className="text-blue-400 font-mono ml-1">
                      {Math.floor(videoCurrentTime / 60).toString().padStart(2, '0')}:
                      {Math.floor(videoCurrentTime % 60).toString().padStart(2, '0')} ({videoCurrentTime.toFixed(1)}s)
                    </strong>
                  </div>

                  {renderDetectionStatusCard(
                    lastResult,
                    'Bấm nút "Phát" hoặc "Tạm dừng" video để hệ thống tự động nhận diện khuôn mặt tại khung hình đó.'
                  )}

                  <div className="video-fullscan-hint mt-3">
                    <Film size={16} className="text-blue-400 shrink-0 mt-0.5" />
                    <span>
                      Để AI quét tự động từng giây toàn bộ thời lượng video và xuất dòng thời gian, hãy bấm{' '}
                      <strong>"Bắt Đầu Quét Video"</strong> ở thanh bên trái.
                    </span>
                  </div>
                </div>
              )
            )}

            {/* ================================================== */}
            {/* KẾT QUẢ CHẾ ĐỘ WEBCAM & ẢNH TĨNH                  */}
            {/* ================================================== */}
            {monitorMode !== 'video' && (
              renderDetectionStatusCard(
                lastResult,
                monitorMode === 'webcam'
                  ? 'Chưa có lượt quét nào. Hãy bật camera hoặc bật chế độ tự động quét.'
                  : 'Chưa có lượt quét nào. Hãy tải ảnh lên để nhận diện.'
              )
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
