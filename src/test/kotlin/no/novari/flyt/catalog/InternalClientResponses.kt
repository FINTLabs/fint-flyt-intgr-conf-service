package no.novari.flyt.catalog

import no.novari.flyt.catalog.contract.fixtures.FixtureObjectMapper
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter

/**
 * The whole response bodies of the internal-client read endpoints. They have the same form as the Kafka replies
 * they replaced, and the tests compare strictly, so a missing or renamed field fails.
 */
internal object InternalClientResponses {
    /** Writes like Spring Boot's auto-configured JsonMapper. */
    fun messageConverter() = JacksonJsonHttpMessageConverter(FixtureObjectMapper.springBoot().rebuild())

    fun expected(name: String): String =
        checkNotNull(javaClass.getResource("/internal-client-responses/$name")) { "Missing response file $name" }
            .readText()
}

/** Sets a field that JPA auditing fills in, such as `createdAt`, which has a protected setter. */
internal fun Any.setInheritedField(
    name: String,
    value: Any?,
) {
    val field =
        generateSequence<Class<*>>(javaClass) { it.superclass }
            .firstNotNullOfOrNull { type -> runCatching { type.getDeclaredField(name) }.getOrNull() }
            ?: error("No field '$name' on ${javaClass.name} or its superclasses")
    field.isAccessible = true
    field.set(this, value)
}
