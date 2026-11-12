import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';

/** 后端以 RFC 9457 Problem Details 返回错误，这里统一转成带业务码的异常。 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(`${name}=`))
    ?.split('=')[1];
}

/**
 * 全局 Loading 计数：并发请求共用一个计数器，最后一个请求结束才收起。
 * 组件通过 `onLoadingChange` 订阅，或监听 `app:loading` 自定义事件。
 */
type LoadingListener = (loading: boolean) => void;
let activeRequests = 0;
const listeners = new Set<LoadingListener>();

function emitLoading() {
  const loading = activeRequests > 0;
  listeners.forEach((listener) => listener(loading));
  window.dispatchEvent(new CustomEvent('app:loading', { detail: { loading } }));
}

function startLoading(): void {
  activeRequests += 1;
  emitLoading();
}

function stopLoading(): void {
  activeRequests = Math.max(0, activeRequests - 1);
  emitLoading();
}

export function onLoadingChange(listener: LoadingListener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** 独立于业务实例的裸 axios，用于预取 CSRF，避免再次走业务拦截器造成循环。 */
const raw = axios.create({ withCredentials: true });

export const http: AxiosInstance = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

// 请求拦截：注入 CSRF Token（双提交 Cookie），并计数全局 Loading。
http.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  startLoading();
  const method = (config.method ?? 'get').toLowerCase();
  if (method !== 'get') {
    let token = readCookie('XSRF-TOKEN');
    if (!token) {
      await raw.get('/v1/auth/csrf', { withCredentials: true });
      token = readCookie('XSRF-TOKEN');
    }
    if (token) config.headers['X-XSRF-TOKEN'] = decodeURIComponent(token);
  }
  return config;
});

// 响应拦截：收起 Loading，并把后端 Problem Details 统一转成 ApiError。
http.interceptors.response.use(
  (response) => {
    stopLoading();
    return response;
  },
  (error) => {
    stopLoading();
    const status: number = error.response?.status ?? 0;
    const data = error.response?.data;
    const code: string = data?.code ?? 'SYS-000';
    const message: string = data?.detail ?? error.message ?? '请求失败';
    throw new ApiError(status, code, message);
  },
);

/** 统一的 GET / POST / PUT / DELETE 封装，返回响应体 JSON。 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const res = await http.request<unknown, { data: T }>(config);
  return res.data;
}
