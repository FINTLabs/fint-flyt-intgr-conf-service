package no.novari.flyt.catalog.discovery.kafka

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.novari.flyt.catalog.CatalogIntegrationTest
import no.novari.flyt.catalog.discovery.IntegrationMetadataRepository
import no.novari.flyt.catalog.discovery.clearDiscovery
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService
import no.novari.kafka.producing.ParameterizedProducerRecord
import no.novari.kafka.producing.ParameterizedTemplateFactory
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

/**
 * Speilet fra discovery-service, med rollene byttet: der testen tidligere bygde katalogtjenestens
 * consumer for hånd, er det nå den gamle tjenestens som bygges for hånd, mens katalogtjenestens er
 * applikasjonens egen bønne.
 *
 * Testen fastholder at katalogtjenesten kan overta `event.integration-metadata-received` uten
 * datatap og uten duplikater, og at en tilbakerulling til discovery-service fungerer selv om
 * katalogtjenesten har skrevet rader i mellomtiden.
 *
 * Den nye applikasjonen får ny consumer group uten commitede offsets, og leser derfor topicen fra
 * begynnelsen. Det er tilsiktet: det er slik meldinger produsert i gapet mellom at den gamle
 * tjenesten stoppes og den nye starter, blir plukket opp. Prisen er at allerede behandlede
 * meldinger leses om igjen, og det er den prisen denne testen viser at vi kan betale.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntegrationMetadataEventIdempotencyIntegrationTest : CatalogIntegrationTest() {
    @Autowired
    private lateinit var integrationMetadataRepository: IntegrationMetadataRepository

    @Autowired
    private lateinit var integrationMetadataEventHandler: IntegrationMetadataEventHandler

    @Autowired
    private lateinit var parameterizedListenerContainerFactoryService: ParameterizedListenerContainerFactoryService

    @Autowired
    private lateinit var parameterizedTemplateFactory: ParameterizedTemplateFactory

    @Autowired
    private lateinit var errorHandlerFactory: ErrorHandlerFactory

    @Autowired
    @Qualifier("integrationMetadataEventConsumer")
    private lateinit var catalogServiceConsumer: ConcurrentMessageListenerContainer<String, IntegrationMetadata>

    @Autowired
    @Qualifier("discoveryDataSource")
    private lateinit var discoveryDataSource: DataSource

    private lateinit var skippedEventAppender: ListAppender<ILoggingEvent>

    private var discoveryServiceConsumer: ConcurrentMessageListenerContainer<String, IntegrationMetadata>? = null

    @BeforeEach
    fun setUp() {
        catalogServiceConsumer.stop()
        discoveryServiceConsumer = null
        JdbcTemplate(discoveryDataSource).clearDiscovery()

        skippedEventAppender =
            ListAppender<ILoggingEvent>().apply { start() }
        handlerLogger().addAppender(skippedEventAppender)
    }

    @AfterEach
    fun tearDown() {
        discoveryServiceConsumer?.stop()
        catalogServiceConsumer.stop()
        handlerLogger().detachAppender(skippedEventAppender)
    }

    @Test
    fun `catalog service takes over without loss and rollback to discovery service reprocesses without duplicates`() {
        val discoveryServiceConsumer = createAndRegisterDiscoveryConsumer()
        discoveryServiceConsumer.start()
        publish(version = 1)
        publish(version = 2)
        awaitStoredVersions(1, 2)

        discoveryServiceConsumer.stop()

        publish(version = 3)
        publish(version = 4)

        catalogServiceConsumer.start()

        awaitStoredVersions(1, 2, 3, 4)
        assertThat(skippedVersions()).containsExactlyInAnyOrder(1L, 2L)

        catalogServiceConsumer.stop()

        publish(version = 5)
        publish(version = 6)

        skippedEventAppender.list.clear()
        discoveryServiceConsumer.start()

        awaitStoredVersions(1, 2, 3, 4, 5, 6)
        assertThat(skippedVersions()).containsExactlyInAnyOrder(3L, 4L)
    }

    /**
     * Containeren opprettes utenfor Spring-konteksten, så ingenting stopper den automatisk.
     * Den registreres derfor for opprydding i [tearDown]: feiler testen mellom start og stop,
     * ville en kjørende consumer ellers blitt stående og forstyrret påfølgende tester.
     */
    private fun createAndRegisterDiscoveryConsumer(): ConcurrentMessageListenerContainer<String, IntegrationMetadata> =
        parameterizedListenerContainerFactoryService
            .createRecordListenerContainerFactory(
                IntegrationMetadata::class.java,
                integrationMetadataEventHandler,
                ListenerConfiguration
                    .stepBuilder()
                    .groupIdApplicationDefaultWithSuffix(DISCOVERY_SERVICE_GROUP_ID_SUFFIX)
                    .maxPollRecordsKafkaDefault()
                    .maxPollIntervalKafkaDefault()
                    .continueFromPreviousOffsetOnAssignment()
                    .build(),
                errorHandlerFactory.createErrorHandler(
                    ErrorHandlerConfiguration
                        .stepBuilder<IntegrationMetadata>()
                        .noRetries()
                        .skipFailedRecords()
                        .build(),
                ),
            ).createContainer(
                IntegrationMetadataEventConsumerConfiguration.integrationMetadataEventTopicNameParameters(),
            ).also { discoveryServiceConsumer = it }

    private fun publish(version: Long) {
        parameterizedTemplateFactory
            .createTemplate(IntegrationMetadata::class.java)
            .send(
                ParameterizedProducerRecord
                    .builder<IntegrationMetadata>()
                    .topicNameParameters(
                        IntegrationMetadataEventConsumerConfiguration
                            .integrationMetadataEventTopicNameParameters(),
                    ).key("$SOURCE_APPLICATION_INTEGRATION_ID-$version")
                    .value(
                        IntegrationMetadata(
                            sourceApplicationId = SOURCE_APPLICATION_ID,
                            sourceApplicationIntegrationId = SOURCE_APPLICATION_INTEGRATION_ID,
                            integrationDisplayName = "Integrasjon $version",
                            version = version,
                        ),
                    ).build(),
            ).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
    }

    private fun awaitStoredVersions(vararg expectedVersions: Long) {
        await()
            .atMost(TIMEOUT)
            .untilAsserted {
                assertThat(storedVersions()).containsExactlyInAnyOrder(*expectedVersions.toTypedArray())
            }
    }

    private fun storedVersions(): List<Long> =
        integrationMetadataRepository
            .findAll()
            .mapNotNull { it.version }

    private fun skippedVersions(): List<Long> =
        skippedEventAppender.list
            .filter { it.level == Level.WARN }
            .mapNotNull { event ->
                event.argumentArray
                    ?.lastOrNull()
                    ?.let { (it as? Long) }
            }

    private fun handlerLogger() =
        LoggerFactory.getLogger(IntegrationMetadataEventHandler::class.java) as ch.qos.logback.classic.Logger

    companion object {
        private const val DISCOVERY_SERVICE_GROUP_ID_SUFFIX = "-discovery-service"
        private const val SOURCE_APPLICATION_ID = 1L
        private const val SOURCE_APPLICATION_INTEGRATION_ID = "TEST-1"
        private val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
