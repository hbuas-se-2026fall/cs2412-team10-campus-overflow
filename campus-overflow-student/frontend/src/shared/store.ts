import { create } from 'zustand';

/**
 * 客户端状态：问题列表的筛选条件。
 *
 * Lab 5「常见陷阱」要求严格区分服务器状态与客户端状态：问题列表、详情、热门标签
 * 全部由 TanStack Query 管理（见 QuestionListPage / QuestionDetailPage），此处只保存
 * 用户交互产生的筛选条件，避免大量 useState 散落组件内导致 prop drilling。
 */
export interface QuestionFilterState {
  keyword: string;
  activeTag?: string;
  sort: string;
  unanswered: boolean;
  setKeyword: (value: string) => void;
  setActiveTag: (value: string | undefined) => void;
  setSort: (value: string) => void;
  setUnanswered: (value: boolean) => void;
  reset: () => void;
}

const initialState = {
  keyword: '',
  activeTag: undefined,
  sort: 'newest',
  unanswered: false,
};

export const useQuestionFilterStore = create<QuestionFilterState>((set) => ({
  ...initialState,
  setKeyword: (keyword) => set({ keyword }),
  setActiveTag: (activeTag) => set({ activeTag }),
  setSort: (sort) => set({ sort }),
  setUnanswered: (unanswered) => set({ unanswered }),
  reset: () => set(initialState),
}));
