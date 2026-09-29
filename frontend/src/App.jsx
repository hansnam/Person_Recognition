import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import CameraMonitor from './components/CameraMonitor';
import MissingProfiles from './components/MissingProfiles';
import DetectionLogs from './components/DetectionLogs';
import LoginModal from './components/LoginModal';
import './App.css';

export default function App() {
  const [activeTab, setActiveTab] = useState('camera');
  const [user, setUser] = useState(null);
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [alertCount, setAlertCount] = useState(0);

  // Khôi phục phiên đăng nhập khi mở trang
  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    const token = localStorage.getItem('token');
    if (savedUser && token) {
      try {
        setUser(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
      }
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  const handleLoginSuccess = (userData) => {
    setUser(userData);
  };

  const handleNewAlert = (alert) => {
    setAlertCount((prev) => prev + 1);
  };

  return (
    <div className="app-container">
      {/* Navigation Header */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        user={user}
        onOpenLogin={() => setIsLoginModalOpen(true)}
        onLogout={handleLogout}
      />

      {/* Main Content Body */}
      <main className="main-content">
        {activeTab === 'camera' && (
          <CameraMonitor onNewAlert={handleNewAlert} />
        )}

        {activeTab === 'profiles' && (
          <MissingProfiles
            user={user}
            onRequireLogin={() => setIsLoginModalOpen(true)}
          />
        )}

        {activeTab === 'logs' && (
          <DetectionLogs user={user} />
        )}
      </main>

      {/* Login Modal */}
      <LoginModal
        isOpen={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onLoginSuccess={handleLoginSuccess}
      />

      {/* Footer */}
      <footer className="app-footer">
        <div className="max-w-6xl mx-auto">
          <p className="font-semibold text-gray-300">
            Đồ Án: Hệ Thống Thông Báo Phát Hiện Người Mất Tích Bằng AI
          </p>
          <p className="mt-1 text-gray-500">
            Kiến trúc Microservice: Python FastAPI (YOLOv8-Face + MobileFaceNet + FAISS) &bull; Java Spring Boot &bull; MySQL &bull; React.js (Vite)
          </p>
        </div>
      </footer>
    </div>
  );
}
