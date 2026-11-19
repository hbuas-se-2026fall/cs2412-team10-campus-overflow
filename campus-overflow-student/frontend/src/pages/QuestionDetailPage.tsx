import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '@/api';
import { useAuth } from '@/auth';

export function QuestionDetailPage() {
  const { id } = useParams();
  const questionId = Number(id);
  const { me } = useAuth();
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');

  const detail = useQuery({
    queryKey: ['question', questionId],
    queryFn: () => api.question(questionId),
    enabled: Number.isFinite(questionId),
  });
  const related = useQuery({
    queryKey: ['related', questionId],
    queryFn: () => api.related(questionId),
    enabled: Number.isFinite(questionId),
  });

  // 写操作统一走 useMutation，成功后失效并刷新相关查询，避免手工维护 loading 状态。
  const mutation = useMutation({
    mutationFn: (fn: () => Promise<unknown>) => fn(),
    onSuccess: () => {
      void detail.refetch();
      void related.refetch();
    },
    onError: (e: Error) => setError(e.message),
  });

  const q = detail.data;
  if (!q) return <p className="muted">{error || detail.error?.message || '加载中…'}</p>;

  const canManage = q.permissions.canManage;
  const run = (fn: () => Promise<unknown>) => mutation.mutate(fn);
  const refresh = () => void detail.refetch();
  const relatedList = related.data ?? [];

  return (
    <div className="layout">
      <article>
        <h1>{q.title}</h1>
        <p className="muted">
          {q.author.displayName} · {new Date(q.createdAt).toLocaleString('zh-CN')} · {q.viewCount}{' '}
          次浏览
          {q.course && <> · 课程 {q.course.name}</>}
        </p>

        <div className="post">
          <div className="votebox">
            <button
              disabled={!me}
              onClick={() => run(() => api.vote('QUESTION', q.id, q.myVote === 1 ? 0 : 1))}
            >
              ▲
            </button>
            <b>{q.score}</b>
            <button
              disabled={!me}
              onClick={() => run(() => api.vote('QUESTION', q.id, q.myVote === -1 ? 0 : -1))}
            >
              ▼
            </button>
          </div>
          <div className="body">
            {/* 正文由后端渲染并经 commonmark + OWASP Sanitizer 双重净化 */}
            <div dangerouslySetInnerHTML={{ __html: q.bodyHtml }} />
            <div className="tagrow">
              {q.tags.map((t) => (
                <span key={t} className="tag">
                  {t}
                </span>
              ))}
            </div>
            <CommentList
              comments={q.comments}
              targetType="QUESTION"
              targetId={q.id}
              onDone={refresh}
              canComment={!!me}
            />
            {canManage && (
              <button
                className="link"
                onClick={() => {
                  const points = Number(prompt('悬赏积分（10–500）', '50'));
                  if (points) run(() => api.bounty(q.id, points, 7));
                }}
              >
                发起悬赏
              </button>
            )}
          </div>
        </div>

        <h2>{q.answerCount} 个回答</h2>
        {q.answers.map((a) => (
          <div
            className={`post ${q.acceptedAnswerId === a.id ? 'is-accepted' : ''}`}
            key={a.id}
            id={`answer-${a.id}`}
          >
            <div className="votebox">
              <button
                disabled={!me}
                onClick={() => run(() => api.vote('ANSWER', a.id, a.myVote === 1 ? 0 : 1))}
              >
                ▲
              </button>
              <b>{a.score}</b>
              <button
                disabled={!me}
                onClick={() => run(() => api.vote('ANSWER', a.id, a.myVote === -1 ? 0 : -1))}
              >
                ▼
              </button>
              {q.acceptedAnswerId === a.id && (
                <span className="check-mark" title="已采纳">
                  ✔
                </span>
              )}
            </div>
            <div className="body">
              <div dangerouslySetInnerHTML={{ __html: a.bodyHtml }} />
              <p className="muted">
                {a.author.displayName}
                {a.endorsedBy && (
                  <span className="endorsed">教师认证 · {a.endorsedBy.displayName}</span>
                )}
                · {new Date(a.createdAt).toLocaleString('zh-CN')}
              </p>
              <div className="actions">
                {q.permissions.canAccept && !q.acceptedAnswerId && (
                  <button onClick={() => run(() => api.accept(q.id, a.id))}>采纳此回答</button>
                )}
                {canManage && !a.endorsedBy && (
                  <button onClick={() => run(() => api.endorse(a.id))}>教师认证</button>
                )}
              </div>
              <CommentList
                comments={a.comments}
                targetType="ANSWER"
                targetId={a.id}
                onDone={refresh}
                canComment={!!me}
              />
            </div>
          </div>
        ))}

        {me ? (
          <section className="answer-form">
            <h3>写回答</h3>
            <textarea
              rows={8}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="支持 Markdown，代码请使用 ``` 包裹"
            />
            <button
              className="primary"
              onClick={() =>
                run(async () => {
                  await api.answer(q.id, draft);
                  setDraft('');
                })
              }
            >
              提交回答
            </button>
          </section>
        ) : (
          <p className="muted">
            <Link to="/login">登录</Link>后可以回答与投票。
          </p>
        )}
        {error && <p className="error">{error}</p>}
      </article>

      <aside>
        <h3>相关问题</h3>
        <ul className="plain">
          {relatedList.map((r) => (
            <li key={r.id}>
              <Link to={`/questions/${r.id}`}>{r.title}</Link>
            </li>
          ))}
          {relatedList.length === 0 && <li className="muted">暂无</li>}
        </ul>
      </aside>
    </div>
  );
}

function CommentList({
  comments,
  targetType,
  targetId,
  onDone,
  canComment,
}: {
  comments: { id: number; author: { displayName: string }; body: string; createdAt: string }[];
  targetType: 'QUESTION' | 'ANSWER';
  targetId: number;
  onDone: () => void;
  canComment: boolean;
}) {
  const [text, setText] = useState('');
  const [open, setOpen] = useState(false);
  return (
    <div className="comments">
      {comments.map((c) => (
        <div className="comment" key={c.id}>
          <span>{c.body}</span>
          <span className="muted"> — {c.author.displayName}</span>
        </div>
      ))}
      {canComment &&
        (open ? (
          <div className="comment-form">
            <input
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="评论，可用 @昵称 提醒他人"
            />
            <button
              onClick={() =>
                api.comment(targetType, targetId, text).then(() => {
                  setText('');
                  setOpen(false);
                  onDone();
                })
              }
            >
              发表
            </button>
          </div>
        ) : (
          <button className="link" onClick={() => setOpen(true)}>
            添加评论
          </button>
        ))}
    </div>
  );
}
