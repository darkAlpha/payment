package org.tars.gateway.security;

import java.util.Set;

/**
 * Authentication result record.
 */
public record AuthenticationResult(String subject, Set<String> roles, String authType) {}

