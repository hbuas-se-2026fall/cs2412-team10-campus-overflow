/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CommentRequest } from '../models/CommentRequest';
import type { IdResponse } from '../models/IdResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class CommentsService {
    /**
     * 发表评论
     * @param requestBody
     * @returns IdResponse 新评论 id
     * @throws ApiError
     */
    public static postV1Comments(
        requestBody: CommentRequest,
    ): CancelablePromise<IdResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/v1/comments',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
