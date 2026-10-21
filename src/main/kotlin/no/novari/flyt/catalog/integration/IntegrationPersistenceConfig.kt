package no.novari.flyt.catalog.integration

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.ValidationMode
import no.novari.flyt.catalog.database.CatalogSchemas
import no.novari.flyt.catalog.database.catalogDataSource
import no.novari.flyt.catalog.database.catalogEntityManagerFactory
import no.novari.flyt.catalog.database.catalogFlyway
import org.flywaydb.core.Flyway
import org.hibernate.cfg.AvailableSettings
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
    basePackages = ["no.novari.flyt.catalog.integration"],
    entityManagerFactoryRef = "integrationEntityManagerFactory",
    transactionManagerRef = "integrationTransactionManager",
)
class IntegrationPersistenceConfig {
    @Bean
    @ConfigurationProperties("novari.flyt.catalog.datasource.integration")
    fun integrationDataSource(
        properties: DataSourceProperties,
        schemas: CatalogSchemas,
    ): HikariDataSource = catalogDataSource(properties, schemas.integration)

    @Bean(initMethod = "migrate")
    fun integrationFlyway(
        @Qualifier("integrationDataSource") dataSource: DataSource,
        schemas: CatalogSchemas,
    ): Flyway = catalogFlyway(dataSource, schemas.integration, "integration")

    @Bean
    @DependsOn("integrationFlyway")
    fun integrationEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("integrationDataSource") dataSource: DataSource,
        schemas: CatalogSchemas,
    ): LocalContainerEntityManagerFactoryBean =
        builder.catalogEntityManagerFactory(
            dataSource = dataSource,
            schema = schemas.integration,
            persistenceUnit = "integration",
            packages = listOf("no.novari.flyt.catalog.integration"),
            // Arvet fra integration-service, som har slått av Hibernates bean-validering ved lagring siden 2022.
            // Uten den kan en lagring som i dag går gjennom, feile på entitetens @NotBlank etter cutover.
            properties = mapOf(AvailableSettings.JAKARTA_VALIDATION_MODE to ValidationMode.NONE.name),
        )

    @Bean
    fun integrationTransactionManager(
        @Qualifier("integrationEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
