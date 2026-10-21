package no.novari.flyt.catalog.configuration

import org.springframework.jdbc.core.JdbcTemplate

/**
 * Uten `@DataJpaTest` er det ingen transaksjon som rulles tilbake mellom tester, og Envers skriver
 * uansett først ved commit. Radene må derfor ryddes eksplisitt.
 */
fun JdbcTemplate.clearConfiguration() {
    val tables =
        queryForList(
            "select table_name from information_schema.tables where table_schema = current_schema() " +
                "and table_name <> 'flyway_schema_history'",
            String::class.java,
        )
    execute("truncate table ${tables.joinToString()} restart identity cascade")
}
