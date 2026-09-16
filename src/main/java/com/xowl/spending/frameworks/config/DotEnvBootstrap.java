package com.xowl.spending.frameworks.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

/** Loads {@code .env} from the process working directory into system properties (only if not already set in the environment). */
public final class DotEnvBootstrap {

    private static boolean loaded;

    private DotEnvBootstrap() {}

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        for (DotenvEntry e : dotenv.entries()) {
            String key = e.getKey();
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, e.getValue());
            }
        }
    }
}
