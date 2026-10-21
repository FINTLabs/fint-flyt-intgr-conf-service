package no.novari.flyt.catalog.configuration.kafka

import no.novari.flyt.catalog.configuration.ConfigurationService
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ConfigurationDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
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
class ConfigurationRequestConsumerConfiguration {
    @Bean
    fun mappingByConfigurationIdRequestConsumer(
        requestTopicService: RequestTopicService,
        requestListenerContainerFactory: RequestListenerContainerFactory,
        configurationService: ConfigurationService,
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
                ).resourceName("mapping")
                .parameterName("configuration-id")
                .build()

        requestTopicService.createOrModifyTopic(
            requestTopicNameParameters,
            RequestTopicConfiguration.builder().retentionTime(RETENTION_TIME).build(),
        )

        val errorHandlerConfiguration =
            ErrorHandlerConfiguration
                .stepBuilder<ObjectMappingDto>()
                .noRetries()
                .skipFailedRecords()
                .build()

        return requestListenerContainerFactory
            .createRecordConsumerFactory(
                Long::class.java,
                ObjectMappingDto::class.java,
                { consumerRecord: ConsumerRecord<String, Long> ->
                    ReplyProducerRecord
                        .builder<ObjectMappingDto>()
                        .value(configurationService.findById(consumerRecord.value(), false)?.mapping)
                        .build()
                },
                RequestListenerConfiguration
                    .stepBuilder(Long::class.java)
                    .maxPollRecordsKafkaDefault()
                    .maxPollIntervalKafkaDefault()
                    .build(),
                errorHandlerFactory.createErrorHandler(errorHandlerConfiguration),
            ).createContainer(requestTopicNameParameters)
    }

    private companion object {
        private val RETENTION_TIME: Duration = Duration.ofMinutes(10)
    }
}
