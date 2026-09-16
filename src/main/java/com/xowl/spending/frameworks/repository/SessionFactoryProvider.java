package com.xowl.spending.frameworks.repository;

import com.xowl.spending.frameworks.config.EnvVariables;
import com.xowl.spending.frameworks.exceptions.DatabaseInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class SessionFactoryProvider {

    public static final String ENV_DB_URL = "CONTROL_GASTOS_DB_URL";
    public static final String ENV_DB_USER = "CONTROL_GASTOS_DB_USER";
    public static final String ENV_DB_PASSWORD = "CONTROL_GASTOS_DB_PASSWORD";

    private static SessionFactory instance;

    /**
     * None of the three has a default.
     *
     * Where the database lives is a fact about one deployment, not about this
     * program, and hibernate.cfg.xml used to carry all three in plain text.
     * Missing any of them fails naming the variable rather than quietly
     * connecting somewhere nobody asked for.
     */
    public SessionFactory provide() {
        if (instance == null) {
            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .configure()
                    .applySetting("hibernate.connection.url", EnvVariables.required(ENV_DB_URL))
                    .applySetting("hibernate.connection.username", EnvVariables.required(ENV_DB_USER))
                    .applySetting("hibernate.connection.password", EnvVariables.required(ENV_DB_PASSWORD))
                    .build();
            try {
                instance = new MetadataSources(registry).buildMetadata().buildSessionFactory();
            } catch (Exception e) {
                throw new DatabaseInitializationException(e.getMessage(), e.getCause());
            }
        }

        return instance;
    }
}
