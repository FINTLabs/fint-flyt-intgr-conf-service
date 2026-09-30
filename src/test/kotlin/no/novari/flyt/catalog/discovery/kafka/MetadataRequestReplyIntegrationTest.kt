package no.novari.flyt.catalog.discovery.kafka

import no.novari.flyt.catalog.CatalogIntegrationTest
import no.novari.flyt.catalog.discovery.IntegrationMetadataRepository
import no.novari.flyt.catalog.discovery.clearDiscovery
import no.novari.flyt.catalog.discovery.model.entities.InstanceMetadataContent
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.requestreply.RequestProducerRecord
import no.novari.kafka.requestreply.RequestTemplate
import no.novari.kafka.requestreply.RequestTemplateFactory
import no.novari.kafka.requestreply.topic.ReplyTopicService
import no.novari.kafka.requestreply.topic.configuration.ReplyTopicConfiguration
import no.novari.kafka.requestreply.topic.name.ReplyTopicNameParameters
import no.novari.kafka.requestreply.topic.name.RequestTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import javax.sql.DataSource

/**
 * Kjører begge metadata-rundene mot ekte Kafka, med klienter bygget slik configuration-service
 * bygger sine — samme topic-parametre, samme svar-topic navngitt etter klienten, samme modellklasser.
 *
 * Akseptansekriteriet er at configuration-service ikke skal bygges eller deployes på nytt i dette
 * steget. Serialiseringstestene dekker payloaden, men ikke at serversiden faktisk lytter på det
 * topicet klienten produserer til og svarer på det klienten lytter på.
 */
class MetadataRequestReplyIntegrationTest : CatalogIntegrationTest() {
    @Autowired
    private lateinit var requestTemplateFactory: RequestTemplateFactory

    @Autowired
    private lateinit var replyTopicService: ReplyTopicService

    @Autowired
    private lateinit var integrationMetadataRepository: IntegrationMetadataRepository

    @Autowired
    @Qualifier("discoveryDataSource")
    private lateinit var discoveryDataSource: DataSource

    @AfterEach
    fun clearState() {
        JdbcTemplate(discoveryDataSource).clearDiscovery()
    }

    @Test
    fun `stored metadata comes back as the model configuration-service expects`() {
        val stored = integrationMetadataRepository.saveAndFlush(storedMetadata())

        val reply =
            configurationServiceClient("metadata", ClientIntegrationMetadata::class.java)
                .requestAndReceive(requestFor(METADATA_TOPIC, stored.id))
                .value()

        assertThat(reply).isNotNull()
        assertThat(reply.sourceApplicationId).isEqualTo(1L)
        assertThat(reply.sourceApplicationIntegrationId).isEqualTo("kildeapp-integrasjon")
        assertThat(reply.sourceApplicationIntegrationUri).isEqualTo("https://kildeapp.example/integrasjon/1")
        assertThat(reply.integrationDisplayName).isEqualTo("Byggesak")
        assertThat(reply.version).isEqualTo(4L)
    }

    @Test
    fun `an unknown metadata id comes back as an empty payload`() {
        val reply =
            configurationServiceClient("metadata", ClientIntegrationMetadata::class.java)
                .requestAndReceive(requestFor(METADATA_TOPIC, 9999L))
                .value()

        assertThat(reply).isNull()
    }

    @Test
    fun `stored instance metadata comes back as the tree configuration-service expects`() {
        val stored = integrationMetadataRepository.saveAndFlush(storedMetadata())

        val reply =
            configurationServiceClient("instance-metadata", ClientInstanceMetadataContent::class.java)
                .requestAndReceive(requestFor(INSTANCE_METADATA_TOPIC, stored.id))
                .value()

        assertThat(reply).isNotNull()
        assertThat(reply.instanceValueMetadata).hasSize(2)
        assertThat(reply.instanceValueMetadata.map { it.key }).containsExactly("tittel", "er-hastesak")
        assertThat(reply.instanceValueMetadata.map { it.type })
            .containsExactly(ClientInstanceValueMetadata.Type.STRING, ClientInstanceValueMetadata.Type.BOOLEAN)
    }

    @Test
    fun `an unknown instance metadata id comes back as an empty payload`() {
        val reply =
            configurationServiceClient("instance-metadata", ClientInstanceMetadataContent::class.java)
                .requestAndReceive(requestFor(INSTANCE_METADATA_TOPIC, 9999L))
                .value()

        assertThat(reply).isNull()
    }

