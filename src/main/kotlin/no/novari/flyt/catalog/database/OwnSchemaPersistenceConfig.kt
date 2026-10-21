package no.novari.flyt.catalog.database

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Tjenestens eget skjema, opprettet av pgerator etter applikasjonsnavnet. Det er tomt og ubrukt, men
 * er primærkandidaten Spring Boots autokonfigurasjon krever — og det er valgt nettopp fordi det er tomt:
 * en `@Transactional`, `EntityManager` eller et repository som glemmer sin qualifier havner her og feiler
 * på manglende tabell, framfor å lese eller skrive stille mot et av de fire domenenes ekte data.
 *
 * Uten primærkandidat feiler autokonfigurasjonen på flertydighet — blant annet er JpaBaseConfiguration
 * `@ConditionalOnSingleCandidate`.
 */
@Configuration(proxyBeanMethods = false)
class OwnSchemaPersistenceConfig {
    @Bean
    @Primary
    @ConfigurationProperties("novari.flyt.catalog.datasource.own")
    fun ownSchemaDataSource(
        properties: DataSourceProperties,
        schemas: CatalogSchemas,
    ): HikariDataSource = catalogDataSource(properties, schemas.own)

    @Bean
    @Primary
    fun ownSchemaEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("ownSchemaDataSource") dataSource: DataSource,
        schemas: CatalogSchemas,
    ): LocalContainerEntityManagerFactoryBean =
        // Ingen pakker: uten packagesToScan skanner Hibernate classpathen og ville dratt inn
        // domenenes entiteter etter hvert som de flyttes inn.
        builder.catalogEntityManagerFactory(
            dataSource = dataSource,
            schema = schemas.own,
            persistenceUnit = "own",
            packages = emptyList(),
        )

    @Bean
    @Primary
    fun ownSchemaTransactionManager(
        @Qualifier("ownSchemaEntityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
