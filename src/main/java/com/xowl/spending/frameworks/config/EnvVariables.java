package com.xowl.spending.frameworks.config;

/** Resolves variables from the OS environment first, then from system properties (e.g. after {@link DotEnvBootstrap}). */
public final class EnvVariables {

    private EnvVariables() {}

    public static String get(String key) {
        String v = System.getenv(key);
        if (!isBlank(v)) {
            return v.trim();
        }
        v = System.getProperty(key);
        return isBlank(v) ? null : v.trim();
    }

    /** The value, or an exception naming the variable that is missing. */
    public static String required(String key) {
        String v = get(key);
        if (isBlank(v)) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return v;
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
