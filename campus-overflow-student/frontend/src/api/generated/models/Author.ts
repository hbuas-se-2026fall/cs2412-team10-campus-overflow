/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type Author = {
    id: number;
    displayName: string;
    role: Author.role;
    verified: boolean;
};
export namespace Author {
    export enum role {
        STUDENT = 'STUDENT',
        TA = 'TA',
        TEACHER = 'TEACHER',
        ADMIN = 'ADMIN',
    }
}

