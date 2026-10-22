package no.novari.flyt.catalog.integration.kafka

import no.novari.flyt.catalog.integration.application.IntegrationService
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
class ActiveConfigurationIdRequestConsumerConfiguration {
    @Bean
    fun activeConfigurationIdRequestConsumer(
        requestTopicService: RequestTopicService,
        integrationService: IntegrationService,
        requestListenerContainerFactory: RequestListenerContainerFactory,
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
                ).resourceName("active-configuration-id")
                .parameterName("integration-id")
                .build()

        requestTopicService.createOrModifyTopic(
            requestTopicNameParameters,
            RequestTopicConfiguration.builder().retentionTime(RETENTION_TIME).build(),
        )

        return requestListenerContainerFactory
            .createRecordConsumerFactory(
                Long::class.java,
                Long::class.java,
                { consumerRecord: ConsumerRecord<String, Long> ->
                    ReplyProducerRecord
                        .builder<Long>()
                        .value(
                            integrationService.findActiveConfigurationIdByIntegrationId(consumerRecord.value()),
                        ).build()
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
        private val RETENTION_TIME = Duration.ofMinutes(10)
    }
}
