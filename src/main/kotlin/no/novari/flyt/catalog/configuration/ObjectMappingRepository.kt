package no.novari.flyt.catalog.configuration

import no.novari.flyt.catalog.configuration.model.configuration.entities.ObjectMapping
import org.springframework.data.jpa.repository.JpaRepository

interface ObjectMappingRepository : JpaRepository<ObjectMapping, Long>
