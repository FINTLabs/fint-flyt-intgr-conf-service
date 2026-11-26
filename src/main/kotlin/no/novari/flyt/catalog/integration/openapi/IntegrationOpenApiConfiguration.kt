package no.novari.flyt.catalog.integration.openapi

import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_CLIENT_API
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IntegrationOpenApiConfiguration {
    @Bean
    fun integrationGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("integrations")
            .pathsToMatch(
                "$INTERNAL_API/integrasjoner/**",
                "$INTERNAL_CLIENT_API/integrasjoner",
                "$INTERNAL_CLIENT_API/integrasjoner/**",
            ).build()
}
