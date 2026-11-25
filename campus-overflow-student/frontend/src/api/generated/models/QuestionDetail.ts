/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Answer } from './Answer';
import type { Author } from './Author';
import type { Comment } from './Comment';
import type { CourseRef } from './CourseRef';
export type QuestionDetail = {
    id: number;
    title: string;
    body: string;
    bodyHtml: string;
    tags: Array<string>;
    course?: CourseRef;
    author: Author;
    score: number;
    answerCount: number;
    viewCount: number;
    acceptedAnswerId?: number | null;
    myVote: number;
    createdAt: string;
    permissions: {
        canEdit?: boolean;
        canDelete?: boolean;
        canAccept?: boolean;
        canManage?: boolean;
    };
    comments: Array<Comment>;
    answers: Array<Answer>;
};

