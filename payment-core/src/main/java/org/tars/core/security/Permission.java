package org.tars.core.security;

/**
 * RBAC permission constants for method-level security.
 */
public final class Permission {

    private Permission() {}

    public static final String DEPOSIT_CREATE = "hasAnyRole('TELLER', 'ADMIN', 'SYSTEM')";
    public static final String DEPOSIT_CLOSE = "hasAnyRole('TELLER', 'ADMIN')";
    public static final String DEPOSIT_TRANSFER = "hasAnyRole('TELLER', 'ADMIN', 'SYSTEM')";
    public static final String DEPOSIT_VIEW = "hasAnyRole('TELLER', 'ADMIN', 'AUDITOR')";
    public static final String DEPOSIT_EOD = "hasRole('SYSTEM')";
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";
}
