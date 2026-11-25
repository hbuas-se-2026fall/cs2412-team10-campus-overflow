import { useQuery } from '@tanstack/react-query';
import { Badge, Button, Layout, Menu } from 'antd';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';

import { api } from '@/api';
import { useAuth } from '@/auth';

const { Header, Sider, Content } = Layout;

/**
 * Lab 5 Step 4 要求的 AppShell：Header + Sidebar + Outlet。
 * 服务器状态由 TanStack Query 管理，客户端筛选状态放在 zustand store，
 * 这里只负责布局、导航与登录态展示。
 */
export function AppShell() {
  const { me, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const unreadQuery = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: () => api.unreadCount(),
    enabled: me !== null,
  });
  const unread = unreadQuery.data?.count ?? 0;

  const items = [
    { key: '/questions', label: <Link to="/questions">问题</Link> },
    { key: '/leaderboard', label: <Link to="/leaderboard">排行榜</Link> },
  ];
  if (me) {
    items.push({ key: '/questions/new', label: <Link to="/questions/new">我要求助</Link> });
    items.push({ key: '/reputation', label: <Link to="/reputation">我的声誉</Link> });
    items.push({
      key: '/notifications',
      label: (
        <Link to="/notifications">
          消息 <Badge count={unread} size="small" />
        </Link>
      ),
    });
  }

  const selected = items
    .map((it) => it.key)
    .filter((key) => location.pathname === key || location.pathname.startsWith(`${key}/`))
    .sort((a, b) => b.length - a.length)[0];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: '#fff',
          borderBottom: '1px solid #e5e7eb',
          paddingInline: 24,
        }}
      >
        <Link to="/questions" style={{ fontSize: 18, fontWeight: 600, color: '#1668dc' }}>
          CampusOverflow
        </Link>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {me ? (
            <>
              <span style={{ color: '#4b5563' }}>
                {me.displayName}
                <span style={{ marginLeft: 6, color: '#9ca3af' }}>{me.role}</span>
              </span>
              <Button
                size="small"
                onClick={() => {
                  void logout().then(() => navigate('/login'));
                }}
              >
                退出
              </Button>
            </>
          ) : (
            <Button size="small" type="primary" onClick={() => navigate('/login')}>
              登录
            </Button>
          )}
        </div>
      </Header>
      <Layout>
        <Sider width={180} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={selected ? [selected] : []}
            items={items}
            style={{ height: '100%', borderInlineEnd: 0 }}
          />
        </Sider>
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
