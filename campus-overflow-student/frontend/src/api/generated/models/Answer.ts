/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Author } from './Author';
import type { Comment } from './Comment';
export type Answer = {
    id: number;
    body: string;
    bodyHtml: string;
    author: Author;
    score: number;
    accepted: boolean;
    endorsedBy?: Author;
    /**
     * -1 / 0 / 1
     */
    myVote: number;
    canEdit: boolean;
    createdAt: string;
    comments: Array<Comment>;
};

