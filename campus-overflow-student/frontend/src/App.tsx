import type { ReactElement } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';

import { AuthProvider, useAuth } from './auth';
import { AskPage } from './pages/AskPage';
import { LeaderboardPage } from './pages/LeaderboardPage';
import { LoginPage } from './pages/LoginPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { QuestionDetailPage } from './pages/QuestionDetailPage';
import { QuestionListPage } from './pages/QuestionListPage';
import { ReputationPage } from './pages/ReputationPage';
import { AppShell } from './shared/AppShell';

function RequireAuth({ children }: { children: ReactElement }) {
  const { me, loading } = useAuth();
  if (loading) return <p className="muted">加载中…</p>;
  return me ? children : <Navigate to="/login" replace />;
}

/**
 * 路由对齐 Lab 5 Step 4：/questions、/questions/new、/questions/:id。
 * 旧路径 / 与 /ask 保留为重定向，避免既有链接与书签失效。
 */
export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<AppShell />}>
            <Route index element={<Navigate to="/questions" replace />} />
            <Route path="/questions" element={<QuestionListPage />} />
            <Route
              path="/questions/new"
              element={
                <RequireAuth>
                  <AskPage />
                </RequireAuth>
              }
            />
            <Route path="/questions/:id" element={<QuestionDetailPage />} />
            <Route path="/ask" element={<Navigate to="/questions/new" replace />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/leaderboard" element={<LeaderboardPage />} />
            <Route
              path="/reputation"
              element={
                <RequireAuth>
                  <ReputationPage />
                </RequireAuth>
              }
            />
            <Route
              path="/notifications"
              element={
                <RequireAuth>
                  <NotificationsPage />
                </RequireAuth>
              }
            />
            <Route path="*" element={<Navigate to="/questions" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
