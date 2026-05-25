package org.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
@EnableJpaRepositories(basePackages = "org.example.repository")
@ComponentScan(basePackages = {
        "org.example.features"
})
public class RootConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public DataSource dataSource(Environment environment) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getRequiredProperty(environment, "spring.datasource.url"));
        config.setUsername(getRequiredProperty(environment, "spring.datasource.username"));
        config.setPassword(getRequiredProperty(environment, "spring.datasource.password"));
        config.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver"));
        config.setMaximumPoolSize(environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class, 10));
        config.setMinimumIdle(environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class, 5));
        config.setConnectionTimeout(environment.getProperty("spring.datasource.hikari.connection-timeout", Long.class, 30000L));
        config.setValidationTimeout(environment.getProperty("spring.datasource.hikari.validation-timeout", Long.class, 5000L));
        config.setInitializationFailTimeout(environment.getProperty("spring.datasource.hikari.initialization-fail-timeout", Long.class, 60000L));
        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            Environment environment) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(environment.getProperty("spring.jpa.show-sql", Boolean.class, false));

        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("org.example.entity");
        factoryBean.setJpaVendorAdapter(vendorAdapter);
        factoryBean.setJpaProperties(jpaProperties(environment));
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }


    private Properties jpaProperties(Environment environment) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", environment.getProperty("spring.jpa.hibernate.ddl-auto", "validate"));
        properties.setProperty("hibernate.dialect", environment.getProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.MySQLDialect"));
        properties.setProperty("hibernate.format_sql", environment.getProperty("spring.jpa.properties.hibernate.format_sql", "false"));
        return properties;
    }

    private String getRequiredProperty(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required property is not set: " + key);
        }
        return value;
    }
}

