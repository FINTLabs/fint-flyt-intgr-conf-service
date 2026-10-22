package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.CollectionMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ObjectMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class UniqueChildrenKeysValidatorTest {
    private lateinit var uniqueChildrenKeysValidator: UniqueChildrenKeysValidator

    @BeforeEach
    fun setUp() {
        uniqueChildrenKeysValidator = UniqueChildrenKeysValidator()
    }

    @Test
    fun `accepts children with unique keys`() {
        val objectMappingDto =
            ObjectMappingDto
                .builder()
                .valueMappingPerKey(
                    mapOf(
                        "one" to
                            ValueMappingDto
                                .builder()
                                .mappingString("stringOne")
                                .type(ValueMapping.Type.STRING)
                                .build(),
                        "two" to
                            ValueMappingDto
                                .builder()
                                .mappingString("stringTwo")
                                .type(ValueMapping.Type.URL)
                                .build(),
                    ),
                ).build()

        val isValid = uniqueChildrenKeysValidator.isValid(objectMappingDto, mock<HibernateConstraintValidatorContext>())

        assertThat(isValid).isTrue()
    }

    @Test
    fun `reports duplicate keys within one collection and across child collections`() {
        val valueMappingPerKey = mutableMapOf<String, ValueMappingDto>()
        valueMappingPerKey["one"] = ValueMappingDto.builder().build()
        valueMappingPerKey["two"] = ValueMappingDto.builder().build()
        valueMappingPerKey["six"] = ValueMappingDto.builder().build()

        val valueCollectionMappingPerKey = mutableMapOf<String, CollectionMappingDto<ValueMappingDto>>()
        valueCollectionMappingPerKey["one"] = CollectionMappingDto.builder<ValueMappingDto>().build()
        valueCollectionMappingPerKey["eight"] = CollectionMappingDto.builder<ValueMappingDto>().build()

        val objectMappingPerKey = mutableMapOf<String, ObjectMappingDto>()
        objectMappingPerKey["two"] = ObjectMappingDto.builder().build()
        objectMappingPerKey["three"] = ObjectMappingDto.builder().build()
        objectMappingPerKey["five"] = ObjectMappingDto.builder().build()
        objectMappingPerKey["four"] =
            ObjectMappingDto.builder().valueMappingPerKey(mapOf("three" to ValueMappingDto.builder().build())).build()

        val objectCollectionMappingPerKey = mutableMapOf<String, CollectionMappingDto<ObjectMappingDto>>()
        objectCollectionMappingPerKey["five"] = CollectionMappingDto.builder<ObjectMappingDto>().build()
        objectCollectionMappingPerKey["seven"] = CollectionMappingDto.builder<ObjectMappingDto>().build()

        val objectMappingDto =
            ObjectMappingDto
                .builder()
                .valueMappingPerKey(valueMappingPerKey)
                .valueCollectionMappingPerKey(valueCollectionMappingPerKey)
                .objectMappingPerKey(objectMappingPerKey)
                .objectCollectionMappingPerKey(objectCollectionMappingPerKey)
                .build()

        val duplicateKeys = uniqueChildrenKeysValidator.findDuplicateKeys(objectMappingDto)

        assertEquals(3, duplicateKeys.size)
        assertTrue(duplicateKeys.containsAll(listOf("one", "two", "five")))
    }
}
