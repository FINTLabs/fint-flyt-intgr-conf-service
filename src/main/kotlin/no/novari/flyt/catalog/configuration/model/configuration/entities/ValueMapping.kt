package no.novari.flyt.catalog.configuration.model.configuration.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.validation.constraints.NotNull

@Entity
class ValueMapping(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonIgnore
    var id: Long = 0,
    @field:NotNull
    @field:Enumerated(EnumType.STRING)
    var type: Type? = null,
    @field:Column(columnDefinition = "text")
    var mappingString: String? = null,
) {
    enum class Type {
        STRING,
        URL,
        BOOLEAN,
        DYNAMIC_STRING,
        FILE,
        VALUE_CONVERTING,
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var id: Long = 0
        private var type: Type? = null
        private var mappingString: String? = null

        fun id(id: Long) = apply { this.id = id }

        fun type(type: Type?) = apply { this.type = type }

        fun mappingString(mappingString: String?) = apply { this.mappingString = mappingString }

        fun build(): ValueMapping = ValueMapping(id = id, type = type, mappingString = mappingString)
    }
}
