import { useEffect, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Badge, Button, Layout, Menu, type MenuProps } from 'antd';
import { api } from '@/api';
import { useAuth } from '@/auth';

const { Header, Sider, Content } = Layout;

/**
 * Lab 5 Step 4 的 AppShell：Header + Sidebar + Outlet。
 * 具体页面由 App.tsx 的嵌套路由渲染到 <Outlet />，布局本身不承载业务状态。
 */
export function AppShell() {
  const { me, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (!me) return;
    const load = () =>
      api
        .unreadCount()
        .then((r) => setUnread(r.count))
        .catch(() => undefined);
    load();
    const timer = window.setInterval(load, 30_000);
    return () => window.clearInterval(timer);
  }, [me]);

  const items: NonNullable<MenuProps['items']> = [
    { key: '/questions', label: <Link to="/questions">问题</Link> },
    { key: '/leaderboard', label: <Link to="/leaderboard">排行榜</Link> },
  ];
  if (me) {
    items.push({
      key: '/notifications',
      label: (
        <Link to="/notifications">
          <Badge count={unread} size="small" offset={[8, 0]}>
            通知
          </Badge>
        </Link>
      ),
    });
  }

  const selectedKey = location.pathname.startsWith('/leaderboard')
    ? '/leaderboard'
    : location.pathname.startsWith('/notifications')
      ? '/notifications'
      : '/questions';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          background: '#fff',
          borderBottom: '1px solid #f0f0f0',
          paddingInline: 24,
        }}
      >
        <Link to="/questions" style={{ fontSize: 18, fontWeight: 700, color: '#1677ff' }}>
          CampusOverflow
        </Link>
        <div style={{ flex: 1 }} />
        {me ? (
          <>
            <span>{me.displayName}</span>
            <span className="role">{me.role}</span>
            <Button onClick={() => logout().then(() => navigate('/questions'))}>退出</Button>
          </>
        ) : (
          <Link className="primary" to="/login">
            登录 / 注册
          </Link>
        )}
      </Header>
      <Layout>
        <Sider width={200} theme="light" breakpoint="lg" collapsedWidth="0">
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            items={items}
            style={{ height: '100%', borderRight: 0 }}
          />
        </Sider>
        <Content style={{ padding: 24, background: '#fff' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
