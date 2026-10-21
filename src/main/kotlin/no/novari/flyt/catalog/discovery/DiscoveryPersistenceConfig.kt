package no.novari.flyt.catalog.discovery

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
    basePackages = ["no.novari.flyt.catalog.discovery"],
    entityManagerFactoryRef = "discoveryEntityManagerFactory",
    transactionManagerRef = "discoveryTransactionManager",
)
class DiscoveryPersistenceConfig {
    @Bean
    @ConfigurationProperties("novari.flyt.catalog.datasource.discovery")
    fun discoveryDataSource(
        properties: DataSourceProperties,
        schemas: CatalogSchemas,
    ): HikariDataSource = catalogDataSource(properties, schemas.discovery)

    @Bean(initMethod = "migrate")
    fun discoveryFlyway(
        @Qualifier("discoveryDataSource") dataSource: DataSource,
        schemas: CatalogSchemas,
    ): Flyway = catalogFlyway(dataSource, schemas.discovery, "discovery")

    @Bean
    @DependsOn("discoveryFlyway")
    fun discoveryEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("discoveryDataSource") dataSource: DataSource,
        schemas: CatalogSchemas,
    ): LocalContainerEntityManagerFactoryBean =
        builder.catalogEntityManagerFactory(
            dataSource = dataSource,
            schema = schemas.discovery,
            persistenceUnit = "discovery",
            packages = listOf("no.novari.flyt.catalog.discovery"),
        )

    @Bean
    fun discoveryTransactionManager(
        @Qualifier("discoveryEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
