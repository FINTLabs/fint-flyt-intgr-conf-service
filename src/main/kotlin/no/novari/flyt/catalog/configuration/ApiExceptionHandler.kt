package no.novari.flyt.catalog.configuration

import jakarta.servlet.http.HttpServletRequest
import no.novari.flyt.catalog.configuration.validation.CouldNotFindInstanceMetadataException
import no.novari.flyt.catalog.configuration.validation.CouldNotFindIntegrationException
import no.novari.flyt.catalog.configuration.validation.CouldNotFindMetadataException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Instant

@RestControllerAdvice(assignableTypes = [ConfigurationController::class, ConfigurationHistoryController::class])
class ApiExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        exception: ResponseStatusException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        errorResponse(
            exception.statusCode,
            exception.reason ?: exception.message ?: "Request failed",
            request.requestURI,
        )

    @ExceptionHandler(
        CouldNotFindIntegrationException::class,
        CouldNotFindMetadataException::class,
        CouldNotFindInstanceMetadataException::class,
    )
    fun handleConfigurationValidationContextException(
        exception: RuntimeException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        errorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY,
            exception.message ?: "Could not validate configuration",
            request.requestURI,
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        exception: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> =
        errorResponse(
            HttpStatus.BAD_REQUEST,
            exception.message ?: "Invalid request",
            request.requestURI,
        )

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(
        exception: Throwable,
        request: HttpServletRequest,
    ): ResponseEntity<ApiErrorResponse> {
        logger.error("Unhandled exception while handling ${request.requestURI}", exception)
        return errorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            request.requestURI,
        )
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? =
        super.handleExceptionInternal(
            ex,
            errorBody(statusCode, (body as? ProblemDetail)?.detail ?: ex.message, requestUri(request)),
            headers,
            statusCode,
            request,
        )

    private fun errorResponse(
        status: HttpStatusCode,
        message: String,
        path: String,
    ): ResponseEntity<ApiErrorResponse> = ResponseEntity.status(status).body(errorBody(status, message, path))

    private fun errorBody(
        status: HttpStatusCode,
        message: String?,
        path: String,
    ): ApiErrorResponse =
        ApiErrorResponse(
            status = status.value(),
            error = HttpStatus.valueOf(status.value()).reasonPhrase,
            message = message ?: "Request failed",
            path = path,
            timestamp = Instant.now(),
        )

    private fun requestUri(request: WebRequest): String =
        (request as? ServletWebRequest)?.request?.requestURI
            ?: request.getDescription(false).removePrefix("uri=")
}

data class ApiErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant,
)
