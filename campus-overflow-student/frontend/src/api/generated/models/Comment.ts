/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Author } from './Author';
export type Comment = {
    id: number;
    author: Author;
    parentId?: number | null;
    body: string;
    createdAt: string;
};

