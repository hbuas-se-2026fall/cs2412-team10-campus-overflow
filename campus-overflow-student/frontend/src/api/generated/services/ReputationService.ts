/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BountyRequest } from '../models/BountyRequest';
import type { IdResponse } from '../models/IdResponse';
import type { LeaderboardEntry } from '../models/LeaderboardEntry';
import type { ReputationProfile } from '../models/ReputationProfile';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class ReputationService {
    /**
     * 声誉排行榜
     * @param period
     * @returns LeaderboardEntry 排行榜条目
     * @throws ApiError
     */
    public static getV1Leaderboard(
        period: 'daily' | 'weekly' | 'term',
    ): CancelablePromise<Array<LeaderboardEntry>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/leaderboard',
            query: {
                'period': period,
            },
        });
    }
    /**
     * 个人声誉摘要
     * @param userId
     * @returns ReputationProfile 声誉档案
     * @throws ApiError
     */
    public static getV1UsersReputation(
        userId: number,
    ): CancelablePromise<ReputationProfile> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/users/{userId}/reputation',
            path: {
                'userId': userId,
            },
        });
    }
    /**
     * 发起悬赏
     * @param questionId
     * @param requestBody
     * @returns IdResponse 新悬赏 id
     * @throws ApiError
     */
    public static postV1QuestionsBounty(
        questionId: number,
        requestBody: BountyRequest,
    ): CancelablePromise<IdResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/v1/questions/{questionId}/bounty',
            path: {
                'questionId': questionId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
