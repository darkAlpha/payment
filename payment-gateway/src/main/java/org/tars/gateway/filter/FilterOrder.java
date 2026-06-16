package org.tars.gateway.filter;

/**
 * Standard filter ordering constants.
 */
public final class FilterOrder {
    private FilterOrder() {}

    public static final int CORS           = -100;
    public static final int REQUEST_ID     = -50;
    public static final int ACCESS_LOG     = 0;
    public static final int RATE_LIMIT     = 100;
    public static final int AUTHENTICATION = 200;
    public static final int AUTHORIZATION  = 300;
    public static final int FEATURE_FLAG   = 400;
    public static final int ROUTE_RESOLVE  = 600;
    public static final int LOAD_BALANCE   = 700;
    public static final int PROXY          = 900;
}

