import { request } from './api/http';
import type { components } from './api/generated/schema';
import type {
  LeaderboardEntry,
  Me,
  Notification,
  Page,
  QuestionDetail,
  QuestionSummary,
  ReputationProfile,
  SearchResult,
} from './types';

export { ApiError } from './api/http';

/** 由 openapi-typescript 生成的契约类型；后端改动字段后重跑 `pnpm gen:api` 会在此反映。 */
type Schema<K extends keyof components['schemas']> = components['schemas'][K];

const get = <T>(path: string, params?: Record<string, unknown>) =>
  request<T>({ method: 'GET', url: path, params });
const post = <T>(path: string, body?: unknown) =>
  request<T>({ method: 'POST', url: path, data: body });
const put = <T>(path: string, body?: unknown) =>
  request<T>({ method: 'PUT', url: path, data: body });
const del = <T>(path: string) => request<T>({ method: 'DELETE', url: path });

export const api = {
  csrf: () => get<{ token: string }>('/v1/auth/csrf'),
  register: (body: Schema<'RegisterRequest'>) => post<{ id: number }>('/v1/auth/register', body),
  login: (username: string, password: string) =>
    request<Me>({
      method: 'POST',
      url: '/v1/auth/login',
      data: { username, password } satisfies Schema<'LoginRequest'>,
    }),
  logout: () => request<void>({ method: 'POST', url: '/v1/auth/logout' }),
  me: () => get<Me>('/v1/auth/me'),

  questions: (page = 1, courseId?: number) =>
    get<Page<QuestionSummary>>(`/v1/questions`, { page, ...(courseId ? { courseId } : {}) }),
  question: (id: number) => get<QuestionDetail>(`/v1/questions/${id}`),
  ask: (body: Schema<'AskQuestionRequest'>) => post<{ id: number }>('/v1/questions', body),
  deleteQuestion: (id: number) => del(`/v1/questions/${id}`),
  answer: (questionId: number, body: string) =>
    post<{ id: number }>(`/v1/questions/${questionId}/answers`, {
      body,
    } satisfies Schema<'AnswerRequest'>),
  accept: (questionId: number, answerId: number) =>
    put<void>(`/v1/questions/${questionId}/accepted-answer`, {
      answerId,
    } satisfies Schema<'AcceptRequest'>),
  endorse: (answerId: number) => post<void>(`/v1/answers/${answerId}/endorsement`),
  vote: (targetType: 'QUESTION' | 'ANSWER', targetId: number, value: -1 | 0 | 1) =>
    put<{ myVote: number; scoreDelta: number }>('/v1/votes', {
      targetType,
      targetId,
      value,
    } satisfies Schema<'VoteRequest'>),
  comment: (targetType: 'QUESTION' | 'ANSWER', targetId: number, body: string, parentId?: number) =>
    post<{ id: number }>('/v1/comments', {
      targetType,
      targetId,
      body,
      parentId,
    } satisfies Schema<'CommentRequest'>),

  search: (
    q: string,
    params: { tag?: string; sort?: string; unanswered?: boolean; page?: number } = {},
  ) => {
    const search = new URLSearchParams({ q, page: String(params.page ?? 1) });
    if (params.tag) search.set('tag', params.tag);
    if (params.sort) search.set('sort', params.sort);
    if (params.unanswered) search.set('unanswered', 'true');
    return get<Page<SearchResult>>(`/v1/search/questions?${search}`);
  },
  related: (questionId: number) =>
    get<{ id: number; title: string; answerCount: number; accepted: boolean }[]>(
      `/v1/questions/${questionId}/related`,
    ),
  popularTags: () => get<{ name: string; questionCount: number }[]>('/v1/tags/popular?limit=15'),

  leaderboard: (period: string) => get<LeaderboardEntry[]>(`/v1/leaderboard?period=${period}`),
  reputation: (userId: number) => get<ReputationProfile>(`/v1/users/${userId}/reputation`),
  bounty: (questionId: number, points: number, days: number) =>
    post<{ id: number }>(`/v1/questions/${questionId}/bounty`, {
      points,
      days,
    } satisfies Schema<'BountyRequest'>),

  notifications: () => get<Page<Notification>>('/v1/notifications'),
  unreadCount: () => get<{ count: number }>('/v1/notifications/unread-count'),
  readAll: () => post<{ updated: number }>('/v1/notifications/read-all', {}),
};
