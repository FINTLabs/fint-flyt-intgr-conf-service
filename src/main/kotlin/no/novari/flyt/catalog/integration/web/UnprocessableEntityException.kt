package no.novari.flyt.catalog.integration.web

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class UnprocessableEntityException(
    message: String,
) : ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message)
