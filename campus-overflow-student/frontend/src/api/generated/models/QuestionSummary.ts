/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Author } from './Author';
export type QuestionSummary = {
    id: number;
    title: string;
    excerpt: string;
    tags: Array<string>;
    courseId?: number;
    author: Author;
    score: number;
    answerCount: number;
    viewCount: number;
    accepted: boolean;
    createdAt: string;
};

