package no.novari.flyt.catalog.database

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CatalogDatabaseProperties::class)
class CatalogDatabaseConfig {
    @Bean
    fun catalogSchemas(
        properties: CatalogDatabaseProperties,
        dataSourceProperties: DataSourceProperties,
    ): CatalogSchemas =
        CatalogSchemas(
            properties.schemaPrefix ?: CatalogSchemas.schemaPrefixFrom(dataSourceProperties.username),
        )
}
