/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotificationPage } from '../models/NotificationPage';
import type { ReadAllResult } from '../models/ReadAllResult';
import type { RelatedQuestion } from '../models/RelatedQuestion';
import type { SearchResultPage } from '../models/SearchResultPage';
import type { UnreadCount } from '../models/UnreadCount';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class DiscoveryService {
    /**
     * 全文检索问题
     * @param q
     * @param tag
     * @param sort
     * @param unanswered
     * @param page
     * @returns SearchResultPage 检索结果分页
     * @throws ApiError
     */
    public static getV1SearchQuestions(
        q: string,
        tag?: string,
        sort?: string,
        unanswered?: boolean,
        page: number = 1,
    ): CancelablePromise<SearchResultPage> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/search/questions',
            query: {
                'q': q,
                'tag': tag,
                'sort': sort,
                'unanswered': unanswered,
                'page': page,
            },
        });
    }
    /**
     * 相关推荐问题
     * @param questionId
     * @returns RelatedQuestion 相关问题列表
     * @throws ApiError
     */
    public static getV1QuestionsRelated(
        questionId: number,
    ): CancelablePromise<Array<RelatedQuestion>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/questions/{questionId}/related',
            path: {
                'questionId': questionId,
            },
        });
    }
    /**
     * 通知分页列表
     * @returns NotificationPage 通知分页
     * @throws ApiError
     */
    public static getV1Notifications(): CancelablePromise<NotificationPage> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/notifications',
        });
    }
    /**
     * 未读通知计数
     * @returns UnreadCount 未读数量
     * @throws ApiError
     */
    public static getV1NotificationsUnreadCount(): CancelablePromise<UnreadCount> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/notifications/unread-count',
        });
    }
    /**
     * 全部标记已读
     * @returns ReadAllResult 更新结果
     * @throws ApiError
     */
    public static postV1NotificationsReadAll(): CancelablePromise<ReadAllResult> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/v1/notifications/read-all',
        });
    }
}
