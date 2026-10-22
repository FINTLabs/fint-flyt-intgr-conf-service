package no.novari.flyt.catalog.configuration.validation.constraints

import no.novari.flyt.catalog.configuration.model.configuration.dtos.ValueMappingDto
import no.novari.flyt.catalog.configuration.model.configuration.entities.ValueMapping
import no.novari.flyt.catalog.configuration.validation.ConfigurationValidationContext
import no.novari.flyt.catalog.configuration.validation.instancereference.InstanceValueKeyExtractionService
import no.novari.flyt.catalog.discovery.model.entities.InstanceValueMetadata
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class InstanceValueKeysAreDefinedInMetadataValidatorTest {
    private lateinit var validator: InstanceValueKeysAreDefinedInMetadataValidator

    @BeforeEach
    fun setUp() {
        validator = InstanceValueKeysAreDefinedInMetadataValidator(InstanceValueKeyExtractionService())
    }

    @Test
    fun `accepts a value without instance field references`() {
        val context =
            ConfigurationValidationContext
                .builder()
                .instanceValueTypePerKey(
                    mapOf(
                        "instanceValueKey1" to InstanceValueMetadata.Type.STRING,
                        "instanceValueKey2" to InstanceValueMetadata.Type.STRING,
                        "instanceValueKey3" to InstanceValueMetadata.Type.STRING,
                    ),
                ).build()
        val hibernateContext: HibernateConstraintValidatorContext = mock()
        whenever(hibernateContext.getConstraintValidatorPayload(ConfigurationValidationContext::class.java))
            .thenReturn(context)

        val valueMappingDto =
            ValueMappingDto
                .builder()
                .type(
                    ValueMapping.Type.DYNAMIC_STRING,
                ).mappingString("Title")
                .build()

        val valid = validator.isValid(valueMappingDto, hibernateContext)

        assertTrue(valid)
    }

    @Test
    fun `accepts a value where every instance field reference key exists in the metadata`() {
        val context =
            ConfigurationValidationContext
                .builder()
                .instanceValueTypePerKey(
                    mapOf(
                        "instanceValueKey1" to InstanceValueMetadata.Type.STRING,
                        "instanceValueKey2" to InstanceValueMetadata.Type.STRING,
                        "instanceValueKey3" to InstanceValueMetadata.Type.STRING,
                    ),
                ).build()
        val hibernateContext: HibernateConstraintValidatorContext = mock()
        whenever(hibernateContext.getConstraintValidatorPayload(ConfigurationValidationContext::class.java))
            .thenReturn(context)

        val valueMappingDto =
            ValueMappingDto
                .builder()
                .type(ValueMapping.Type.DYNAMIC_STRING)
                .mappingString("Title \$if{instanceValueKey1} \$if{instanceValueKey2}")
                .build()

        val valid = validator.isValid(valueMappingDto, hibernateContext)

        assertTrue(valid)
    }

    @Test
    fun `rejects a value with an instance field reference key that is missing from the metadata`() {
        val context =
            ConfigurationValidationContext
                .builder()
                .instanceValueTypePerKey(
                    mapOf(
                        "instanceValueKey1" to InstanceValueMetadata.Type.STRING,
                        "instanceValueKey3" to InstanceValueMetadata.Type.STRING,
                    ),
                ).build()
        val hibernateContext: HibernateConstraintValidatorContext = mock()
        whenever(hibernateContext.getConstraintValidatorPayload(ConfigurationValidationContext::class.java))
            .thenReturn(context)

        val valueMappingDto =
            ValueMappingDto
                .builder()
                .type(ValueMapping.Type.DYNAMIC_STRING)
                .mappingString("Title \$if{instanceValueKey1} \$if{instanceValueKey2}")
                .build()

        val valid = validator.isValid(valueMappingDto, hibernateContext)

        assertFalse(valid)
    }
}
