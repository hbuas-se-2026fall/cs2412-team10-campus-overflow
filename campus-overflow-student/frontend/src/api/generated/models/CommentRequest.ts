/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type CommentRequest = {
    targetType: CommentRequest.targetType;
    targetId: number;
    body: string;
    parentId?: number | null;
};
export namespace CommentRequest {
    export enum targetType {
        QUESTION = 'QUESTION',
        ANSWER = 'ANSWER',
    }
}

