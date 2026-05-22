package com.gestionstock.security;

import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final AuthenticatedUserProvider userProvider;

    public User requireOwner() {
        User user = userProvider.requireUser();
        if (user.getRole() != Role.OWNER) {
            throw new AccessDeniedException("OWNER role required");
        }
        return user;
    }

    public User requireAdmin() {
        User user = userProvider.requireUser();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("ADMIN role required");
        }
        return user;
    }

    public User requireAdminOrOwner() {
        User user = userProvider.requireUser();
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.OWNER) {
            throw new AccessDeniedException("ADMIN or OWNER role required");
        }
        return user;
    }

    public User requireAuthenticatedTenantUser() {
        User user = userProvider.requireUser();
        if (user.getRole() == Role.OWNER) {
            throw new AccessDeniedException("Tenant role required");
        }
        return user;
    }
}
