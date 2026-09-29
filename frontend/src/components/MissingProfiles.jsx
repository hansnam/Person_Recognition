import React, { useState, useEffect } from 'react';
import { Search, Plus, Trash2, Calendar, MapPin, Mail, Upload, X, AlertCircle } from 'lucide-react';
import { getProfiles, createProfile, deleteProfile, getFullImageUrl } from '../services/api';

export default function MissingProfiles({ user, onRequireLogin }) {
  const [profiles, setProfiles] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Form State
  const [formHoTen, setFormHoTen] = useState('');
  const [formNgayMatTich, setFormNgayMatTich] = useState('');
  const [formKhuVuc, setFormKhuVuc] = useState('');
  const [formLienHe, setFormLienHe] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Tải danh sách hồ sơ
  const loadProfiles = async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const data = await getProfiles(keyword);
      setProfiles(data);
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadProfiles();
  }, [keyword]);

  // Chọn ảnh chân dung
  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
    }
  };

  // Mở modal thêm mới
  const handleOpenAddModal = () => {
    if (!user) {
      onRequireLogin();
      return;
    }
    setFormHoTen('');
    setFormNgayMatTich('');
    setFormKhuVuc('');
    setFormLienHe('');
    setSelectedFile(null);
    setPreviewUrl('');
    setIsModalOpen(true);
  };

  // Submit đăng ký hồ sơ
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedFile) {
      alert('Vui lòng chọn ảnh chân dung rõ mặt');
      return;
    }

    setIsSubmitting(true);
    try {
      const formData = new FormData();
      formData.append('hoTen', formHoTen);
      if (formNgayMatTich) formData.append('ngayMatTich', formNgayMatTich);
      if (formKhuVuc) formData.append('khuVuc', formKhuVuc);
      formData.append('lienHeNguoiThan', formLienHe);
      formData.append('file', selectedFile);

      await createProfile(formData);
      setIsModalOpen(false);
      loadProfiles();
    } catch (err) {
      alert('Lỗi tạo hồ sơ: ' + err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Xoá hồ sơ
  const handleDelete = async (id, hoTen) => {
    if (!user) {
      onRequireLogin();
      return;
    }
    if (window.confirm(`Bạn có chắc muốn xoá hồ sơ của "${hoTen}"? Dữ liệu khuôn mặt trong FAISS cũng sẽ được xoá.`)) {
      try {
        await deleteProfile(id);
        loadProfiles();
      } catch (err) {
        alert('Lỗi xoá hồ sơ: ' + err.message);
      }
    }
  };

  return (
    <div className="profiles-container">
      {/* Thanh công cụ tìm kiếm và nút Thêm mới */}
      <div className="profiles-toolbar">
        <div className="search-bar">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            placeholder="Tìm kiếm theo họ và tên..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className="search-input"
          />
        </div>

        <button className="btn btn-primary" onClick={handleOpenAddModal}>
          <Plus size={18} />
          <span>Thêm Hồ Sơ Tìm Kiếm</span>
        </button>
      </div>

      {errorMessage && (
        <div className="error-alert mb-4">
          <AlertCircle size={16} />
          <span>{errorMessage} (Bạn cần đăng nhập để xem danh sách hồ sơ được bảo mật)</span>
        </div>
      )}

      {/* Lưới danh sách hồ sơ */}
      {isLoading ? (
        <div className="loading-state">Đang tải danh sách hồ sơ...</div>
      ) : profiles.length === 0 ? (
        <div className="empty-state">
          <p>Không tìm thấy hồ sơ người mất tích nào.</p>
        </div>
      ) : (
        <div className="profiles-grid">
          {profiles.map((p) => (
            <div key={p.id} className="glass-panel profile-card">
              <div className="profile-img-wrap">
                <img
                  src={getFullImageUrl(p.anhDaiDienUrl)}
                  alt={p.hoTen}
                  className="profile-img"
                />
                <span className="profile-faiss-badge">FAISS #{p.vectorIdFaiss}</span>
              </div>

              <div className="profile-body">
                <h3 className="profile-name">{p.hoTen}</h3>

                <div className="profile-info-list">
                  <div className="profile-info-item">
                    <Calendar size={14} className="text-muted" />
                    <span>Ngày mất tích: {p.ngayMatTich || 'Chưa rõ'}</span>
                  </div>

                  <div className="profile-info-item">
                    <MapPin size={14} className="text-muted" />
                    <span>Khu vực: {p.khuVuc || 'Chưa rõ'}</span>
                  </div>

                  <div className="profile-info-item">
                    <Mail size={14} className="text-muted" />
                    <span className="truncate">Liên hệ: {p.lienHeNguoiThan}</span>
                  </div>
                </div>

                <div className="profile-actions">
                  <button
                    className="btn btn-danger btn-sm w-full"
                    onClick={() => handleDelete(p.id, p.hoTen)}
                  >
                    <Trash2 size={14} /> Xoá hồ sơ
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Thêm Mới Hồ Sơ */}
      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content glass-panel">
            <div className="modal-header">
              <h3 className="modal-title">Đăng Ký Hồ Sơ Người Mất Tích</h3>
              <button className="modal-close" onClick={() => setIsModalOpen(false)}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="modal-body">
              {/* Tải ảnh chân dung */}
              <div className="form-group">
                <label className="form-label">Ảnh chân dung (YOLOv8 & ArcFace trích xuất đặc trưng): *</label>
                <div
                  className="upload-dropzone"
                  onClick={() => document.getElementById('portraitInput')?.click()}
                >
                  {previewUrl ? (
                    <img src={previewUrl} alt="Preview" className="preview-img" />
                  ) : (
                    <div className="dropzone-placeholder">
                      <Upload size={32} className="text-muted mb-2" />
                      <p className="font-semibold">Bấm để chọn ảnh chân dung</p>
                      <p className="text-xs text-muted">Hỗ trợ JPG, PNG (khuôn mặt rõ nét)</p>
                    </div>
                  )}
                  <input
                    id="portraitInput"
                    type="file"
                    accept="image/*"
                    style={{ display: 'none' }}
                    onChange={handleFileChange}
                  />
                </div>
              </div>

              {/* Thông tin hồ sơ */}
              <div className="form-group">
                <label className="form-label">Họ và tên: *</label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Nguyễn Văn A"
                  className="form-input"
                  value={formHoTen}
                  onChange={(e) => setFormHoTen(e.target.value)}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="form-group">
                  <label className="form-label">Ngày mất tích:</label>
                  <input
                    type="date"
                    className="form-input"
                    value={formNgayMatTich}
                    onChange={(e) => setFormNgayMatTich(e.target.value)}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Khu vực / Địa bàn:</label>
                  <input
                    type="text"
                    placeholder="Ví dụ: Quận Cầu Giấy, Hà Nội"
                    className="form-input"
                    value={formKhuVuc}
                    onChange={(e) => setFormKhuVuc(e.target.value)}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Email người thân (nhận cảnh báo khẩn cấp): *</label>
                <input
                  type="email"
                  required
                  placeholder="nguoithan@gmail.com"
                  className="form-input"
                  value={formLienHe}
                  onChange={(e) => setFormLienHe(e.target.value)}
                />
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setIsModalOpen(false)}
                >
                  Huỷ bỏ
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? 'Đang trích xuất vector & lưu...' : 'Lưu hồ sơ'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
