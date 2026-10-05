package no.novari.flyt.catalog.valueconverting.openapi

import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ValueConvertingOpenApiConfiguration {
    @Bean
    fun valueConvertingGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("value-convertings")
            .pathsToMatch(
                "$INTERNAL_API/value-convertings",
                "$INTERNAL_API/value-convertings/**",
            ).build()
}
