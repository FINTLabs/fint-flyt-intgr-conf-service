package no.novari.flyt.catalog.discovery.kafka

import no.novari.flyt.catalog.discovery.IntegrationMetadataRepository
import no.novari.flyt.catalog.discovery.IntegrationMetadataService
import no.novari.flyt.catalog.discovery.model.dtos.InstanceMetadataContentDto
import no.novari.flyt.catalog.discovery.model.entities.IntegrationMetadata
import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.requestreply.ReplyProducerRecord
import no.novari.kafka.requestreply.RequestListenerConfiguration
import no.novari.kafka.requestreply.RequestListenerContainerFactory
import no.novari.kafka.requestreply.topic.RequestTopicService
import no.novari.kafka.requestreply.topic.configuration.RequestTopicConfiguration
import no.novari.kafka.requestreply.topic.name.RequestTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import java.time.Duration

@Configuration
class MetadataRequestConsumerConfiguration {
    @Bean
    fun metadataByMetadataIdRequestConsumer(
        requestTopicService: RequestTopicService,
        requestListenerFactoryService: RequestListenerContainerFactory,
        integrationMetadataRepository: IntegrationMetadataRepository,
        errorHandlerFactory: ErrorHandlerFactory,
    ): ConcurrentMessageListenerContainer<String, Long> {
        val requestTopicNameParameters =
            RequestTopicNameParameters
                .builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).resourceName("metadata")
                .parameterName("metadata-id")
                .build()

        requestTopicService.createOrModifyTopic(
            requestTopicNameParameters,
            RequestTopicConfiguration
                .builder()
                .retentionTime(RETENTION_TIME_METADATA_TOPIC)
                .build(),
        )

        return requestListenerFactoryService
            .createRecordConsumerFactory(
                Long::class.java,
                IntegrationMetadata::class.java,
                { consumerRecord: ConsumerRecord<String, Long> ->
                    ReplyProducerRecord
                        .builder<IntegrationMetadata>()
                        .value(integrationMetadataRepository.findById(consumerRecord.value()).orElse(null))
                        .build()
                },
                RequestListenerConfiguration
                    .stepBuilder(Long::class.java)
                    .maxPollRecordsKafkaDefault()
                    .maxPollIntervalKafkaDefault()
                    .build(),
                errorHandlerFactory.createErrorHandler(
                    ErrorHandlerConfiguration
                        .stepBuilder<Long>()
                        .noRetries()
                        .skipFailedRecords()
                        .build(),
                ),
            ).createContainer(requestTopicNameParameters)
    }

    @Bean
    fun instanceMetadataByMetadataIdRequestConsumer(
        requestTopicService: RequestTopicService,
        requestListenerFactoryService: RequestListenerContainerFactory,
        integrationMetadataService: IntegrationMetadataService,
        errorHandlerFactory: ErrorHandlerFactory,
    ): ConcurrentMessageListenerContainer<String, Long> {
        val requestTopicNameParameters =
            RequestTopicNameParameters
                .builder()
                .resourceName("instance-metadata")
                .parameterName("metadata-id")
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).build()

        requestTopicService.createOrModifyTopic(
            requestTopicNameParameters,
            RequestTopicConfiguration
                .builder()
                .retentionTime(RETENTION_TIME_INSTANCE_METADATA_TOPIC)
                .build(),
        )

        return requestListenerFactoryService
            .createRecordConsumerFactory(
                Long::class.java,
                InstanceMetadataContentDto::class.java,
                { consumerRecord: ConsumerRecord<String, Long> ->
                    ReplyProducerRecord
                        .builder<InstanceMetadataContentDto>()
                        .value(integrationMetadataService.getInstanceMetadataById(consumerRecord.value()))
                        .build()
                },
                RequestListenerConfiguration
                    .stepBuilder(Long::class.java)
                    .maxPollRecordsKafkaDefault()
                    .maxPollIntervalKafkaDefault()
                    .build(),
                errorHandlerFactory.createErrorHandler(
                    ErrorHandlerConfiguration
                        .stepBuilder<Long>()
                        .noRetries()
                        .skipFailedRecords()
                        .build(),
                ),
            ).createContainer(requestTopicNameParameters)
    }

    companion object {
        val RETENTION_TIME_METADATA_TOPIC: Duration = Duration.ofMinutes(10)
        val RETENTION_TIME_INSTANCE_METADATA_TOPIC: Duration = Duration.ZERO
    }
}
