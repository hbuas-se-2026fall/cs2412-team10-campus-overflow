package com.campusoverflow.bootstrap.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("TraceIdFilter：把 traceId 注入 MDC 并透传到响应头")
class TraceIdFilterTest {

    private static final String HEADER = "X-Request-Id";

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("请求头缺失时生成 32 位 traceId，并在调用链内可见")
    void generatesTraceIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> inChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> inChain.set(MDC.get(TraceIdFilter.TRACE_ID)));

        String traceId = inChain.get();
        assertThat(traceId).isNotNull().matches("[0-9a-f]{32}");
        assertThat(response.getHeader(HEADER)).isEqualTo(traceId);
    }

    @Test
    @DisplayName("上游传来的合法 traceId 原样透传，保证跨服务链路连续")
    void reusesSafeIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        request.addHeader(HEADER, "req-2026_1109-ABCDEF");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> inChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> inChain.set(MDC.get(TraceIdFilter.TRACE_ID)));

        assertThat(inChain.get()).isEqualTo("req-2026_1109-ABCDEF");
        assertThat(response.getHeader(HEADER)).isEqualTo("req-2026_1109-ABCDEF");
    }

    @Test
    @DisplayName("含非法字符的 traceId 被丢弃并重新生成，避免日志注入")
    void rejectsUnsafeIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        request.addHeader(HEADER, "bad id <>");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> inChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> inChain.set(MDC.get(TraceIdFilter.TRACE_ID)));

        assertThat(inChain.get()).matches("[0-9a-f]{32}").isNotEqualTo("bad id <>");
        assertThat(response.getHeader(HEADER)).isEqualTo(inChain.get());
    }

    @Test
    @DisplayName("下游抛异常时仍清理 MDC，避免线程复用导致串号")
    void clearsMdcWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/questions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new IllegalStateException("下游故障");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
    }
}
