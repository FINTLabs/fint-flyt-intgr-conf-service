package no.novari.flyt.catalog.configuration.model

import java.util.UUID

data class User(
    var objectIdentifier: UUID? = null,
    var email: String? = null,
    var name: String? = null,
    var sourceApplicationIds: MutableList<Long> = mutableListOf(),
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var objectIdentifier: UUID? = null
        private var email: String? = null
        private var name: String? = null
        private var sourceApplicationIds: MutableList<Long> = mutableListOf()

        fun objectIdentifier(objectIdentifier: UUID?) = apply { this.objectIdentifier = objectIdentifier }

        fun email(email: String?) = apply { this.email = email }

        fun name(name: String?) = apply { this.name = name }

        fun sourceApplicationIds(sourceApplicationIds: List<Long>?) =
            apply { this.sourceApplicationIds = sourceApplicationIds?.toMutableList() ?: mutableListOf() }

        fun build(): User =
            User(
                objectIdentifier = objectIdentifier,
                email = email,
                name = name,
                sourceApplicationIds = sourceApplicationIds,
            )
    }
}
