package no.novari.flyt.catalog.discovery.mapping

import no.novari.flyt.audit.actor.ActorDisplayProperties
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.audit.actor.ActorNameLookup
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataCategoryDto
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.dtos.InstanceObjectCollectionMetadataDto
import no.novari.flyt.catalog.discovery.model.dtos.InstanceValueMetadataDto
import no.novari.flyt.catalog.discovery.model.dtos.IntegrationMetadataDto
import no.novari.flyt.catalog.discovery.model.entities.InstanceMetadataCategory
import no.novari.flyt.catalog.discovery.model.entities.InstanceMetadataContent
import no.novari.flyt.catalog.discovery.model.entities.InstanceObjectCollectionMetadata
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SpringBootTest(
    classes = [
        IntegrationMetadataMappingService::class,
        InstanceMetadataContentMappingService::class,
        InstanceValueMetadataMappingService::class,
        InstanceObjectCollectionMetadataMappingService::class,
        InstanceMetadataCategoryMappingService::class,
        IntegrationMetadataMappingIntegrationTest.TestConfig::class,
    ],
)
class IntegrationMetadataMappingIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun actorNameLookup(): ActorNameLookup = ActorNameLookup { emptyMap() }

        @Bean
        fun actorDisplayResolver(lookup: ActorNameLookup) = ActorDisplayResolver(lookup, ActorDisplayProperties())
    }

    @Autowired
    private lateinit var integrationMetadataMappingService: IntegrationMetadataMappingService

    private lateinit var integrationMetadataDto: IntegrationMetadataDto
    private lateinit var expectedIntegrationMetadata: IntegrationMetadata

    @BeforeEach
    fun setup() {
        val nestedObjectCollectionValueDto =
            InstanceValueMetadataDto(
                displayName = OBJECT_COLLECTION_VALUE_DISPLAY_NAME,
                type = InstanceValueMetadata.Type.STRING,
                key = OBJECT_COLLECTION_VALUE_KEY,
            )
        val nestedObjectCollectionValue =
            InstanceValueMetadata(
                displayName = OBJECT_COLLECTION_VALUE_DISPLAY_NAME,
                type = InstanceValueMetadata.Type.STRING,
                key = OBJECT_COLLECTION_VALUE_KEY,
            )

        integrationMetadataDto =
            IntegrationMetadataDto(
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "Test-1",
                version = 1L,
                integrationDisplayName = "displayName",
                instanceMetadata =
                    InstanceMetadataContentDto(
                        instanceValueMetadata =
                            listOf(
                                InstanceValueMetadataDto(
                                    displayName = "value1DisplayName",
                                    type = InstanceValueMetadata.Type.STRING,
                                    key = "value1Key",
                                ),
                                InstanceValueMetadataDto(
                                    displayName = "value2DisplayName",
                                    type = InstanceValueMetadata.Type.STRING,
                                    key = "value2Key",
                                ),
                            ),
                        instanceObjectCollectionMetadata =
                            listOf(
                                InstanceObjectCollectionMetadataDto(
                                    displayName = "objectCollection1DisplayName",
                                    objectMetadata =
                                        InstanceMetadataContentDto(
                                            instanceValueMetadata =
                                                listOf(
                                                    InstanceValueMetadataDto(
                                                        displayName = "objectCollection1value1DisplayName",
                                                        type = InstanceValueMetadata.Type.BOOLEAN,
                                                        key = "objectCollection1value1Key",
                                                    ),
                                                ),
                                        ),
                                    key = "objectCollection1key",
                                ),
                                InstanceObjectCollectionMetadataDto(
                                    displayName = "objectCollection2DisplayName",
                                    objectMetadata =
                                        InstanceMetadataContentDto(
                                            instanceValueMetadata =
                                                listOf(
                                                    InstanceValueMetadataDto(
                                                        displayName = "objectCollection2value1DisplayName",
                                                        type = InstanceValueMetadata.Type.BOOLEAN,
                                                        key = "objectCollection2value1Key",
                                                    ),
                                                    InstanceValueMetadataDto(
                                                        displayName = "objectCollection2value2DisplayName",
                                                        type = InstanceValueMetadata.Type.FILE,
                                                        key = "objectCollection2value2Key",
                                                    ),
                                                ),
                                            instanceObjectCollectionMetadata =
                                                listOf(
                                                    InstanceObjectCollectionMetadataDto(
                                                        displayName = "objectCollection2objectCollection1DisplayName",
                                                        objectMetadata =
                                                            InstanceMetadataContentDto(
                                                                instanceValueMetadata =
                                                                    listOf(
                                                                        nestedObjectCollectionValueDto,
                                                                    ),
                                                            ),
                                                        key = "objectCollection2objectCollection1key",
                                                    ),
                                                ),
                                        ),
                                    key = "objectCollection2key",
                                ),
                            ),
                        categories =
                            listOf(
                                InstanceMetadataCategoryDto(
                                    displayName = "category1Key",
                                    content =
                                        InstanceMetadataContentDto(
                                            categories =
                                                listOf(
                                                    InstanceMetadataCategoryDto(
                                                        displayName = "category1category1Key",
                                                        content =
                                                            InstanceMetadataContentDto(
                                                                instanceValueMetadata =
                                                                    listOf(
                                                                        InstanceValueMetadataDto(
                                                                            displayName =
                                                                                "category1category1value1DisplayName",
                                                                            type = InstanceValueMetadata.Type.STRING,
                                                                            key = "category1category2value1Key",
                                                                        ),
                                                                    ),
                                                            ),
                                                    ),
                                                ),
                                        ),
                                ),
                                InstanceMetadataCategoryDto(
                                    displayName = "category2Key",
                                    content =
                                        InstanceMetadataContentDto(
                                            instanceValueMetadata =
                                                listOf(
                                                    InstanceValueMetadataDto(
                                                        displayName = "category2value1DisplayName",
                                                        type = InstanceValueMetadata.Type.STRING,
                                                        key = "category2value1Key",
                                                    ),
                                                ),
                                        ),
                                ),
                            ),
                    ),
            )

        expectedIntegrationMetadata =
            IntegrationMetadata(
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "Test-1",
                version = 1L,
                integrationDisplayName = "displayName",
                instanceMetadata =
                    InstanceMetadataContent(
                        instanceValueMetadata =
                            mutableListOf(
                                InstanceValueMetadata(
                                    displayName = "value1DisplayName",
                                    type = InstanceValueMetadata.Type.STRING,
                                    key = "value1Key",
                                ),
                                InstanceValueMetadata(
                                    displayName = "value2DisplayName",
                                    type = InstanceValueMetadata.Type.STRING,
                                    key = "value2Key",
                                ),
                            ),
                        instanceObjectCollectionMetadata =
                            mutableListOf(
                                InstanceObjectCollectionMetadata(
                                    displayName = "objectCollection1DisplayName",
                                    objectMetadata =
                                        InstanceMetadataContent(
                                            instanceValueMetadata =
                                                mutableListOf(
                                                    InstanceValueMetadata(
                                                        displayName = "objectCollection1value1DisplayName",
                                                        type = InstanceValueMetadata.Type.BOOLEAN,
                                                        key = "objectCollection1value1Key",
                                                    ),
                                                ),
                                        ),
                                    key = "objectCollection1key",
                                ),
                                InstanceObjectCollectionMetadata(
                                    displayName = "objectCollection2DisplayName",
                                    objectMetadata =
                                        InstanceMetadataContent(
                                            instanceValueMetadata =
                                                mutableListOf(
                                                    InstanceValueMetadata(
                                                        displayName = "objectCollection2value1DisplayName",
                                                        type = InstanceValueMetadata.Type.BOOLEAN,
                                                        key = "objectCollection2value1Key",
                                                    ),
                                                    InstanceValueMetadata(
                                                        displayName = "objectCollection2value2DisplayName",
                                                        type = InstanceValueMetadata.Type.FILE,
                                                        key = "objectCollection2value2Key",
                                                    ),
                                                ),
                                            instanceObjectCollectionMetadata =
                                                mutableListOf(
                                                    InstanceObjectCollectionMetadata(
                                                        displayName = "objectCollection2objectCollection1DisplayName",
                                                        objectMetadata =
                                                            InstanceMetadataContent(
                                                                instanceValueMetadata =
                                                                    mutableListOf(
                                                                        nestedObjectCollectionValue,
                                                                    ),
                                                            ),
                                                        key = "objectCollection2objectCollection1key",
                                                    ),
                                                ),
                                        ),
                                    key = "objectCollection2key",
                                ),
                            ),
                        categories =
                            mutableListOf(
                                InstanceMetadataCategory(
                                    displayName = "category1Key",
                                    content =
                                        InstanceMetadataContent(
                                            categories =
                                                mutableListOf(
                                                    InstanceMetadataCategory(
                                                        displayName = "category1category1Key",
                                                        content =
                                                            InstanceMetadataContent(
                                                                instanceValueMetadata =
                                                                    mutableListOf(
                                                                        InstanceValueMetadata(
                                                                            displayName =
                                                                                "category1category1value1DisplayName",
                                                                            type = InstanceValueMetadata.Type.STRING,
                                                                            key = "category1category2value1Key",
                                                                        ),
                                                                    ),
                                                            ),
                                                    ),
                                                ),
                                        ),
                                ),
                                InstanceMetadataCategory(
                                    displayName = "category2Key",
                                    content =
                                        InstanceMetadataContent(
                                            instanceValueMetadata =
                                                mutableListOf(
                                                    InstanceValueMetadata(
                                                        displayName = "category2value1DisplayName",
                                                        type = InstanceValueMetadata.Type.STRING,
                                                        key = "category2value1Key",
                                                    ),
                                                ),
                                        ),
                                ),
                            ),
                    ),
            )
    }

    @Test
    fun `maps dto to integration metadata entity`() {
        val integrationMetadata = integrationMetadataMappingService.toEntity(integrationMetadataDto)

        assertEquals(expectedIntegrationMetadata, integrationMetadata)
    }

    companion object {
        private const val OBJECT_COLLECTION_VALUE_DISPLAY_NAME =
            "objectCollection2objectCollection1value1DisplayName"
        private const val OBJECT_COLLECTION_VALUE_KEY =
            "objectCollection2objectCollection1value1Key"
    }
}
