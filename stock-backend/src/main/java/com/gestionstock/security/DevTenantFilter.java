package com.gestionstock.security;

import jakarta.servlet.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Profile("dev")
public class DevTenantFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
