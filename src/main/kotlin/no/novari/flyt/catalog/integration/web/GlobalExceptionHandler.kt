package no.novari.flyt.catalog.integration.web

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.catalog.integration.api.IntegrationController
import no.novari.flyt.catalog.validation.ValidationErrorsFormattingService
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice(assignableTypes = [IntegrationController::class])
class GlobalExceptionHandler(
    private val validationErrorsFormattingService: ValidationErrorsFormattingService,
) : ResponseEntityExceptionHandler() {
    private val log = KotlinLogging.logger {}

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): ProblemDetail {
        log.atWarn {
            message = "Handled response status exception with status={} and reason={}"
            arguments = arrayOf(exception.statusCode, exception.reason)
            cause = exception
        }

        return exception.reason
            ?.let { ProblemDetail.forStatusAndDetail(exception.statusCode, it) }
            ?: ProblemDetail.forStatus(exception.statusCode)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val validationErrorMessage =
            validationErrorsFormattingService.format(ex.bindingResult)

        log.atWarn {
            message = "Handled method argument validation failure: {}"
            arguments = arrayOf(validationErrorMessage)
            cause = ex
        }

        return problem(ex, HttpStatus.UNPROCESSABLE_ENTITY, validationErrorMessage, headers, request)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.atWarn {
            message = "Handled malformed request body"
            cause = ex
        }

        return problem(ex, HttpStatus.BAD_REQUEST, "Malformed request body", headers, request)
    }

    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val parameterName = (ex as? MethodArgumentTypeMismatchException)?.name ?: ex.propertyName

        log.atWarn {
            message = "Handled type mismatch for request parameter {}"
            arguments = arrayOf(parameterName)
            cause = ex
        }

        return problem(
            ex,
            HttpStatus.BAD_REQUEST,
            "Invalid value for request parameter '$parameterName'",
            headers,
            request,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ProblemDetail {
        log.atError {
            message = "Handled unexpected exception"
            cause = exception
        }

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
        )
    }

    private fun problem(
        exception: Exception,
        status: HttpStatus,
        detail: String,
        headers: HttpHeaders,
        request: WebRequest,
    ): ResponseEntity<Any>? =
        handleExceptionInternal(
            exception,
            ProblemDetail.forStatusAndDetail(status, detail),
            headers,
            status,
            request,
        )
}
