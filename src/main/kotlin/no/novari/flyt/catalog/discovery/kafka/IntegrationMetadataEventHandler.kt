package no.novari.flyt.catalog.discovery.kafka

import no.novari.flyt.catalog.discovery.IntegrationMetadataRepository
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.function.Consumer

/**
 * Idempotent lagring av mottatt integrasjonsmetadata. Idempotensen er en forutsetning for at en
 * consumer group kan lese topicen om igjen uten observerbar effekt, og er dekket av en unik
 * constraint på `(source_application_id, source_application_integration_id, version)` i tillegg
 * til sjekken her.
 */
@Component
class IntegrationMetadataEventHandler(
    private val integrationMetadataRepository: IntegrationMetadataRepository,
) : Consumer<ConsumerRecord<String, IntegrationMetadata>> {
    override fun accept(consumerRecord: ConsumerRecord<String, IntegrationMetadata>) {
        val integrationMetadata = consumerRecord.value()
        val sourceApplicationId = requireNotNull(integrationMetadata.sourceApplicationId)
        val sourceApplicationIntegrationId =
            requireNotNull(integrationMetadata.sourceApplicationIntegrationId)
        val version = requireNotNull(integrationMetadata.version)
        val exists =
            integrationMetadataRepository
                .existsBySourceApplicationIdAndSourceApplicationIntegrationIdAndVersion(
                    sourceApplicationId,
                    sourceApplicationIntegrationId,
                    version,
                )

        if (!exists) {
            integrationMetadataRepository.save(integrationMetadata)
        } else {
            logger.warn(
                "Ignored metadata with sourceApplicationId={}, " +
                    "sourceApplicationIntegrationId={} and version={} because it already exists",
                sourceApplicationId,
                sourceApplicationIntegrationId,
                version,
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IntegrationMetadataEventHandler::class.java)
    }
}
