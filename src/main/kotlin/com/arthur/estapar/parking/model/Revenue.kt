package com.arthur.estapar.parking.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Receita calculada para um setor em uma data")
data class Revenue(
    @Schema(description = "Valor total faturado em reais", example = "120.00")
    val amount: Double,

    @Schema(description = "Moeda", example = "BRL")
    val currency: String = "BRL",

    @Schema(description = "Momento em que a consulta foi gerada (ISO-8601 com offset UTC)", example = "2025-01-01T12:00:00.000Z")
    val timestamp: OffsetDateTime
)
