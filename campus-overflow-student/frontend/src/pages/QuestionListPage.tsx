import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api } from '@/api';
import { useAuth } from '@/auth';
import { useQuestionFilterStore } from '@/shared/store';

export function QuestionListPage() {
  const { me } = useAuth();
  const { keyword, activeTag, sort, unanswered, setKeyword, setActiveTag, setSort, setUnanswered } =
    useQuestionFilterStore();

  const searching = keyword.trim() !== '' || Boolean(activeTag) || unanswered || sort !== 'newest';

  // 服务器状态由 TanStack Query 请求 + 缓存，筛选条件作为 queryKey 的一部分自动触发重取。
  const questions = useQuery({
    queryKey: ['questions', { keyword, activeTag, sort, unanswered }],
    queryFn: () =>
      searching
        ? api.search(keyword.trim(), { tag: activeTag, sort, unanswered })
        : api.questions(1),
  });
  const popularTags = useQuery({
    queryKey: ['popularTags'],
    queryFn: () => api.popularTags(),
  });

  const data = questions.data;
  const error = questions.error?.message ?? '';
  const tags = popularTags.data ?? [];

  return (
    <div className="layout">
      <section>
        <div className="toolbar">
          <input
            placeholder="搜索问题（支持中文全文检索）"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <select value={sort} onChange={(e) => setSort(e.target.value)}>
            <option value="newest">最新</option>
            <option value="active">最近活跃</option>
            <option value="hot">得分最高</option>
            <option value="relevance">相关度</option>
          </select>
          <label className="check">
            <input
              type="checkbox"
              checked={unanswered}
              onChange={(e) => setUnanswered(e.target.checked)}
            />
            仅看未回答
          </label>
          {me && (
            <Link className="primary" to="/questions/new">
              我要提问
            </Link>
          )}
        </div>

        {activeTag && (
          <p className="muted">
            标签筛选：<b>{activeTag}</b>{' '}
            <button onClick={() => setActiveTag(undefined)}>清除</button>
          </p>
        )}
        {error && <p className="error">{error}</p>}
        {questions.isFetching && <p className="muted">正在刷新…</p>}

        <ul className="qlist">
          {data?.items.map((q) => (
            <li key={q.id}>
              <div className="stats">
                <span>
                  <b>{q.score}</b> 票
                </span>
                <span className={q.accepted ? 'accepted' : ''}>
                  <b>{q.answerCount}</b> 回答
                </span>
                <span>{q.viewCount} 浏览</span>
              </div>
              <div className="qmain">
                <Link className="qtitle" to={`/questions/${q.id}`}>
                  {q.title}
                </Link>
                <p className="excerpt">{q.excerpt}</p>
                <div className="tagrow">
                  {q.tags.map((t) => (
                    <button key={t} className="tag" onClick={() => setActiveTag(t)}>
                      {t}
                    </button>
                  ))}
                  <span className="muted">
                    {q.author.displayName} · {new Date(q.createdAt).toLocaleString('zh-CN')}
                  </span>
                </div>
              </div>
            </li>
          ))}
        </ul>
        {data && data.items.length === 0 && (
          <p className="muted">没有匹配的问题，换个关键词试试。</p>
        )}
      </section>

      <aside>
        <h3>热门标签</h3>
        <div className="tagrow">
          {tags.map((t) => (
            <button key={t.name} className="tag" onClick={() => setActiveTag(t.name)}>
              {t.name}
              <span className="count">{t.questionCount}</span>
            </button>
          ))}
        </div>
      </aside>
    </div>
  );
}
