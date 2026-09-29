import React from 'react';
import { Camera, Users, History, Shield, LogOut, LogIn, AlertCircle } from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab, user, onOpenLogin, onLogout }) {
  return (
    <header className="navbar-container">
      <div className="navbar-content">
        {/* Logo & Brand */}
        <div className="navbar-brand">
          <div className="brand-icon">
            <Shield size={22} className="text-blue-400" />
          </div>
          <div>
            <div className="brand-title">FINDME AI</div>
            <div className="brand-subtitle">Hệ Thống Tìm Kiếm Người Mất Tích</div>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="navbar-tabs">
          <button
            className={`nav-tab ${activeTab === 'camera' ? 'active' : ''}`}
            onClick={() => setActiveTab('camera')}
          >
            <Camera size={18} />
            <span>Giám Sát Camera</span>
          </button>

          <button
            className={`nav-tab ${activeTab === 'profiles' ? 'active' : ''}`}
            onClick={() => setActiveTab('profiles')}
          >
            <Users size={18} />
            <span>Hồ Sơ Tìm Kiếm</span>
          </button>

          <button
            className={`nav-tab ${activeTab === 'logs' ? 'active' : ''}`}
            onClick={() => setActiveTab('logs')}
          >
            <History size={18} />
            <span>Lịch Sử Cảnh Báo</span>
          </button>
        </nav>

        {/* Right Actions: Status & User */}
        <div className="navbar-actions">
          <div className="badge badge-green">
            <span className="status-dot"></span>
            AI Online
          </div>

          {user ? (
            <div className="user-profile">
              <span className="user-email">{user.email}</span>
              <span className="badge badge-blue">{user.vaiTro}</span>
              <button
                className="btn btn-outline btn-icon"
                title="Đăng xuất"
                onClick={onLogout}
              >
                <LogOut size={16} />
              </button>
            </div>
          ) : (
            <button className="btn btn-primary" onClick={onOpenLogin}>
              <LogIn size={16} />
              <span>Đăng nhập</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
