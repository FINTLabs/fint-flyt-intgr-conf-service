package no.novari.flyt.catalog.discovery.kafka

import no.novari.kafka.starter.topic.EventCleanupFrequency
import no.novari.kafka.starter.topic.EventTopicConfig
import no.novari.kafka.starter.topic.EventTopicName
import no.novari.kafka.starter.topic.KafkaTopics
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration

@Configuration
class IntegrationMetadataEventConsumerConfiguration {
    @Bean
    fun integrationMetadataReceivedTopic(topics: KafkaTopics): EventTopicName =
        topics.event("integration-metadata-received")

    @Bean
    fun integrationMetadataReceivedTopicCreation(
        topics: KafkaTopics,
        integrationMetadataReceivedTopic: EventTopicName,
    ): NewTopic =
        topics.newTopic(
            integrationMetadataReceivedTopic,
            EventTopicConfig(
                retention = Duration.ofDays(7),
                cleanupFrequency = EventCleanupFrequency.NORMAL,
                partitions = 1,
            ),
        )

    /** No retries: a record that fails is logged and skipped, as before. */
    @Bean
    fun kafkaErrorHandler(): CommonErrorHandler = DefaultErrorHandler(FixedBackOff(0, 0))
}
