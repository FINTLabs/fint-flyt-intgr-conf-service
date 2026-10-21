package no.novari.flyt.catalog.configuration.kafka

import no.novari.flyt.catalog.configuration.model.integration.Integration
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.requestreply.RequestProducerRecord
import no.novari.kafka.requestreply.RequestTemplate
import no.novari.kafka.requestreply.RequestTemplateFactory
import no.novari.kafka.requestreply.topic.ReplyTopicService
import no.novari.kafka.requestreply.topic.configuration.ReplyTopicConfiguration
import no.novari.kafka.requestreply.topic.name.ReplyTopicNameParameters
import no.novari.kafka.requestreply.topic.name.RequestTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class IntegrationRequestProducerService(
    @Value("\${novari.kafka.application-id}") applicationId: String,
    requestTemplateFactory: RequestTemplateFactory,
    replyTopicService: ReplyTopicService,
) {
    private val requestTopicNameParameters: RequestTopicNameParameters
    private val requestTemplate: RequestTemplate<Long, Integration>

    init {
        val replyTopicNameParameters =
            ReplyTopicNameParameters
                .builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).applicationId(applicationId)
                .resourceName("integration")
                .build()

        replyTopicService.createOrModifyTopic(
            replyTopicNameParameters,
            ReplyTopicConfiguration.builder().retentionTime(RETENTION_TIME).build(),
        )

        requestTopicNameParameters =
            RequestTopicNameParameters
                .builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).resourceName("integration")
                .parameterName("integration-id")
                .build()

        requestTemplate =
            requestTemplateFactory.createTemplate(
                replyTopicNameParameters,
                Long::class.java,
                Integration::class.java,
                REPLY_TIMEOUT,
                ListenerConfiguration
                    .stepBuilder()
                    .groupIdApplicationDefault()
                    .maxPollRecordsKafkaDefault()
                    .maxPollIntervalKafkaDefault()
                    .continueFromPreviousOffsetOnAssignment()
                    .build(),
            )
    }

    fun get(integrationId: Long): Integration? =
        requestTemplate
            .requestAndReceive(
                RequestProducerRecord
                    .builder<Long>()
                    .topicNameParameters(requestTopicNameParameters)
                    .value(integrationId)
                    .build(),
            ).value()

    private companion object {
        private val RETENTION_TIME: Duration = Duration.ofMinutes(10)
        private val REPLY_TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}
