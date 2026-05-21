package com.arthur.estapar.parking.exception

import com.arthur.estapar.parking.service.RecordNotFoundException
import com.arthur.estapar.parking.service.SectorFullException
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@Schema(description = "Resposta de erro padronizada")
data class ErrorResponse(
    @Schema(description = "Momento do erro", example = "2025-01-01T12:00:00")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Código HTTP", example = "409")
    val status: Int,

    @Schema(description = "Tipo do erro", example = "Sector Full")
    val error: String,

    @Schema(description = "Descrição detalhada", example = "Setor A está com lotação máxima")
    val message: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(SectorFullException::class)
    fun handleSectorFull(ex: SectorFullException): ResponseEntity<ErrorResponse> {
        log.warn("Setor cheio: ${ex.message}")
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(status = HttpStatus.CONFLICT.value(), error = "Sector Full", message = ex.message ?: "Estacionamento lotado")
        )
    }

    @ExceptionHandler(RecordNotFoundException::class)
    fun handleRecordNotFound(ex: RecordNotFoundException): ResponseEntity<ErrorResponse> {
        log.warn("Registro não encontrado: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(status = HttpStatus.NOT_FOUND.value(), error = "Not Found", message = ex.message ?: "Registro não encontrado")
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("Argumento inválido: ${ex.message}")
        return ResponseEntity.badRequest().body(
            ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), error = "Bad Request", message = ex.message ?: "Dados inválidos")
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericError(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Erro inesperado: ${ex.message}", ex)
        return ResponseEntity.internalServerError().body(
            ErrorResponse(status = HttpStatus.INTERNAL_SERVER_ERROR.value(), error = "Internal Server Error", message = "Ocorreu um erro interno. Tente novamente.")
        )
    }
}