    private fun storedMetadata() =
        IntegrationMetadata(
            sourceApplicationId = 1L,
            sourceApplicationIntegrationId = "kildeapp-integrasjon",
            sourceApplicationIntegrationUri = "https://kildeapp.example/integrasjon/1",
            integrationDisplayName = "Byggesak",
            version = 4L,
            instanceMetadata =
                InstanceMetadataContent(
                    instanceValueMetadata =
                        mutableListOf(
                            InstanceValueMetadata(
                                displayName = "Tittel",
                                type = InstanceValueMetadata.Type.STRING,
                                key = "tittel",
                            ),
                            InstanceValueMetadata(
                                displayName = "Hastesak",
                                type = InstanceValueMetadata.Type.BOOLEAN,
                                key = "er-hastesak",
                            ),
                        ),
                ),
        )

    private fun requestFor(
        topicNameParameters: RequestTopicNameParameters,
        metadataId: Long,
    ) = RequestProducerRecord
        .builder<Long>()
        .topicNameParameters(topicNameParameters)
        .value(metadataId)
        .build()

    private fun <T> configurationServiceClient(
        resourceName: String,
        replyType: Class<T>,
    ): RequestTemplate<Long, T> {
        val replyTopicNameParameters =
            ReplyTopicNameParameters
                .builder()
                .applicationId(CONFIGURATION_SERVICE_APPLICATION_ID)
                .topicNamePrefixParameters(topicNamePrefixParameters())
                .resourceName(resourceName)
                .build()

        replyTopicService.createOrModifyTopic(
            replyTopicNameParameters,
            ReplyTopicConfiguration.builder().retentionTime(Duration.ofMinutes(10)).build(),
        )

        return requestTemplateFactory.createTemplate(
            replyTopicNameParameters,
            Long::class.javaObjectType,
            replyType,
            Duration.ofSeconds(30),
            ListenerConfiguration
                .stepBuilder()
                // Klienten må ha egen consumer group, ellers havner request- og svar-lytteren i samme
                // gruppe med ulike topics og partisjonene tilordnes feil medlem.
                .groupIdApplicationDefaultWithUniqueSuffix()
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                .continueFromPreviousOffsetOnAssignment()
                .build(),
        )
    }

    /**
     * Speiler `no.novari.flyt.configuration.model.metadata.IntegrationMetadata`. Den mangler `id`,
     * `instanceMetadata` og audit-feltene entiteten sender, så typen fastholder også at klienten
     * tåler felter den ikke kjenner.
     */
    data class ClientIntegrationMetadata(
        val sourceApplicationId: Long? = null,
        val sourceApplicationIntegrationId: String? = null,
        val sourceApplicationIntegrationUri: String? = null,
        val integrationDisplayName: String? = null,
        val version: Long? = null,
    )

    /** Speiler `no.novari.flyt.configuration.model.metadata.InstanceMetadataContent`. */
    data class ClientInstanceMetadataContent(
        val instanceValueMetadata: List<ClientInstanceValueMetadata> = emptyList(),
        val instanceObjectCollectionMetadata: List<Any> = emptyList(),
        val categories: List<Any> = emptyList(),
    )

    /** Speiler `no.novari.flyt.configuration.model.metadata.InstanceValueMetadata`. */
    data class ClientInstanceValueMetadata(
        val displayName: String? = null,
        val key: String? = null,
        val type: Type? = null,
    ) {
        enum class Type {
            STRING,
            BOOLEAN,
            FILE,
        }
    }

    private companion object {
        const val CONFIGURATION_SERVICE_APPLICATION_ID = "fint-flyt-configuration-service"

        fun topicNamePrefixParameters(): TopicNamePrefixParameters =
            TopicNamePrefixParameters
                .stepBuilder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build()

        val METADATA_TOPIC: RequestTopicNameParameters =
            RequestTopicNameParameters
                .builder()
                .topicNamePrefixParameters(topicNamePrefixParameters())
                .resourceName("metadata")
                .parameterName("metadata-id")
                .build()

        val INSTANCE_METADATA_TOPIC: RequestTopicNameParameters =
            RequestTopicNameParameters
                .builder()
                .topicNamePrefixParameters(topicNamePrefixParameters())
                .resourceName("instance-metadata")
                .parameterName("metadata-id")
                .build()
    }
}
