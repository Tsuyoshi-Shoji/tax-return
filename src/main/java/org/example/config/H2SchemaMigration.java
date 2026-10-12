package org.example.config;

import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public final class H2SchemaMigration {
    private static final String H2_URL_PREFIX = "jdbc:h2:";

    private H2SchemaMigration() {
    }

    public static void migrateIfConfigured(DataSource dataSource, String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(H2_URL_PREFIX)) {
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.addScript(new ClassPathResource("db/h2/schema.sql"));
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
