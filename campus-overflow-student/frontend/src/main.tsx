import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import './styles.css';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { OpenAPI } from './api/generated/core/OpenAPI';

// 1. 配置生成的 API 客户端指向代理路径
OpenAPI.BASE = '/api';

// 2. 创建 React Query 客户端
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1, // 失败重试1次
      refetchOnWindowFocus: false, // 窗口聚焦时不自动刷新
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>,
);
