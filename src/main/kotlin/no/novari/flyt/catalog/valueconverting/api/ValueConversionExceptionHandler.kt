package no.novari.flyt.catalog.valueconverting.api

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.catalog.valueconverting.api.exception.InvalidRequestParameterException
import no.novari.flyt.catalog.valueconverting.api.exception.ValueConversionDataIntegrityException
import no.novari.flyt.catalog.valueconverting.api.exception.ValueConversionNotFoundException
import no.novari.flyt.catalog.valueconverting.api.exception.ValueConversionValidationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Avgrenset til value-converting-kontrollerne, fordi hvert katalogdomene har sin egen feilhåndterer
 * med sitt eget feilformat.
 */
@RestControllerAdvice(
    assignableTypes = [
        ValueConversionController::class,
        ValueConversionHistoryController::class,
    ],
)
class ValueConversionExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = KotlinLogging.logger {}

    @ExceptionHandler(ValueConversionNotFoundException::class)
    fun handleValueConversionNotFound(exception: ValueConversionNotFoundException): ProblemDetail {
        log.warn(exception) { "Value conversion not found" }
        return createProblemDetail(
            status = HttpStatus.NOT_FOUND,
            title = "Not Found",
            detail = exception.message ?: "Value conversion not found",
        )
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.warn(ex) { "Malformed request body" }
        return problem(
            exception = ex,
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            title = "Unprocessable Entity",
            detail = createValidationDetail(ex),
            headers = headers,
            request = request,
        )
    }

    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.warn(ex) { "Request parameter validation failed" }
        return problem(
            exception = ex,
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = createMethodValidationDetail(ex),
            headers = headers,
            request = request,
        )
    }

    @ExceptionHandler(InvalidRequestParameterException::class)
    fun handleInvalidRequestParameter(exception: InvalidRequestParameterException): ProblemDetail {
        log.warn(exception) { "Invalid request parameter" }
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = exception.message ?: "Validation error: invalid request parameters",
        )
    }

    override fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.warn(ex) { "Missing request parameter" }
        return problem(
            exception = ex,
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = "Validation error: '${ex.parameterName}' is required",
            headers = headers,
            request = request,
        )
    }

    /**
     * `MethodArgumentNotValidException` er en `BindException`, men basisklassen har en egen handler
     * for den. Den overstyres slik at valideringsfeil får samme svar som [handleBindException] gir.
     */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.warn(ex) { "Request parameter binding failed" }
        return problem(
            exception = ex,
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = createBindingValidationDetail(ex),
            headers = headers,
            request = request,
        )
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(exception: BindException): ProblemDetail {
        log.warn(exception) { "Request parameter binding failed" }
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = createBindingValidationDetail(exception),
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): ProblemDetail {
        val status = HttpStatus.valueOf(exception.statusCode.value())
        log.warn(exception) { "Request rejected with response status" }
        return createProblemDetail(
            status = status,
            title = status.reasonPhrase,
            detail = exception.reason ?: status.reasonPhrase,
        )
    }

    @ExceptionHandler(ValueConversionValidationException::class)
    fun handleValueConversionValidation(exception: ValueConversionValidationException): ProblemDetail {
        log.warn(exception) { "Value conversion validation failed" }
        return createProblemDetail(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            title = "Unprocessable Entity",
            detail = exception.message ?: "Validation error",
        )
    }

    @ExceptionHandler(ValueConversionDataIntegrityException::class)
    fun handleValueConversionDataIntegrity(exception: ValueConversionDataIntegrityException): ProblemDetail {
        log.error(exception) { "Value conversion data integrity violation" }
        return createProblemDetail(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Internal Server Error",
            detail = exception.message ?: "Internal data integrity violation",
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(exception: MethodArgumentTypeMismatchException): ProblemDetail {
        log.warn(exception) { "Request parameter type mismatch" }
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = "Invalid value for request parameter '${exception.name}'",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnhandledException(exception: Exception): ProblemDetail {
        log.error(exception) { "Unhandled exception" }
        return createProblemDetail(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Internal Server Error",
            detail = "Internal server error",
        )
    }

    private fun problem(
        exception: Exception,
        status: HttpStatus,
        title: String,
        detail: String,
        headers: HttpHeaders,
        request: WebRequest,
    ): ResponseEntity<Any>? =
        handleExceptionInternal(
            exception,
            createProblemDetail(status, title, detail),
            headers,
            status,
            request,
        )

    private fun createProblemDetail(
        status: HttpStatus,
        title: String,
        detail: String,
    ): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.title = title
        }
    }

    private fun createValidationDetail(exception: HttpMessageNotReadableException): String {
        val mostSpecificCause = exception.mostSpecificCause
        if (mostSpecificCause is MismatchedInputException) {
            val fieldName = mostSpecificCause.path.lastOrNull()?.fieldName
            if (!fieldName.isNullOrBlank()) {
                return "Validation error: '$fieldName is required'"
            }
        }
        return "Validation error: malformed request body"
    }

    private fun createMethodValidationDetail(exception: HandlerMethodValidationException): String {
        val validationResult = exception.parameterValidationResults.firstOrNull()
        val error = validationResult?.resolvableErrors?.firstOrNull()
        val parameterName = validationResult?.methodParameter?.parameterName
        val defaultMessage = error?.defaultMessage

        return if (!parameterName.isNullOrBlank() && !defaultMessage.isNullOrBlank()) {
            "Validation error: '$parameterName' $defaultMessage"
        } else {
            "Validation error: invalid request parameters"
        }
    }

    private fun createBindingValidationDetail(exception: BindException): String {
        val fieldError = exception.bindingResult.fieldErrors.firstOrNull()
        if (fieldError != null) {
            if (fieldError.isBindingFailure) {
                return "Invalid value for request parameter '${fieldError.field}'"
            }

            val defaultMessage = fieldError.defaultMessage
            return when {
                fieldError.code == "NotNull" -> {
                    "Validation error: '${fieldError.field}' is required"
                }

                fieldError.code == "AssertTrue" && !defaultMessage.isNullOrBlank() -> {
                    "Validation error: $defaultMessage"
                }

                !defaultMessage.isNullOrBlank() -> {
                    "Validation error: '${fieldError.field}' $defaultMessage"
                }

                else -> {
                    "Validation error: invalid request parameters"
                }
            }
        }

        val objectError = exception.bindingResult.globalErrors.firstOrNull()
        val defaultMessage = objectError?.defaultMessage

        return if (!defaultMessage.isNullOrBlank()) {
            "Validation error: $defaultMessage"
        } else {
            "Validation error: invalid request parameters"
        }
    }
}
