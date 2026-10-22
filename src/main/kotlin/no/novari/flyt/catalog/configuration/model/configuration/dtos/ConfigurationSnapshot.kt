package no.novari.flyt.catalog.configuration.model.configuration.dtos

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Tilstanden til en konfigurasjon slik den var i en gitt revisjon, brukt som `snapshot` i
 * historikk-API-et. Uten mapping: mapping-treet er ikke auditert, så innholdet finnes ikke i
 * revisjonen. Uten audit-sidecar-felt (`createdBy`/`lastModifiedBy` m.fl.): de er `@NotAudited`
 * og dermed alltid tomme i en rekonstruert revisjon, og hvem/når per revisjon eksponeres
 * allerede på historikk-rad-nivå (`actor`/`actorDisplay`/`timestamp`).
 */
@Schema(description = "Configuration state recorded for an audit revision.")
data class ConfigurationSnapshot(
    val id: Long?,
    val integrationId: Long?,
    val integrationMetadataId: Long?,
    val version: Int?,
    val completed: Boolean,
    val comment: String?,
)
