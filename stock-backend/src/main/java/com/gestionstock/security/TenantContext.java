package com.gestionstock.security;

public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_ORG = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setOrganisationId(Long orgId) {
        CURRENT_ORG.set(orgId);
    }

    public static Long getOrganisationId() {
        return CURRENT_ORG.get();
    }

    public static Long requireOrganisationId() {
        Long orgId = getOrganisationId();
        if (orgId == null) {
            throw new IllegalStateException("No tenant available for current request");
        }
        return orgId;
    }

    public static void clear() {
        CURRENT_ORG.remove();
    }
}
