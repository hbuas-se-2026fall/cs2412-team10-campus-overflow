/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CsrfResponse } from '../models/CsrfResponse';
import type { IdResponse } from '../models/IdResponse';
import type { LoginRequest } from '../models/LoginRequest';
import type { Me } from '../models/Me';
import type { RegisterRequest } from '../models/RegisterRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class AuthService {
    /**
     * 获取 CSRF Token
     * @returns CsrfResponse CSRF 令牌（同时写入 XSRF-TOKEN Cookie）
     * @throws ApiError
     */
    public static getV1AuthCsrf(): CancelablePromise<CsrfResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/auth/csrf',
        });
    }
    /**
     * 校园身份注册
     * @param requestBody
     * @returns IdResponse 注册成功
     * @throws ApiError
     */
    public static postV1AuthRegister(
        requestBody: RegisterRequest,
    ): CancelablePromise<IdResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/v1/auth/register',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `参数校验失败`,
                409: `状态冲突或重复操作`,
            },
        });
    }
    /**
     * 登录
     * @param requestBody
     * @returns Me 当前登录用户
     * @throws ApiError
     */
    public static postV1AuthLogin(
        requestBody: LoginRequest,
    ): CancelablePromise<Me> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/v1/auth/login',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                401: `未登录或会话失效`,
            },
        });
    }
    /**
     * 退出登录
     * @returns void
     * @throws ApiError
     */
    public static postV1AuthLogout(): CancelablePromise<void> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/v1/auth/logout',
        });
    }
    /**
     * 当前登录用户
     * @returns Me 当前用户
     * @throws ApiError
     */
    public static getV1AuthMe(): CancelablePromise<Me> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/v1/auth/me',
            errors: {
                401: `未登录或会话失效`,
            },
        });
    }
}
