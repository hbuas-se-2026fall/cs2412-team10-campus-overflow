/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AskQuestionRequest } from '../models/AskQuestionRequest';
import type { IdResponse } from '../models/IdResponse';
import type { QuestionDetail } from '../models/QuestionDetail';
import type { QuestionSummaryPage } from '../models/QuestionSummaryPage';
import type { TagCount } from '../models/TagCount';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class QuestionsService {
    /**
     * 课程问题分页列表
     * @param page
     * @param courseId
     * @returns QuestionSummaryPage 分页问题摘要
     * @throws ApiError
     */
    public static getV1Questions(
        page: number = 1,
        courseId?: number,
    ): CancelablePromise<QuestionSummaryPage> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/questions',
            query: {
                'page': page,
                'courseId': courseId,
            },
        });
    }
    /**
     * 发布问题
     * @param requestBody
     * @returns IdResponse 新问题 id
     * @throws ApiError
     */
    public static postV1Questions(
        requestBody: AskQuestionRequest,
    ): CancelablePromise<IdResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/v1/questions',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                401: `未登录或会话失效`,
            },
        });
    }
    /**
     * 问题详情
     * @param questionId
     * @returns QuestionDetail 问题详情
     * @throws ApiError
     */
    public static getV1Questions1(
        questionId: number,
    ): CancelablePromise<QuestionDetail> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/questions/{questionId}',
            path: {
                'questionId': questionId,
            },
            errors: {
                404: `资源不存在`,
            },
        });
    }
    /**
     * 编辑问题
     * @param questionId
     * @param requestBody
     * @returns any 已更新
     * @throws ApiError
     */
    public static putV1Questions(
        questionId: number,
        requestBody: AskQuestionRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/v1/questions/{questionId}',
            path: {
                'questionId': questionId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `无权限`,
            },
        });
    }
    /**
     * 删除问题
     * @param questionId
     * @returns void
     * @throws ApiError
     */
    public static deleteV1Questions(
        questionId: number,
    ): CancelablePromise<void> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/v1/questions/{questionId}',
            path: {
                'questionId': questionId,
            },
            errors: {
                403: `无权限`,
            },
        });
    }
    /**
     * 热门标签
     * @param limit
     * @returns TagCount 标签及使用次数
     * @throws ApiError
     */
    public static getV1TagsPopular(
        limit: number = 15,
    ): CancelablePromise<Array<TagCount>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/tags/popular',
            query: {
                'limit': limit,
            },
        });
    }
}
