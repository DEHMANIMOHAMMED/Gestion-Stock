package com.gestionstock.security;

import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, jakarta.servlet.ServletException {

        try {
            String authHeader = request.getHeader("Authorization");

            if ((authHeader == null || !authHeader.startsWith("Bearer "))
                    && !"/notifications/stream".equals(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = authHeader != null && authHeader.startsWith("Bearer ")
                    ? authHeader.substring(7)
                    : request.getParameter("access_token");
            if (jwt == null || jwt.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }
            String email;

            try {
                email = jwtService.extractEmail(jwt);
            } catch (JwtException | IllegalArgumentException e) {
                filterChain.doFilter(request, response);
                return;
            }

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userRepository.findByEmail(email).orElse(null);

                if (userDetails instanceof User user && jwtService.isTokenValid(jwt, user)) {
                    Long orgId = jwtService.extractOrganisationId(jwt);
                    if (!orgId.equals(user.getOrganisation().getId())) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    user.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    TenantContext.setOrganisationId(orgId);
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
}
