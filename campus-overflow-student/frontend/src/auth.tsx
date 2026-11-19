import { createContext, useContext, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from './api';
import { queryKeys } from './queryKeys';
import type { Me } from './types';

interface AuthState {
  me: Me | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();

  // 先取 CSRF Cookie（双提交机制），完成后才确认登录态，避免首屏多打一次无效请求
  const csrfQuery = useQuery({
    queryKey: queryKeys.auth.csrf(),
    queryFn: () => api.csrf(),
    staleTime: Infinity,
    retry: false,
  });

  const meQuery = useQuery({
    queryKey: queryKeys.auth.me(),
    queryFn: async (): Promise<Me | null> => {
      try {
        return await api.me();
      } catch {
        return null; // 401 表示未登录，属于正常状态
      }
    },
    enabled: csrfQuery.isSuccess || csrfQuery.isError,
  });

  const loginMutation = useMutation({
    mutationFn: ({ username, password }: { username: string; password: string }) =>
      api.login(username, password),
    onSuccess: (me) => queryClient.setQueryData(queryKeys.auth.me(), me),
  });

  const logoutMutation = useMutation({
    mutationFn: () => api.logout(),
    onSuccess: () => queryClient.setQueryData(queryKeys.auth.me(), null),
  });

  // 对外结构与原先 useState 版本保持一致，页面无需感知底层换成了 Query 缓存
  const value: AuthState = {
    me: meQuery.data ?? null,
    loading: meQuery.isPending,
    login: async (username, password) => {
      await loginMutation.mutateAsync({ username, password });
    },
    logout: async () => {
      await logoutMutation.mutateAsync();
    },
    refresh: async () => {
      await meQuery.refetch();
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth 必须在 AuthProvider 内使用');
  return ctx;
}
