/** 查询键集中定义：所有 useQuery 与缓存失效都从这里取键，避免字面量散落导致漏失效。 */

export interface QuestionListFilters {
  keyword: string;
  tag?: string;
  sort: string;
  unanswered: boolean;
  page: number;
}

export const queryKeys = {
  auth: {
    all: () => ['auth'] as const,
    me: () => [...queryKeys.auth.all(), 'me'] as const,
    csrf: () => [...queryKeys.auth.all(), 'csrf'] as const,
  },
  questions: {
    all: () => ['questions'] as const,
    // lists 是 list 的前缀：失效整个列表族时无需关心当前筛选条件
    lists: () => [...queryKeys.questions.all(), 'list'] as const,
    list: (filters: QuestionListFilters) => [...queryKeys.questions.lists(), filters] as const,
    details: () => [...queryKeys.questions.all(), 'detail'] as const,
    detail: (questionId: number) => [...queryKeys.questions.details(), questionId] as const,
    related: (questionId: number) => [...queryKeys.questions.all(), 'related', questionId] as const,
    popularTags: () => [...queryKeys.questions.all(), 'popular-tags'] as const,
  },
};
