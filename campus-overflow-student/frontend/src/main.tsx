import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import { App } from '@/App';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { GlobalLoading } from '@/components/GlobalLoading';
import '@/styles.css';

// 服务器状态由 TanStack Query 管理，客户端状态（列表筛选）由 zustand 管理，二者严格分离。
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider theme={{ token: { colorPrimary: '#1677ff' } }}>
      <QueryClientProvider client={queryClient}>
        <ErrorBoundary>
          <GlobalLoading />
          <App />
        </ErrorBoundary>
      </QueryClientProvider>
    </ConfigProvider>
  </React.StrictMode>,
);
