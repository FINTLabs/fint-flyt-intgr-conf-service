package no.novari.flyt.catalog.integration.web

import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.flyt.catalog.integration.api.IntegrationController
import no.novari.flyt.catalog.integration.validation.ValidationErrorsFormattingService
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice(assignableTypes = [IntegrationController::class])
class GlobalExceptionHandler(
    private val validationErrorsFormattingService: ValidationErrorsFormattingService,
) {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): ProblemDetail {
        logger.atWarn {
            message = "Handled response status exception with status={} and reason={}"
            arguments = arrayOf(exception.statusCode, exception.reason)
            cause = exception
        }

        return exception.reason
            ?.let { ProblemDetail.forStatusAndDetail(exception.statusCode, it) }
            ?: ProblemDetail.forStatus(exception.statusCode)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ProblemDetail {
        val validationErrorMessage =
            validationErrorsFormattingService.format(exception.bindingResult)

        logger.atWarn {
            message = "Handled method argument validation failure: {}"
            arguments = arrayOf(validationErrorMessage)
            cause = exception
        }

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,
            validationErrorMessage,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException): ProblemDetail {
        logger.atWarn {
            message = "Handled malformed request body"
            cause = exception
        }

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Malformed request body",
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(exception: MethodArgumentTypeMismatchException): ProblemDetail {
        logger.atWarn {
            message = "Handled type mismatch for request parameter {}"
            arguments = arrayOf(exception.name)
            cause = exception
        }

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid value for request parameter '${exception.name}'",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ProblemDetail {
        logger.atError {
            message = "Handled unexpected exception"
            cause = exception
        }

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
        )
    }
}
