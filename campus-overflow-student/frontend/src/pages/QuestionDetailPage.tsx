import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { queryKeys } from '../queryKeys';

type VoteTarget = 'QUESTION' | 'ANSWER';

export function QuestionDetailPage() {
  const { id } = useParams();
  const questionId = Number(id);
  const { me } = useAuth();
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');

  const detailQuery = useQuery({
    queryKey: queryKeys.questions.detail(questionId),
    queryFn: () => api.question(questionId),
  });

  const relatedQuery = useQuery({
    queryKey: queryKeys.questions.related(questionId),
    queryFn: () => api.related(questionId),
  });

  // 写操作统一失效：问题详情族 + 当前问题的相关推荐 + 列表族
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: queryKeys.questions.details() });
    await queryClient.invalidateQueries({ queryKey: queryKeys.questions.related(questionId) });
    await queryClient.invalidateQueries({ queryKey: queryKeys.questions.lists() });
  };

  const onMutationError = (e: Error) => setError(e.message);

  const voteMutation = useMutation({
    mutationFn: (vars: { targetType: VoteTarget; targetId: number; value: -1 | 0 | 1 }) =>
      api.vote(vars.targetType, vars.targetId, vars.value),
    onSuccess: refresh,
    onError: onMutationError,
  });

  const bountyMutation = useMutation({
    mutationFn: (points: number) => api.bounty(questionId, points, 7),
    onSuccess: refresh,
    onError: onMutationError,
  });

  const acceptMutation = useMutation({
    mutationFn: (answerId: number) => api.accept(questionId, answerId),
    onSuccess: refresh,
    onError: onMutationError,
  });

  const endorseMutation = useMutation({
    mutationFn: (answerId: number) => api.endorse(answerId),
    onSuccess: refresh,
    onError: onMutationError,
  });

  const answerMutation = useMutation({
    mutationFn: (body: string) => api.answer(questionId, body),
    onSuccess: async () => {
      setDraft('');
      await refresh();
    },
    onError: onMutationError,
  });

  if (!detailQuery.data) {
    const message = detailQuery.error?.message || error;
    return <p className="muted">{message || '加载中…'}</p>;
  }

  const q = detailQuery.data;
  const related = relatedQuery.data ?? [];
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
              onClick={() =>
                voteMutation.mutate({
                  targetType: 'QUESTION',
                  targetId: q.id,
                  value: q.myVote === 1 ? 0 : 1,
                })
              }
            >
              ▲
            </button>
            <b>{q.score}</b>
            <button
              disabled={!me}
              onClick={() =>
                voteMutation.mutate({
                  targetType: 'QUESTION',
                  targetId: q.id,
                  value: q.myVote === -1 ? 0 : -1,
                })
              }
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
                  if (points) bountyMutation.mutate(points);
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
                onClick={() =>
                  voteMutation.mutate({
                    targetType: 'ANSWER',
                    targetId: a.id,
                    value: a.myVote === 1 ? 0 : 1,
                  })
                }
              >
                ▲
              </button>
              <b>{a.score}</b>
              <button
                disabled={!me}
                onClick={() =>
                  voteMutation.mutate({
                    targetType: 'ANSWER',
                    targetId: a.id,
                    value: a.myVote === -1 ? 0 : -1,
                  })
                }
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
                  <button onClick={() => acceptMutation.mutate(a.id)}>采纳此回答</button>
                )}
                {canManage && !a.endorsedBy && (
                  <button onClick={() => endorseMutation.mutate(a.id)}>教师认证</button>
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
            <button className="primary" onClick={() => answerMutation.mutate(draft)}>
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

  const commentMutation = useMutation({
    mutationFn: (body: string) => api.comment(targetType, targetId, body),
    onSuccess: () => {
      setText('');
      setOpen(false);
      onDone();
    },
  });

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
            <button onClick={() => commentMutation.mutate(text)}>发表</button>
          </div>
        ) : (
          <button className="link" onClick={() => setOpen(true)}>
            添加评论
          </button>
        ))}
    </div>
  );
}
