// Owner: HeenuReet | Final Integration | HandlerInterceptor blocking unauthenticated access with /login redirect
package com.shopping.system.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class SessionInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Allow static resources and public pages
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")
                || uri.startsWith("/static/") || uri.equals("/login") || uri.equals("/register")
                || uri.equals("/") || uri.startsWith("/webjars/") || uri.startsWith("/favicon")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        return true;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this)
                .excludePathPatterns("/login", "/register", "/css/**", "/js/**", "/images/**",
                        "/static/**", "/webjars/**", "/favicon.ico", "/");
    }
}
