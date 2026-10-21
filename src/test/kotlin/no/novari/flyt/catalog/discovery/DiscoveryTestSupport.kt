package no.novari.flyt.catalog.discovery

import org.springframework.jdbc.core.JdbcTemplate

/**
 * Uten `@DataJpaTest` er det ingen transaksjon som rulles tilbake mellom tester, så radene må
 * ryddes eksplisitt.
 */
fun JdbcTemplate.clearDiscovery() {
    execute(
        """
        truncate table integration_metadata,
                       instance_metadata_content_categories,
                       instance_metadata_content_instance_object_collection_metadata,
                       instance_metadata_content_instance_value_metadata,
                       instance_metadata_category,
                       instance_object_collection_metadata,
                       instance_value_metadata,
                       instance_metadata_content
        restart identity cascade
        """.trimIndent(),
    )
}
