import React, { useState } from 'react';
import { Lock, Mail, X, ShieldAlert, Check } from 'lucide-react';
import { login } from '../services/api';

export default function LoginModal({ isOpen, onClose, onLoginSuccess }) {
  const [email, setEmail] = useState('admin@gmail.com');
  const [matKhau, setMatKhau] = useState('123456');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage('');
    try {
      const data = await login(email, matKhau);
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify({ email: data.email, vaiTro: data.vaiTro }));
      onLoginSuccess({ email: data.email, vaiTro: data.vaiTro });
      onClose();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFillDefaultAdmin = () => {
    setEmail('admin@gmail.com');
    setMatKhau('123456');
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content glass-panel max-w-md">
        <div className="modal-header">
          <div className="flex items-center gap-2">
            <Lock size={18} className="text-blue-400" />
            <h3 className="modal-title">Đăng Nhập Quản Trị Viên</h3>
          </div>
          <button className="modal-close" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          {errorMessage && (
            <div className="error-alert mb-3">
              <ShieldAlert size={16} />
              <span>{errorMessage}</span>
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Email quản trị viên:</label>
            <div className="input-icon-wrap">
              <input
                type="email"
                required
                className="form-input"
                placeholder="admin@gmail.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Mật khẩu:</label>
            <div className="input-icon-wrap">
              <input
                type="password"
                required
                className="form-input"
                placeholder="••••••••"
                value={matKhau}
                onChange={(e) => setMatKhau(e.target.value)}
              />
            </div>
          </div>

          {/* Nút điền sẵn tài khoản mặc định */}
          <div className="default-account-hint">
            <span>Tài khoản đồ án mặc định: </span>
            <button
              type="button"
              className="text-blue-400 underline font-semibold text-xs"
              onClick={handleFillDefaultAdmin}
            >
              admin@gmail.com / 123456
            </button>
          </div>

          <div className="modal-footer mt-4">
            <button type="button" className="btn btn-outline" onClick={onClose}>
              Đóng
            </button>
            <button type="submit" className="btn btn-primary" disabled={isLoading}>
              {isLoading ? 'Đang xác thực...' : 'Đăng nhập'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
