/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type VoteRequest = {
    targetType: VoteRequest.targetType;
    targetId: number;
    value: VoteRequest.value;
};
export namespace VoteRequest {
    export enum targetType {
        QUESTION = 'QUESTION',
        ANSWER = 'ANSWER',
    }
    export enum value {
        '_-1' = -1,
        '_0' = 0,
        '_1' = 1,
    }
}

