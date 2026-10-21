package no.novari.flyt.catalog.configuration.validation.groups

import jakarta.validation.GroupSequence

@GroupSequence(value = [ValueParsability::class, InstanceValueKeys::class, InstanceValueTypes::class])
interface Completed
