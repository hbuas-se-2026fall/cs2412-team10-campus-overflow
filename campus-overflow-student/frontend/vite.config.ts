import { fileURLToPath, URL } from 'node:url';

import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// 开发期把 /api 代理到后端，避免跨域，也让会话 Cookie 与 CSRF 双提交在同源下正常工作。
// path alias：@ 指向 src，与 tsconfig.json 的 paths 保持一致。
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // 按依赖来源分包：业务代码与三方库分离，避免单个 chunk 过大并提升缓存命中率。
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'data-vendor': ['@tanstack/react-query', 'zustand', 'axios'],
          'ui-vendor': ['antd'],
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
