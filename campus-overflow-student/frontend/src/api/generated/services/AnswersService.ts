/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AcceptRequest } from '../models/AcceptRequest';
import type { AnswerRequest } from '../models/AnswerRequest';
import type { IdResponse } from '../models/IdResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class AnswersService {
    /**
     * 提交回答
     * @param questionId
     * @param requestBody
     * @returns IdResponse 新回答 id
     * @throws ApiError
     */
    public static postV1QuestionsAnswers(
        questionId: number,
        requestBody: AnswerRequest,
    ): CancelablePromise<IdResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/v1/questions/{questionId}/answers',
            path: {
                'questionId': questionId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 编辑回答
     * @param answerId
     * @param requestBody
     * @returns any 已更新
     * @throws ApiError
     */
    public static putV1Answers(
        answerId: number,
        requestBody: AnswerRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/v1/answers/{answerId}',
            path: {
                'answerId': answerId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 删除回答
     * @param answerId
     * @returns void
     * @throws ApiError
     */
    public static deleteV1Answers(
        answerId: number,
    ): CancelablePromise<void> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/v1/answers/{answerId}',
            path: {
                'answerId': answerId,
            },
        });
    }
    /**
     * 采纳回答
     * @param questionId
     * @param requestBody
     * @returns any 已采纳
     * @throws ApiError
     */
    public static putV1QuestionsAcceptedAnswer(
        questionId: number,
        requestBody: AcceptRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/v1/questions/{questionId}/accepted-answer',
            path: {
                'questionId': questionId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                409: `状态冲突或重复操作`,
            },
        });
    }
    /**
     * 教师认证回答
     * @param answerId
     * @returns any 已认证
     * @throws ApiError
     */
    public static postV1AnswersEndorsement(
        answerId: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/v1/answers/{answerId}/endorsement',
            path: {
                'answerId': answerId,
            },
        });
    }
}
