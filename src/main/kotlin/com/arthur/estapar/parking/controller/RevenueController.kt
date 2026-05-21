package com.arthur.estapar.parking.controller

import com.arthur.estapar.parking.exception.ErrorResponse
import com.arthur.estapar.parking.model.Revenue
import com.arthur.estapar.parking.service.RevenueService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

data class RevenueRequest(
    @Schema(description = "Data de consulta no formato YYYY-MM-DD", example = "2025-01-01")
    val date: String,

    @Schema(description = "Identificador do setor da garagem", example = "A")
    val sector: String
)

@RestController
@RequestMapping("/revenue")
@Tag(name = "Revenue", description = "Consulta de faturamento")
class RevenueController(
    private val revenueService: RevenueService
) {

    @Operation(
        summary = "Consultar receita por setor e data",
        description = """
            Retorna o faturamento total de um setor em uma data específica.
            Considera apenas veículos que **já saíram** naquele dia (com `exit_time` preenchido).
            Veículos ainda estacionados não entram no cálculo.
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Receita calculada com sucesso",
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = Revenue::class),
                examples = [ExampleObject(
                    value = """{"amount": 120.00, "currency": "BRL", "timestamp": "2025-01-01T18:00:00"}"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Data ou setor inválidos",
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = Schema(implementation = ErrorResponse::class)
            )]
        )
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Filtro de data e setor",
        required = true,
        content = [Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples = [ExampleObject(
                name = "Consulta setor A em 01/01/2025",
                value = """{"date": "2025-01-01", "sector": "A"}"""
            )]
        )]
    )
    @GetMapping
    fun getRevenue(@RequestBody request: RevenueRequest): ResponseEntity<Revenue> {
        val date = LocalDate.parse(request.date)
        val revenue = revenueService.calculateRevenue(date, request.sector)
        return ResponseEntity.ok(revenue)
    }
}
