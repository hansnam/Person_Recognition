import React, { useState, useEffect } from 'react';
import { History, RefreshCw, AlertCircle, Calendar, Clock, ExternalLink } from 'lucide-react';
import { getLogs, getFullImageUrl } from '../services/api';

export default function DetectionLogs({ user }) {
  const [logs, setLogs] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [selectedImage, setSelectedImage] = useState(null);

  const loadLogs = async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const data = await getLogs();
      setLogs(data);
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, []);

  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      return d.toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="logs-container">
      <div className="logs-header">
        <div>
          <h2 className="text-xl font-bold flex items-center gap-2">
            <History size={22} className="text-blue-400" />
            <span>Nhật Ký & Lịch Sử Cảnh Báo Phát Hiện</span>
          </h2>
          <p className="text-sm text-muted">
            Toàn bộ các lần camera giám sát phát hiện khuôn mặt trùng khớp với hồ sơ tìm kiếm
          </p>
        </div>

        <button className="btn btn-outline" onClick={loadLogs} disabled={isLoading}>
          <RefreshCw size={16} className={isLoading ? 'animate-spin' : ''} />
          Làm mới
        </button>
      </div>

      {errorMessage && (
        <div className="error-alert mb-4">
          <AlertCircle size={16} />
          <span>{errorMessage} (Bạn cần đăng nhập để xem lịch sử được bảo mật)</span>
        </div>
      )}

      {isLoading ? (
        <div className="loading-state">Đang tải lịch sử cảnh báo...</div>
      ) : logs.length === 0 ? (
        <div className="empty-state">
          <p>Chưa có lượt phát hiện người mất tích nào được ghi nhận.</p>
        </div>
      ) : (
        <div className="glass-panel logs-table-wrap">
          <table className="logs-table">
            <thead>
              <tr>
                <th>Ảnh Chụp Camera</th>
                <th>Người Phát Hiện</th>
                <th>Thời Gian</th>
                <th>Độ Tin Cậy (Similarity)</th>
                <th>Email Người Thân</th>
                <th>Trạng Thái Cảnh Báo</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td>
                    {log.anhChupUrl ? (
                      <img
                        src={getFullImageUrl(log.anhChupUrl)}
                        alt="Camera snapshot"
                        className="log-thumbnail"
                        onClick={() => setSelectedImage(getFullImageUrl(log.anhChupUrl))}
                        title="Bấm để xem ảnh lớn"
                      />
                    ) : (
                      <span className="text-muted text-xs">Không có ảnh</span>
                    )}
                  </td>
                  <td>
                    <div className="font-semibold text-white">{log.hoTenNguoiMatTich}</div>
                    <div className="text-xs text-muted">Mã hồ sơ: #{log.nguoiMatTichId}</div>
                  </td>
                  <td>
                    <div className="text-sm">{formatDateTime(log.thoiGian)}</div>
                  </td>
                  <td>
                    <div className="flex items-center gap-2">
                      <div className="progress-bar-bg">
                        <div
                          className="progress-bar-fill"
                          style={{ width: `${Math.min(100, log.doTinCay * 100)}%` }}
                        ></div>
                      </div>
                      <span className="font-mono font-bold text-sm">
                        {(log.doTinCay * 100).toFixed(1)}%
                      </span>
                    </div>
                  </td>
                  <td>
                    <span className="text-sm text-secondary">{log.lienHeNguoiThan}</span>
                  </td>
                  <td>
                    <span className="badge badge-green">
                      ✓ Đã gửi Email
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal phóng to ảnh chụp */}
      {selectedImage && (
        <div className="modal-overlay" onClick={() => setSelectedImage(null)}>
          <div className="image-modal-content" onClick={(e) => e.stopPropagation()}>
            <img src={selectedImage} alt="Ảnh camera phóng to" className="enlarged-img" />
            <button className="btn btn-primary mt-3" onClick={() => setSelectedImage(null)}>
              Đóng
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
