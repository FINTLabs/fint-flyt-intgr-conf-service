package no.novari.flyt.catalog.integration.kafka

import no.novari.flyt.catalog.integration.api.dto.IntegrationDto
import no.novari.flyt.catalog.integration.api.dto.SourceApplicationIdAndSourceApplicationIntegrationIdDto
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
class IntegrationRequestConsumerConfiguration {
    @Bean
    fun integrationBySourceApplicationAndIntegrationIdRequestConsumer(
        requestListenerContainerFactory: RequestListenerContainerFactory,
        requestTopicService: RequestTopicService,
        integrationService: IntegrationService,
        errorHandlerFactory: ErrorHandlerFactory,
    ): ConcurrentMessageListenerContainer<String, SourceApplicationIdAndSourceApplicationIntegrationIdDto> {
        val requestTopicNameParameters =
            RequestTopicNameParameters
                .builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).resourceName("integration")
                .parameterName("source-application-id-and-source-application-integration-id")
                .build()

        requestTopicService.createOrModifyTopic(
            requestTopicNameParameters,
            RequestTopicConfiguration.builder().retentionTime(RETENTION_TIME).build(),
        )

        return requestListenerContainerFactory
            .createRecordConsumerFactory(
                SourceApplicationIdAndSourceApplicationIntegrationIdDto::class.java,
                IntegrationDto::class.java,
                { consumerRecord: ConsumerRecord<String, SourceApplicationIdAndSourceApplicationIntegrationIdDto> ->
                    val request = consumerRecord.value()
                    val integration =
                        request.sourceApplicationId?.let { sourceApplicationId ->
                            request.sourceApplicationIntegrationId?.let { sourceApplicationIntegrationId ->
                                integrationService
                                    .findIntegrationBySourceApplicationIdAndSourceApplicationIntegrationId(
                                        sourceApplicationId,
                                        sourceApplicationIntegrationId,
                                    )
                            }
                        }

                    ReplyProducerRecord.builder<IntegrationDto>().value(integration).build()
                },
                RequestListenerConfiguration
                    .stepBuilder(SourceApplicationIdAndSourceApplicationIntegrationIdDto::class.java)
                    .maxPollRecordsKafkaDefault()
                    .maxPollIntervalKafkaDefault()
                    .build(),
                errorHandlerFactory.createErrorHandler(
                    ErrorHandlerConfiguration
                        .stepBuilder<SourceApplicationIdAndSourceApplicationIntegrationIdDto>()
                        .noRetries()
                        .skipFailedRecords()
                        .build(),
                ),
            ).createContainer(requestTopicNameParameters)
    }

    companion object {
        private val RETENTION_TIME = Duration.ofMinutes(5)
    }
}
