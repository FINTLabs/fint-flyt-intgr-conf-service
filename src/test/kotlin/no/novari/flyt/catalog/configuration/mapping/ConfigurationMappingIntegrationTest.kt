package no.novari.flyt.catalog.configuration.mapping

import no.novari.flyt.audit.actor.ActorDisplayProperties
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.audit.actor.NoOpActorNameLookup
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.Configuration
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SpringBootTest(
    classes = [
        ObjectsFromCollectionMappingMappingService::class,
        ObjectCollectionMappingMappingService::class,
        ValuesFromCollectionMappingMappingService::class,
        ValueCollectionMappingMappingService::class,
        InstanceCollectionReferencesMappingService::class,
        PerKeyMappingService::class,
        ValueMappingMappingService::class,
        ObjectMappingMappingService::class,
        ConfigurationMappingService::class,
        ConfigurationMappingIntegrationTest.ActorDisplayResolverConfiguration::class,
    ],
)
class ConfigurationMappingIntegrationTest {
    @TestConfiguration
    class ActorDisplayResolverConfiguration {
        @Bean
        fun actorDisplayResolver() = ActorDisplayResolver(NoOpActorNameLookup(), ActorDisplayProperties())
    }

    @Autowired
    lateinit var configurationMappingService: ConfigurationMappingService

    private lateinit var configuration: Configuration
    private lateinit var configurationDto: ConfigurationDto

    @BeforeEach
    fun setUp() {
        configurationDto =
            ConfigurationDto
                .builder()
                .integrationId(1L)
                .integrationMetadataId(2L)
                .mapping(
                    ObjectMappingDto
                        .builder()
                        .valueMappingPerKey(
                            mutableMapOf(
                                "field" to
                                    ValueMappingDto
                                        .builder()
                                        .type(ValueMapping.Type.STRING)
                                        .mappingString("abc")
                                        .build(),
                            ),
                        ).build(),
                ).build()

        configuration = configurationMappingService.toEntity(configurationDto)
    }

    @Test
    fun `keeps all values when mapping an entity to a dto with the mapping included`() {
        val result = configurationMappingService.toDto(configuration, false)
        assertEquals(configurationDto, result)
    }

    @Test
    fun `keeps all values except the mapping when it is excluded`() {
        val result = configurationMappingService.toDto(configuration, true)

        assertEquals(
            ConfigurationDto
                .builder()
                .integrationId(1L)
                .integrationMetadataId(2L)
                .build(),
            result,
        )
    }

    @Test
    fun `keeps all values through a dto to entity to dto round trip`() {
        val firstResult = configurationMappingService.toEntity(configurationDto)
        val secondResult = configurationMappingService.toDto(firstResult, false)
        assertEquals(configurationDto, secondResult)
    }
}
