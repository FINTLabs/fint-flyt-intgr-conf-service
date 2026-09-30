package no.novari.flyt.catalog.discovery

import org.slf4j.LoggerFactory
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
        requestLogger.warn(
            "Failed to read integration metadata request. request={}, status={}, cause={}",
            request.getDescription(false),
            status.value(),
            ex.mostSpecificCause.message ?: ex.message,
            ex,
        )

        return super.handleHttpMessageNotReadable(ex, headers, status, request)
    }

    companion object {
        private val requestLogger = LoggerFactory.getLogger(IntegrationMetadataRequestExceptionHandler::class.java)
    }
}
