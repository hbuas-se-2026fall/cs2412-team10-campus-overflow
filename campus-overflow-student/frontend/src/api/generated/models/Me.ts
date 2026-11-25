/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Author_properties_role } from './Author_properties_role';
import type { CourseRef } from './CourseRef';
export type Me = {
    id: number;
    username: string;
    displayName: string;
    email: string;
    role: Author_properties_role;
    verified: boolean;
    college?: string;
    className?: string;
    courses: Array<CourseRef>;
};

