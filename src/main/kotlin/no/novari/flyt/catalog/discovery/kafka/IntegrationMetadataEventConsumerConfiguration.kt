package no.novari.flyt.catalog.discovery.kafka

import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.configuration.EventCleanupFrequency
import no.novari.kafka.topic.configuration.EventTopicConfiguration
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import java.time.Duration

@Configuration
class IntegrationMetadataEventConsumerConfiguration {
    @Bean
    fun integrationMetadataEventConsumer(
        parameterizedListenerContainerFactoryService: ParameterizedListenerContainerFactoryService,
        integrationMetadataEventHandler: IntegrationMetadataEventHandler,
        eventTopicService: EventTopicService,
        errorHandlerFactory: ErrorHandlerFactory,
    ): ConcurrentMessageListenerContainer<String, IntegrationMetadata> {
        val eventTopicNameParameters = integrationMetadataEventTopicNameParameters()

        eventTopicService.createOrModifyTopic(
            eventTopicNameParameters,
            EventTopicConfiguration
                .stepBuilder()
                .partitions(PARTITIONS)
                .retentionTime(RETENTION_TIME)
                .cleanupFrequency(EventCleanupFrequency.NORMAL)
                .build(),
        )

        return parameterizedListenerContainerFactoryService
            .createRecordListenerContainerFactory(
                IntegrationMetadata::class.java,
                integrationMetadataEventHandler,
                ListenerConfiguration
                    .stepBuilder()
                    .groupIdApplicationDefault()
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
            ).createContainer(eventTopicNameParameters)
    }

    companion object {
        private const val PARTITIONS = 1
        private val RETENTION_TIME: Duration = Duration.ofDays(7)

        fun integrationMetadataEventTopicNameParameters(): EventTopicNameParameters =
            EventTopicNameParameters
                .builder()
                .eventName("integration-metadata-received")
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).build()
    }
}
