package com.codex.trimlink.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.codex.trimlink.rateLimit.RateLimiter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingFilter implements Filter {

    private final RateLimiter rateLimiter;

    // DEPENDENCY INJECTION: The RateLimiter bean will be injected by Spring -> implements Dependency Inversion Principle
    public RateLimitingFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws java.io.IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientId = httpRequest.getHeader("X-Client-ID");
        if (clientId == null || clientId.isEmpty()) {
            clientId = httpRequest.getRemoteAddr(); // Fallback to IP address if no client ID is provided
        }

        // Evaluate the request against the rate limiter
        if (!rateLimiter.isAllowed(clientId)) {
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("text/plain");
            httpResponse.getWriter().write("Too Many Requests: Try again later.");
            return;
        }

        chain.doFilter(request, response);
    }
    
}
