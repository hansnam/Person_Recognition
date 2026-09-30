// Sử dụng relative URL '/api' để mọi yêu cầu qua proxy của Vite (tránh hoàn toàn lỗi CORS / Failed to fetch)
const API_BASE_URL = '/api';
export const SERVER_BASE_URL = '';

export function getFullImageUrl(path) {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('data:')) {
    return path;
  }
  return `${SERVER_BASE_URL}${path.startsWith('/') ? '' : '/'}${path}`;
}

export function getAuthHeader() {
  const token = localStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// 1. Auth API
export async function login(email, matKhau) {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, matKhau }),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Đăng nhập không thành công');
  }
  return data;
}

// 2. Missing Profiles API
export async function getProfiles(keyword = '') {
  const url = keyword
    ? `${API_BASE_URL}/nguoi-mat-tich?keyword=${encodeURIComponent(keyword)}`
    : `${API_BASE_URL}/nguoi-mat-tich`;

  const response = await fetch(url, {
    headers: {
      ...getAuthHeader(),
    },
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Không thể tải danh sách hồ sơ');
  }
  return data;
}

export async function createProfile(formData) {
  const response = await fetch(`${API_BASE_URL}/nguoi-mat-tich`, {
    method: 'POST',
    headers: {
      ...getAuthHeader(),
    },
    body: formData,
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Không thể đăng ký hồ sơ');
  }
  return data;
}

export async function deleteProfile(id) {
  const response = await fetch(`${API_BASE_URL}/nguoi-mat-tich/${id}`, {
    method: 'DELETE',
    headers: {
      ...getAuthHeader(),
    },
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Không thể xoá hồ sơ');
  }
  return data;
}

// 3. Detection API (Camera Stream)
export async function detectFace(fileBlob, threshold = 0.45) {
  const formData = new FormData();
  formData.append('file', fileBlob, 'frame.jpg');

  const response = await fetch(
    `${API_BASE_URL}/detection/match?threshold=${threshold}`,
    {
      method: 'POST',
      body: formData,
    }
  );

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Lỗi nhận diện khuôn mặt');
  }
  return data;
}

// 3b. Video Detection API (hỗ trợ theo dõi tiến độ tải lên thực tế)
export function detectVideo(fileBlob, threshold = 0.45, frameInterval = 1.0, onProgress = null) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const safeFilename = fileBlob.name ? fileBlob.name.replace(/[^a-zA-Z0-9._-]/g, '_') : 'video.mp4';
    const formData = new FormData();
    formData.append('file', fileBlob, safeFilename);

    xhr.open(
      'POST',
      `${API_BASE_URL}/detection/match-video?threshold=${threshold}&frameInterval=${frameInterval}`
    );

    // Theo dõi tiến độ upload thực tế theo dung lượng byte
    if (xhr.upload && onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable && e.total > 0) {
          const percent = Math.round((e.loaded / e.total) * 100);
          onProgress({
            phase: 'upload',
            percent,
            loaded: e.loaded,
            total: e.total,
          });
        }
      };
    }

    xhr.onload = () => {
      let data;
      try {
        data = JSON.parse(xhr.responseText);
      } catch (err) {
        return reject(new Error(`Máy chủ trả về phản hồi không hợp lệ (mã lỗi ${xhr.status})`));
      }

      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(data);
      } else {
        reject(new Error(data.message || `Lỗi nhận diện video (mã lỗi ${xhr.status})`));
      }
    };

    xhr.onerror = () => {
      reject(new Error('Không thể kết nối đến máy chủ hoặc tệp video bị gián đoạn.'));
    };

    xhr.ontimeout = () => {
      reject(new Error('Hết thời gian chờ phản hồi từ máy chủ (Timeout sau 5 phút).'));
    };
    xhr.timeout = 300000; // 5 phút

    xhr.send(formData);
  });
}

// 3c. Fusion Detection API (Face + Body Re-ID)
export async function detectFusion(fileBlob, faceThreshold = 0.45, bodyThreshold = 0.65) {
  const formData = new FormData();
  formData.append('file', fileBlob, 'frame.jpg');

  const response = await fetch(
    `${API_BASE_URL}/detection/match/fusion?faceThreshold=${faceThreshold}&bodyThreshold=${bodyThreshold}`,
    {
      method: 'POST',
      body: formData,
    }
  );

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Lỗi nhận diện Fusion');
  }
  return data;
}

// 3d. Fusion Video Detection API (Face + Body Re-ID Video Stream)
export function detectFusionVideo(fileBlob, faceThreshold = 0.45, bodyThreshold = 0.65, frameInterval = 1.0, onProgress = null) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const safeFilename = fileBlob.name ? fileBlob.name.replace(/[^a-zA-Z0-9._-]/g, '_') : 'fusion_video.mp4';
    const formData = new FormData();
    formData.append('file', fileBlob, safeFilename);

    xhr.open(
      'POST',
      `${API_BASE_URL}/detection/match/fusion-video?faceThreshold=${faceThreshold}&bodyThreshold=${bodyThreshold}&frameInterval=${frameInterval}`
    );

    if (xhr.upload && onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable && e.total > 0) {
          const percent = Math.round((e.loaded / e.total) * 100);
          onProgress({
            phase: 'upload',
            percent,
            loaded: e.loaded,
            total: e.total,
          });
        }
      };
    }

    xhr.onload = () => {
      let data;
      try {
        data = JSON.parse(xhr.responseText);
      } catch (err) {
        return reject(new Error(`Máy chủ trả về phản hồi không hợp lệ (mã lỗi ${xhr.status})`));
      }

      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(data);
      } else {
        reject(new Error(data.message || `Lỗi nhận diện video Fusion (mã lỗi ${xhr.status})`));
      }
    };

    xhr.onerror = () => {
      reject(new Error('Không thể kết nối đến máy chủ hoặc tệp video bị gián đoạn.'));
    };

    xhr.ontimeout = () => {
      reject(new Error('Hết thời gian chờ phản hồi từ máy chủ (Timeout sau 5 phút).'));
    };
    xhr.timeout = 300000; // 5 phút

    xhr.send(formData);
  });
}

// 3e. Register Body Re-ID for Profile
export async function registerBody(id, fileBlob) {
  const formData = new FormData();
  formData.append('file', fileBlob, 'body.jpg');

  const response = await fetch(`${API_BASE_URL}/nguoi-mat-tich/${id}/register-body`, {
    method: 'POST',
    headers: {
      ...getAuthHeader(),
    },
    body: formData,
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Không thể đăng ký đặc trưng thân hình');
  }
  return data;
}

// 4. Detection Logs API
export async function getLogs() {
  const response = await fetch(`${API_BASE_URL}/logs`, {
    headers: {
      ...getAuthHeader(),
    },
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || 'Không thể tải lịch sử phát hiện');
  }
  return data;
}

