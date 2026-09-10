package no.novari.flyt.catalog.valueconverting.api.exception

class ValueConversionNotFoundException(
    valueConversionId: Long,
) : RuntimeException("Value conversion with id=$valueConversionId was not found")
