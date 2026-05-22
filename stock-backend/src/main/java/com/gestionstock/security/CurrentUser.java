package com.gestionstock.security;

import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    public static User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) return user;

        return null;
    }

    public static boolean isAdmin() {
        User user = get();
        return user != null && user.getRole() == Role.ADMIN;
    }

    public static boolean isOwner() {
        User user = get();
        return user != null && user.getRole() == Role.OWNER;
    }
}
