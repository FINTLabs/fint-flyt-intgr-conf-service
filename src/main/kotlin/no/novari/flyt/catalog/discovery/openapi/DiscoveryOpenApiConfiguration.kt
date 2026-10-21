package no.novari.flyt.catalog.discovery.openapi

import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DiscoveryOpenApiConfiguration {
    @Bean
    fun discoveryGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("discovery")
            .pathsToMatch(
                "$INTERNAL_API/metadata",
                "$INTERNAL_API/metadata/**",
            ).build()
}
