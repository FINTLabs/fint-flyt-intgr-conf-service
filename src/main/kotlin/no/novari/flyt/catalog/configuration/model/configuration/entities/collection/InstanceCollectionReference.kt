package no.novari.flyt.catalog.configuration.model.configuration.entities.collection

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.validation.constraints.NotBlank

@Entity
class InstanceCollectionReference(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonIgnore
    var id: Long = 0,
    var index: Int = 0,
    @field:NotBlank
    @field:Column(columnDefinition = "text")
    var reference: String? = null,
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var id: Long = 0
        private var index: Int = 0
        private var reference: String? = null

        fun id(id: Long) = apply { this.id = id }

        fun index(index: Int) = apply { this.index = index }

        fun reference(reference: String?) = apply { this.reference = reference }

        fun build(): InstanceCollectionReference =
            InstanceCollectionReference(id = id, index = index, reference = reference)
    }
}
