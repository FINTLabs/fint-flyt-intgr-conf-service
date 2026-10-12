package no.novari.flyt.catalog.valueconverting

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import no.novari.flyt.catalog.database.CatalogSchemas
import no.novari.flyt.catalog.database.catalogDataSource
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
import org.springframework.orm.jpa.SharedEntityManagerCreator
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
    basePackages = ["no.novari.flyt.catalog.valueconverting"],
    entityManagerFactoryRef = "valueConvertingEntityManagerFactory",
    transactionManagerRef = "valueConvertingTransactionManager",
)
class ValueConvertingPersistenceConfig {
    @Bean
    @ConfigurationProperties("novari.flyt.catalog.datasource.value-converting")
    fun valueConvertingDataSource(
        properties: DataSourceProperties,
        schemas: CatalogSchemas,
    ): HikariDataSource = catalogDataSource(properties, schemas.valueConverting)

    @Bean(initMethod = "migrate")
    fun valueConvertingFlyway(
        @Qualifier("valueConvertingDataSource") dataSource: DataSource,
        schemas: CatalogSchemas,
    ): Flyway = catalogFlyway(dataSource, schemas.valueConverting, "valueconverting")

    @Bean
    @DependsOn("valueConvertingFlyway")
    fun valueConvertingEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("valueConvertingDataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(dataSource)
            .packages("no.novari.flyt.catalog.valueconverting", "no.novari.flyt.audit.revision")
            .persistenceUnit("valueConverting")
            .build()

    @Bean
    fun valueConvertingTransactionManager(
        @Qualifier("valueConvertingEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)

    /**
     * `EnversHistoryService` tar `EntityManager` i konstruktøren, og med fem persistence units må
     * kilden navngis. Uten denne bønnen ville injiseringen enten vært flertydig eller falt tilbake
     * på primærkandidaten, som peker på tjenestens tomme skjema.
     */
    @Bean
    fun valueConvertingEntityManager(
        @Qualifier("valueConvertingEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): EntityManager = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory)
}
