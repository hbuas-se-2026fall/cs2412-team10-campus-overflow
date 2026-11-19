import { useEffect, useState } from 'react';
import { onLoadingChange } from '@/api/http';

/** 全局 Loading：订阅 axios 拦截器维护的请求计数，有进行中的请求时展示。 */
export function GlobalLoading() {
  const [loading, setLoading] = useState(false);

  useEffect(() => onLoadingChange(setLoading), []);

  if (!loading) return null;

  return (
    <div className="global-loading" role="status" aria-live="polite">
      加载中…
    </div>
  );
}
