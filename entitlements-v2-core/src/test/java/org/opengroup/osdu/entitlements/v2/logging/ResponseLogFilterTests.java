package org.opengroup.osdu.entitlements.v2.logging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.Request;
import org.opengroup.osdu.core.common.model.http.RequestInfo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResponseLogFilterTests {
    @Mock
    private RequestInfo requestInfo;
    @Mock
    private DpsHeaders headers;
    @Mock
    private ServletContext context;
    @Mock
    private FilterChain filterChain;
    @Mock
    private JaxRsDpsLog logger;
    @Mock
    private HttpServletRequest servletRequest;
    @InjectMocks
    private ResponseLogFilter responseLogFilter;
    private final HttpServletResponse servletResponse = mockHttpServletResponse();

    @Before
    public void setup() {
        when(requestInfo.getHeaders()).thenReturn(headers);
        when(headers.getCorrelationId()).thenReturn("testcorrelationId");
        when(requestInfo.getUri()).thenReturn("http://google.com");
        when(requestInfo.getUserIp()).thenReturn("127.0.0.1");
        when(servletRequest.getMethod()).thenReturn("OPTIONS");
        when(servletRequest.getServletContext()).thenReturn(context);
        when(context.getAttribute("starttime")).thenReturn(null);
    }

    @Test
    public void shouldNotThrowAppExceptionWhenIsACron() throws Exception {
        responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
        assertEquals(200, servletResponse.getStatus());
    }

    @Test
    public void shouldNotThrowAppExceptionWhenIsAHttpsRequest() throws Exception {
        responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
        assertEquals(200, servletResponse.getStatus());
    }

    @Test
    public void shouldLogHttpRequestInfoWhenFilterIsCalled() throws Exception {
        responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
        assertEquals(200, servletResponse.getStatus());
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
        verify(logger).request(eq(ResponseLogFilter.class.getName()), argument.capture());
        Request result = argument.getValue();
        assertEquals("127.0.0.1", result.getIp());
        assertEquals(200, result.getStatus());
    }

    @Test
    public void shouldNotLogHttpRequestInfoWhenItsHealthCheckRequest() throws Exception {
        String[] healthCheckPaths = {"/liveness_check", "/readiness_check"};
        for (String path : healthCheckPaths) {
            when(requestInfo.getUri()).thenReturn("http:/" + path);
            responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
            assertEquals(200, servletResponse.getStatus());
            ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
            verify(logger, never()).request(argument.capture());
        }
    }

    @Test
    public void shouldReturn200WhenItsSwaggerRequest() throws Exception {
        String[] swaggerPaths = {"/swagger", "/v2/api-docs", "/configuration/ui", "/webjars/"};
        for (String path : swaggerPaths) {
            when(requestInfo.getUri()).thenReturn("http:/" + path);
            responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
            assertEquals(200, servletResponse.getStatus());
        }
    }

    @Test
    public void shouldReturn200WhenItsHealthCheckRequest() throws Exception {
        String[] healthCheckPaths = {"/liveness_check", "/readiness_check"};
        for (String path : healthCheckPaths) {
            when(requestInfo.getUri()).thenReturn("http:/" + path);
            responseLogFilter.doFilter(servletRequest, servletResponse, filterChain);
            assertEquals(200, servletResponse.getStatus());
        }
    }

    @Test
    public void shouldSetCorrectResponseHeaders() throws IOException, ServletException {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
        FilterChain filterChain = Mockito.mock(FilterChain.class);
        Mockito.when(requestInfo.getUri()).thenReturn("https://test.com");
        Mockito.when(headers.getCorrelationId()).thenReturn("correlation-id-value");
        Mockito.when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getServletContext()).thenReturn(context);
        when(context.getAttribute("starttime")).thenReturn(null);
        org.springframework.test.util.ReflectionTestUtils.setField(responseLogFilter, "ACCESS_CONTROL_ALLOW_ORIGIN_DOMAINS", "custom-domain");

        responseLogFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        Mockito.verify(httpServletResponse).addHeader("Access-Control-Allow-Origin", "custom-domain");
        Mockito.verify(httpServletResponse).addHeader("Access-Control-Allow-Headers", "access-control-allow-origin, origin, content-type, accept, authorization, data-partition-id, correlation-id, appkey");
        Mockito.verify(httpServletResponse).addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        Mockito.verify(httpServletResponse).addHeader("Access-Control-Allow-Credentials", "true");
        Mockito.verify(httpServletResponse).addHeader("X-Frame-Options", "DENY");
        Mockito.verify(httpServletResponse).addHeader("X-XSS-Protection", "1; mode=block");
        Mockito.verify(httpServletResponse).addHeader("X-Content-Type-Options", "nosniff");
        Mockito.verify(httpServletResponse).addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        Mockito.verify(httpServletResponse).addHeader("Content-Security-Policy", "default-src 'self'");
        Mockito.verify(httpServletResponse).addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        Mockito.verify(httpServletResponse).addHeader("Expires", "0");
        Mockito.verify(httpServletResponse).addHeader("correlation-id", "correlation-id-value");
        Mockito.verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
        verify(logger).request(eq(ResponseLogFilter.class.getName()), argument.capture());
    }

    private HttpServletResponse mockHttpServletResponse() {
        return new HttpServletResponse() {
            int response;

            @Override
            public void addCookie(Cookie cookie) {
            }

            @Override
            public boolean containsHeader(String s) {
                return false;
            }

            @Override
            public String encodeURL(String s) {
                return null;
            }

            @Override
            public String encodeRedirectURL(String s) {
                return null;
            }

            @Override
            public void sendError(int i, String s) {
            }

            @Override
            public void sendError(int i) {
            }

            @Override
            public void sendRedirect(String s) {
            }

            @Override
            public void setDateHeader(String s, long l) {
            }

            @Override
            public void addDateHeader(String s, long l) {
            }

            @Override
            public void setHeader(String s, String s1) {
            }

            @Override
            public void addHeader(String s, String s1) {
            }

            @Override
            public void setIntHeader(String s, int i) {
            }

            @Override
            public void addIntHeader(String s, int i) {
            }

            @Override
            public void setStatus(int i) {
                response = i;
            }

            @Override
            public int getStatus() {
                return response;
            }

            @Override
            public String getHeader(String s) {
                return null;
            }

            @Override
            public Collection<String> getHeaders(String s) {
                return null;
            }

            @Override
            public Collection<String> getHeaderNames() {
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public ServletOutputStream getOutputStream() {
                return null;
            }

            @Override
            public PrintWriter getWriter() {
                return null;
            }

            @Override
            public void setCharacterEncoding(String s) {
            }

            @Override
            public void setContentLength(int i) {
            }

            @Override
            public void setContentLengthLong(long l) {
            }

            @Override
            public void setContentType(String s) {
            }

            @Override
            public void setBufferSize(int i) {
            }

            @Override
            public int getBufferSize() {
                return 0;
            }

            @Override
            public void flushBuffer() {
            }

            @Override
            public void resetBuffer() {
            }

            @Override
            public boolean isCommitted() {
                return false;
            }

            @Override
            public void reset() {
            }

            @Override
            public void setLocale(Locale locale) {
            }

            @Override
            public Locale getLocale() {
                return null;
            }
        };
    }
}
