package no.novari.flyt.catalog.configuration

import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.persistence.entity.Integration
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Uten `@DataJpaTest` er det ingen transaksjon som rulles tilbake mellom tester, og Envers skriver
 * uansett først ved commit. Radene må derfor ryddes eksplisitt.
 */
fun JdbcTemplate.clearConfiguration() {
    val tables =
        queryForList(
            "select table_name from information_schema.tables where table_schema = current_schema() " +
                "and table_name <> 'flyway_schema_history'",
            String::class.java,
        )
    execute("truncate table ${tables.joinToString()} restart identity cascade")
}

fun integrationDto(
    sourceApplicationId: Long = 1L,
    sourceApplicationIntegrationId: String = "kildeapp-integrasjon",
): IntegrationDto =
    IntegrationDto(
        id = 10L,
        sourceApplicationId = sourceApplicationId,
        sourceApplicationIntegrationId = sourceApplicationIntegrationId,
        destination = "arkiv",
        state = Integration.State.DEACTIVATED,
    )

fun integrationMetadataDto(
    sourceApplicationId: Long = 1L,
    sourceApplicationIntegrationId: String = "kildeapp-integrasjon",
    instanceMetadata: InstanceMetadataContentDto? = null,
): IntegrationMetadataDto =
    IntegrationMetadataDto(
        id = 100L,
        sourceApplicationId = sourceApplicationId,
        sourceApplicationIntegrationId = sourceApplicationIntegrationId,
        integrationDisplayName = "Integrasjon",
        version = 1L,
        instanceMetadata = instanceMetadata,
    )
