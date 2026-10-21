package no.novari.flyt.catalog.discovery

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice(assignableTypes = [IntegrationMetadataController::class])
@Order(Ordered.HIGHEST_PRECEDENCE)
class IntegrationMetadataRequestExceptionHandler : ResponseEntityExceptionHandler() {
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        requestLog.atWarn {
            message = "Failed to read integration metadata request. request={}, status={}, cause={}"
            arguments =
                arrayOf(
                    request.getDescription(false),
                    status.value(),
                    ex.mostSpecificCause.message ?: ex.message,
                )
            cause = ex
        }

        return super.handleHttpMessageNotReadable(ex, headers, status, request)
    }

    companion object {
        private val requestLog = KotlinLogging.logger {}
    }
}
