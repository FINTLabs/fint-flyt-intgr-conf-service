package no.novari.flyt.catalog.valueconverting.infrastructure.persistence

import no.novari.flyt.catalog.CatalogIntegrationTest
import no.novari.flyt.catalog.valueconverting.application.ValueConversionFilter
import no.novari.flyt.catalog.valueconverting.clearValueConverting
import no.novari.flyt.catalog.valueconverting.domain.ValueConversion
import no.novari.flyt.catalog.valueconverting.setAuthenticatedUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class ValueConversionRepositoryTest : CatalogIntegrationTest() {
    @Autowired
    private lateinit var repository: ValueConversionRepository

    @Autowired
    @Qualifier("valueConvertingDataSource")
    private lateinit var valueConvertingDataSource: DataSource

    private val jdbcTemplate: JdbcTemplate get() = JdbcTemplate(valueConvertingDataSource)

    @AfterEach
    fun clearState() {
        SecurityContextHolder.clearContext()
        jdbcTemplate.clearValueConverting()
    }

    @Test
    fun `find all should combine filters with authorized source application ids`() {
        val creatorId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val otherCreatorId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val matchingId =
            saveConversion(
                actorId = creatorId,
                displayName = "County text mapping",
                fromApplicationId = 2L,
                fromTypeId = "text",
                toApplicationId = "archive",
                toTypeId = "code",
                createdAt = Instant.parse("2026-01-10T10:00:00Z"),
                lastModifiedAt = Instant.parse("2026-02-10T10:00:00Z"),
            )
        saveConversion(
            actorId = creatorId,
            displayName = "Unauthorized text mapping",
            fromApplicationId = 9L,
            fromTypeId = "text",
            toApplicationId = "archive",
            toTypeId = "code",
            createdAt = Instant.parse("2026-01-10T10:00:00Z"),
            lastModifiedAt = Instant.parse("2026-02-10T10:00:00Z"),
        )
        saveConversion(
            actorId = otherCreatorId,
            displayName = "County number mapping",
            fromApplicationId = 2L,
            fromTypeId = "number",
            toApplicationId = "archive",
            toTypeId = "code",
            createdAt = Instant.parse("2026-01-10T10:00:00Z"),
            lastModifiedAt = Instant.parse("2026-02-10T10:00:00Z"),
        )
        saveConversion(
            actorId = creatorId,
            displayName = "Old text mapping",
            fromApplicationId = 2L,
            fromTypeId = "text",
            toApplicationId = "archive",
            toTypeId = "code",
            createdAt = Instant.parse("2025-12-31T10:00:00Z"),
            lastModifiedAt = Instant.parse("2026-02-10T10:00:00Z"),
        )

        val filter =
            ValueConversionFilter(
                sourceApplicationIds = setOf(2L, 9L),
                fromTypeId = "text",
                toApplicationId = "archive",
                toTypeId = "code",
                displayName = "text",
                createdBy = creatorId,
                createdAtFrom = Instant.parse("2026-01-01T00:00:00Z"),
                createdAtTo = Instant.parse("2026-01-31T23:59:59Z"),
                modifiedBy = creatorId,
                modifiedAtFrom = Instant.parse("2026-02-01T00:00:00Z"),
                modifiedAtTo = Instant.parse("2026-02-28T23:59:59Z"),
            )

        val page =
            repository.findAll(
                ValueConversionSpecifications.matchingFilter(
                    authorizedSourceApplicationIds = setOf(2L, 3L),
                    filter = filter,
                ),
                PageRequest.of(0, 10, Sort.by("displayName")),
            )

        assertThat(page.content.map { it.id }).containsExactly(matchingId)
    }

    @Test
    fun `find all should escape wildcard characters in display name filter`() {
        val matchingId = saveConversion(displayName = "Completion 100% ready", fromApplicationId = 1L)
        saveConversion(displayName = "Completion 1000 ready", fromApplicationId = 1L)

        val page =
            repository.findAll(
                ValueConversionSpecifications.matchingFilter(
                    authorizedSourceApplicationIds = setOf(1L),
                    filter = ValueConversionFilter(displayName = "100%"),
                ),
                PageRequest.of(0, 10, Sort.by("displayName")),
            )

        assertThat(page.content.map { it.id }).containsExactly(matchingId)
    }

    @Test
    fun `find all should keep sorting and pagination when no filters are set`() {
        saveConversion(displayName = "Bravo", fromApplicationId = 1L)
        saveConversion(displayName = "Alpha", fromApplicationId = 1L)
        saveConversion(displayName = "Charlie", fromApplicationId = 1L)
        saveConversion(displayName = "Unauthorized", fromApplicationId = 9L)

        val page =
            repository.findAll(
                ValueConversionSpecifications.matchingFilter(
                    authorizedSourceApplicationIds = setOf(1L),
                    filter = ValueConversionFilter(),
                ),
                PageRequest.of(1, 1, Sort.Direction.DESC, "displayName"),
            )

        assertThat(page.totalElements).isEqualTo(3)
        assertThat(page.content.map { it.displayName }).containsExactly("Bravo")
    }

    @Test
    fun `find all should return empty page when user has no authorized source applications`() {
        saveConversion(displayName = "Alpha", fromApplicationId = 1L)

        val page =
            repository.findAll(
                ValueConversionSpecifications.matchingFilter(
                    authorizedSourceApplicationIds = emptySet(),
                    filter = ValueConversionFilter(sourceApplicationIds = setOf(1L)),
                ),
                PageRequest.of(0, 10, Sort.by("displayName")),
            )

        assertThat(page.totalElements).isZero()
        assertThat(page.content).isEmpty()
    }

    private fun saveConversion(
        actorId: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        displayName: String,
        fromApplicationId: Long,
        fromTypeId: String = "type",
        toApplicationId: String = "target",
        toTypeId: String = "targetType",
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        lastModifiedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ): Long {
        setAuthenticatedUser(actorId)
        val saved =
            repository.saveAndFlush(
                ValueConversion(
                    displayName = displayName,
                    fromApplicationId = fromApplicationId,
                    fromTypeId = fromTypeId,
                    toApplicationId = toApplicationId,
                    toTypeId = toTypeId,
                    convertingMap = mutableMapOf("A" to "B"),
                ),
            )
        val id = checkNotNull(saved.id)
        jdbcTemplate.update(
            """
            update value_converting
            set created_at = ?, last_modified_at = ?
            where id = ?
            """.trimIndent(),
            Timestamp.from(createdAt),
            Timestamp.from(lastModifiedAt),
            id,
        )
        return id
    }
}
