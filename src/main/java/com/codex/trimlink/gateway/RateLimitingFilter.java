package com.codex.trimlink.gateway;

import org.springframework.http.HttpStatus;
import com.codex.trimlink.rateLimit.RateLimiter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RateLimitingFilter implements Filter {

    private final RateLimiter rateLimiter;

    public RateLimitingFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws java.io.IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientId = httpRequest.getHeader("X-Client-ID");
        if (clientId == null || clientId.isEmpty()) {
            clientId = httpRequest.getRemoteAddr();
        }

        if (!rateLimiter.isAllowed(clientId)) {
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("text/plain");
            httpResponse.getWriter().write("Too Many Requests: Try again later.");
            return;
        }

        chain.doFilter(request, response);
    }
}