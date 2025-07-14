package jeffrey.testapp.server;

import cafe.jeffrey.jfr.events.http.HttpServerExchangeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

public class RequestInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestInterceptor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String JFR_HTTP_EVENT_ATTRIBUTE = "jfrHttpEvent";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpServerExchangeEvent event = new HttpServerExchangeEvent();
        event.begin();
        request.setAttribute(JFR_HTTP_EVENT_ATTRIBUTE, event);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        HttpServerExchangeEvent event = (HttpServerExchangeEvent) request.getAttribute(JFR_HTTP_EVENT_ATTRIBUTE);
        if (event != null) {

            if (event.shouldCommit()) {
                event.end();
                event.method = request.getMethod();
                event.uri = request.getRequestURI();
                event.status = response.getStatus();
                event.requestLength = request.getContentLengthLong();
                event.responseLength = getResponseContentLength(response);
                event.remoteHost = getRemoteAddress(request);
                event.remotePort = request.getRemotePort();
                event.mediaType = request.getContentType();
                event.queryParams = getQueryParamsAsJson(request);
                event.pathParams = getPathParamsAsJson(request);
                event.commit();
            }
        } else {
            LOG.error("HttpServerExchangeEvent is null in afterCompletion");
        }
    }

    private long getResponseContentLength(HttpServletResponse response) {
        String contentLength = response.getHeader("Content-Length");
        if (contentLength != null) {
            try {
                return Long.parseLong(contentLength);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private String getRemoteAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String getQueryParamsAsJson(HttpServletRequest request) {
        ObjectNode queryParams = OBJECT_MAPPER.createObjectNode();

        if (request.getQueryString() != null) {
            String[] params = request.getQueryString().split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }

        return queryParams.toString();
    }

    private String getPathParamsAsJson(HttpServletRequest request) {
        ObjectNode pathParams = OBJECT_MAPPER.createObjectNode();

        // Spring path variables are typically stored as request attributes
        // This is a simplified approach - in a real Spring application,
        // you'd need to access the HandlerMapping attributes
        Object pathVariables = request.getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables");
        if (pathVariables instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> pathVars = (Map<String, String>) pathVariables;
            for (Map.Entry<String, String> entry : pathVars.entrySet()) {
                pathParams.put(entry.getKey(), entry.getValue());
            }
        }

        return pathParams.toString();
    }
}
