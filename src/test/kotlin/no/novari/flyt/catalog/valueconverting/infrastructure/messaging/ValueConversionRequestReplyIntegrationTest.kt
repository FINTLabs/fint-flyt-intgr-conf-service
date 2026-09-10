package no.novari.flyt.catalog.valueconverting.infrastructure.messaging

import no.novari.flyt.catalog.CatalogIntegrationTest
import no.novari.flyt.catalog.valueconverting.clearValueConverting
import no.novari.flyt.catalog.valueconverting.domain.ValueConversion
import no.novari.flyt.catalog.valueconverting.infrastructure.persistence.ValueConversionRepository
import no.novari.flyt.catalog.valueconverting.setAuthenticatedUser
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
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Duration
import java.util.UUID
import javax.sql.DataSource

/**
 * Kjører hele request/reply-runden mot ekte Kafka, med en klient bygget slik mapping-service bygger
 * sin — samme topic-parametre, samme svar-topic navngitt etter klienten, samme modellklasse.
 *
 * Akseptansekriteriet er at mapping-service ikke skal måtte bygges eller deployes på nytt. Det er
 * ikke noe man kan lese seg til av serialiseringstestene alene: de dekker payloaden, ikke at
 * serversiden faktisk lytter på det topicet klienten produserer til, og svarer på det klienten
 * lytter på.
 */
class ValueConversionRequestReplyIntegrationTest : CatalogIntegrationTest() {
    @Autowired
    private lateinit var requestTemplateFactory: RequestTemplateFactory

    @Autowired
    private lateinit var replyTopicService: ReplyTopicService

    @Autowired
    private lateinit var valueConversionRepository: ValueConversionRepository

    @Autowired
    @Qualifier("valueConvertingDataSource")
    private lateinit var valueConvertingDataSource: DataSource

    @AfterEach
    fun clearState() {
        SecurityContextHolder.clearContext()
        JdbcTemplate(valueConvertingDataSource).clearValueConverting()
    }

    @Test
    fun `a stored value conversion comes back as the entity mapping-service expects`() {
        setAuthenticatedUser(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        val stored =
            valueConversionRepository.saveAndFlush(
                ValueConversion(
                    displayName = "Display name",
                    fromApplicationId = 1L,
                    fromTypeId = "fromType",
                    toApplicationId = "toAppId",
                    toTypeId = "toType",
                    convertingMap = mutableMapOf("A" to "B"),
                ),
            )

        val reply = mappingServiceClient().requestAndReceive(requestFor(checkNotNull(stored.id))).value()

        assertThat(reply).isNotNull()
        assertThat(reply.fromApplicationId).isEqualTo(1L)
        assertThat(reply.fromTypeId).isEqualTo("fromType")
        assertThat(reply.toApplicationId).isEqualTo("toAppId")
        assertThat(reply.toTypeId).isEqualTo("toType")
        assertThat(reply.convertingMap).containsExactlyInAnyOrderEntriesOf(mapOf("A" to "B"))
        // id er @JsonIgnore på entiteten, og klienten har derfor aldri fått den. Det er kontrakt.
        assertThat(reply.id).isNull()
    }

    @Test
    fun `an unknown id comes back as an empty payload`() {
        val reply = mappingServiceClient().requestAndReceive(requestFor(9999L)).value()

        assertThat(reply).isNull()
    }

    private fun requestFor(valueConversionId: Long) =
        RequestProducerRecord
            .builder<Long>()
            .topicNameParameters(REQUEST_TOPIC_NAME_PARAMETERS)
            .value(valueConversionId)
            .build()

    private fun mappingServiceClient(): RequestTemplate<Long, ClientValueConverting> {
        val replyTopicNameParameters =
            ReplyTopicNameParameters
                .builder()
                .applicationId(MAPPING_SERVICE_APPLICATION_ID)
                .topicNamePrefixParameters(topicNamePrefixParameters())
                .resourceName("value-converting")
                .build()

        replyTopicService.createOrModifyTopic(
            replyTopicNameParameters,
            ReplyTopicConfiguration.builder().retentionTime(Duration.ofMinutes(10)).build(),
        )

        return requestTemplateFactory.createTemplate(
            replyTopicNameParameters,
            Long::class.javaObjectType,
            ClientValueConverting::class.java,
            Duration.ofSeconds(30),
            ListenerConfiguration
                .stepBuilder()
                // Klienten må ha egen consumer group. I drift er den mapping-service sin; her holder
                // det at den er en annen enn serversidens, ellers havner request- og svar-lytteren i
                // samme gruppe med ulike topics og partisjonene tilordnes feil medlem.
                .groupIdApplicationDefaultWithUniqueSuffix()
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                .continueFromPreviousOffsetOnAssignment()
                .build(),
        )
    }

    /**
     * Speiler `no.novari.flyt.mapping.model.valueconverting.ValueConverting`. Den mangler
     * `displayName` og audit-feltene entiteten sender, så typen fastholder også at klienten tåler
     * felter den ikke kjenner.
     */
    data class ClientValueConverting(
        val id: Long? = null,
        val fromApplicationId: Long? = null,
        val fromTypeId: String? = null,
        val toApplicationId: String? = null,
        val toTypeId: String? = null,
        val convertingMap: Map<String, String> = emptyMap(),
    )

    private companion object {
        const val MAPPING_SERVICE_APPLICATION_ID = "fint-flyt-mapping-service"

        fun topicNamePrefixParameters(): TopicNamePrefixParameters =
            TopicNamePrefixParameters
                .stepBuilder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build()

        val REQUEST_TOPIC_NAME_PARAMETERS: RequestTopicNameParameters =
            RequestTopicNameParameters
                .builder()
                .topicNamePrefixParameters(topicNamePrefixParameters())
                .resourceName("value-converting")
                .parameterName("value-converting-id")
                .build()
    }
}
