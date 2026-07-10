import api from './index.js';

// 上传文件到 OSS，返回可访问的 URL
export function uploadToOss(file, onProgress) {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/oss/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  });
}
