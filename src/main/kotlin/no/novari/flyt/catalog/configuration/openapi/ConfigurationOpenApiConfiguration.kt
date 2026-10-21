package no.novari.flyt.catalog.configuration.openapi

import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ConfigurationOpenApiConfiguration {
    @Bean
    fun configurationGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("configurations")
            .pathsToMatch("$INTERNAL_API/konfigurasjoner/**")
            .build()
}
