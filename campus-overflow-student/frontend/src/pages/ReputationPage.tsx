import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { queryKeys } from '../queryKeys';

/** 个人声誉摘要：声誉值、积分（仅本人可见）与已授予徽章。 */
export function ReputationPage() {
  const { me } = useAuth();
  const userId = me ? me.id : null;

  const profileQuery = useQuery({
    queryKey: queryKeys.users.reputation(userId ?? 0),
    queryFn: async () => {
      if (userId === null) throw new Error('未登录');
      return api.reputation(userId);
    },
    enabled: userId !== null,
  });

  if (profileQuery.error) {
    return <p className="error">{profileQuery.error.message}</p>;
  }
  const profile = profileQuery.data;
  if (!profile) {
    return <p className="muted">加载中…</p>;
  }

  const availablePoints = profile.availablePoints;
  const frozenPoints = profile.frozenPoints;
  const badges = profile.badges;

  return (
    <div className="form-page">
      <h1>个人声誉摘要</h1>

      <table className="board">
        <tbody>
          <tr>
            <th scope="row">用户</th>
            <td>{profile.displayName}</td>
          </tr>
          <tr>
            <th scope="row">声誉</th>
            <td>{profile.reputation}</td>
          </tr>
          {typeof availablePoints === 'number' && (
            <tr>
              <th scope="row">可用积分</th>
              <td>{availablePoints}</td>
            </tr>
          )}
          {typeof frozenPoints === 'number' && (
            <tr>
              <th scope="row">冻结积分</th>
              <td>{frozenPoints}</td>
            </tr>
          )}
        </tbody>
      </table>

      <h2>徽章</h2>
      {badges.length === 0 ? (
        <p className="muted">还没有获得徽章，回答问题并采纳答案后可获得徽章。</p>
      ) : (
        <ul className="plain">
          {badges.map((badge) => (
            <li key={badge.code}>
              <b>{badge.name}</b>
              <span className="muted"> · {badge.description}</span>
              <div className="muted">
                授予于 {new Date(badge.awardedAt).toLocaleDateString('zh-CN')}
              </div>
            </li>
          ))}
        </ul>
      )}

      <p className="muted">
        积分仅本人登录可见，他人查看时只显示声誉值与徽章。
        <Link to="/leaderboard">查看排行榜</Link>
      </p>
    </div>
  );
}
