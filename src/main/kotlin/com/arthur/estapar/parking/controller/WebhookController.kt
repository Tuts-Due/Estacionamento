package com.arthur.estapar.parking.controller

import com.arthur.estapar.parking.exception.ErrorResponse
import com.arthur.estapar.parking.model.VehicleEvent
import com.arthur.estapar.parking.service.ParkingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/webhook")
@Tag(name = "Webhook", description = "Recebe eventos do simulador de garagem")
class WebhookController(
    private val parkingService: ParkingService
) {
    private val log = LoggerFactory.getLogger(WebhookController::class.java)

    @Operation(
        summary = "Receber evento de veículo",
        description = """
            Endpoint chamado pelo simulador para notificar eventos de veículos.
            Os três tipos suportados são:
            - **ENTRY**: veículo entra na cancela — ocupa uma vaga e calcula preço dinâmico
            - **PARKED**: veículo estacionou — confirma a vaga pelo lat/lng
            - **EXIT**: veículo saiu — libera a vaga e calcula o valor final
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Evento processado com sucesso"),
        ApiResponse(
            responseCode = "400",
            description = "Tipo de evento desconhecido ou dados inválidos",
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class)
            )]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Registro de entrada não encontrado para o veículo (evento EXIT sem ENTRY)",
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class)
            )]
        ),
        ApiResponse(
            responseCode = "409",
            description = "Setor com lotação máxima — entrada bloqueada",
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class)
            )]
        )
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Payload do evento. O campo `event_type` determina quais outros campos são obrigatórios.",
        required = true,
        content = [Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples = [
                ExampleObject(
                    name = "ENTRY",
                    summary = "Veículo entrando na cancela",
                    value = """{"license_plate": "ZUL0001", "entry_time": "2025-01-01T12:00:00.000Z", "event_type": "ENTRY"}"""
                ),
                ExampleObject(
                    name = "PARKED",
                    summary = "Veículo estacionado — informa posição",
                    value = """{"license_plate": "ZUL0001", "lat": -23.561684, "lng": -46.655981, "event_type": "PARKED"}"""
                ),
                ExampleObject(
                    name = "EXIT",
                    summary = "Veículo saindo — calcula cobrança",
                    value = """{"license_plate": "ZUL0001", "exit_time": "2025-01-01T13:30:00.000Z", "event_type": "EXIT"}"""
                )
            ]
        )]
    )
    @PostMapping
    fun handleWebhookEvent(@RequestBody event: VehicleEvent): ResponseEntity<Void> {
        log.info("Webhook recebido: tipo=${event.eventType}, placa=${event.licensePlate}")
        when (event.eventType) {
            "ENTRY"  -> parkingService.processEntry(event)
            "PARKED" -> parkingService.processParked(event)
            "EXIT"   -> parkingService.processExit(event)
            else     -> {
                log.warn("Tipo de evento desconhecido: ${event.eventType}")
                return ResponseEntity.badRequest().build()
            }
        }
        return ResponseEntity.ok().build()
    }
}
