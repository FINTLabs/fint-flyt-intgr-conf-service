package no.novari.flyt.catalog.configuration

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import no.novari.flyt.catalog.database.CatalogSchemas
import no.novari.flyt.catalog.database.catalogDataSource
import no.novari.flyt.catalog.database.catalogEntityManagerFactory
import no.novari.flyt.catalog.database.catalogFlyway
import org.flywaydb.core.Flyway
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
    basePackages = ["no.novari.flyt.catalog.configuration"],
    entityManagerFactoryRef = "configurationEntityManagerFactory",
    transactionManagerRef = "configurationTransactionManager",
)
class ConfigurationPersistenceConfig {
    @Bean
    @ConfigurationProperties("novari.flyt.catalog.datasource.configuration")
    fun configurationDataSource(
        properties: DataSourceProperties,
        schemas: CatalogSchemas,
    ): HikariDataSource = catalogDataSource(properties, schemas.configuration)

    @Bean(initMethod = "migrate")
    fun configurationFlyway(
        @Qualifier("configurationDataSource") dataSource: DataSource,
        schemas: CatalogSchemas,
    ): Flyway = catalogFlyway(dataSource, schemas.configuration, "configuration")

    @Bean
    @DependsOn("configurationFlyway")
    fun configurationEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("configurationDataSource") dataSource: DataSource,
        schemas: CatalogSchemas,
    ): LocalContainerEntityManagerFactoryBean =
        builder.catalogEntityManagerFactory(
            dataSource = dataSource,
            schema = schemas.configuration,
            persistenceUnit = "configuration",
            packages =
                listOf(
                    "no.novari.flyt.catalog.configuration",
                    "no.novari.flyt.audit.revision",
                ),
        )

    @Bean
    fun configurationTransactionManager(
        @Qualifier("configurationEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
