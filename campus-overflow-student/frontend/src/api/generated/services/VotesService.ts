/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { VoteRequest } from '../models/VoteRequest';
import type { VoteResult } from '../models/VoteResult';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class VotesService {
    /**
     * 投票 / 改投 / 撤销
     * @param requestBody
     * @returns VoteResult 投票结果
     * @throws ApiError
     */
    public static putV1Votes(
        requestBody: VoteRequest,
    ): CancelablePromise<VoteResult> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/v1/votes',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
