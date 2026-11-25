import { useQuery } from '@tanstack/react-query';
import type { QuestionSummaryPage } from '../api/generated/models/QuestionSummaryPage';
import type { QuestionSummary } from '../api/generated/models/QuestionSummary';
import { QuestionsService } from '../api/generated/services/QuestionsService';

export default function QuestionListPage() {
  const { data, isLoading, error } = useQuery<QuestionSummaryPage>({
    queryKey: ['questions'],
    queryFn: () => QuestionsService.getV1Questions(1),
  });

  if (isLoading) {
    return <div>加载中...</div>;
  }

  if (error) {
    return (
      <div>
        加载失败，请检查后端是否启动。报错：
        {error instanceof Error ? error.message : String(error)}
      </div>
    );
  }

  const items: QuestionSummary[] = data?.items ?? [];

  return (
    <div>
      <h2>问题列表</h2>
      {items.length > 0 ? (
        <ul>
          {items.map((q) => (
            <li
              key={q.id}
              style={{
                marginBottom: '10px',
                borderBottom: '1px solid #eee',
                paddingBottom: '10px',
              }}
            >
              <h3 style={{ margin: '5px 0' }}>{q.title}</h3>
              <p style={{ margin: '5px 0', color: '#666' }}>{q.excerpt}</p>
              <p style={{ margin: '5px 0', color: '#888' }}>
                标签：{q.tags.join('、')} ｜ 回答 {q.answerCount} ｜ 得分 {q.score}
              </p>
            </li>
          ))}
        </ul>
      ) : (
        <div style={{ padding: '20px', background: '#f5f5f5', color: '#888', borderRadius: '4px' }}>
          暂无数据（但接口已成功连通！）
        </div>
      )}
    </div>
  );
}
