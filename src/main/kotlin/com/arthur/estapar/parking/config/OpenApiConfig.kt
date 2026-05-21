package com.arthur.estapar.parking.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Estapar Parking API")
                .version("1.0.0")
                .description(
                    """
                    Sistema de gerenciamento de estacionamento da Estapar.
                    
                    ## Fluxo de eventos
                    O simulador envia eventos via webhook nesta ordem:
                    1. **ENTRY** — veículo entra na cancela
                    2. **PARKED** — veículo estaciona e informa posição (lat/lng)
                    3. **EXIT** — veículo sai e o valor é calculado
                    
                    ## Regras de preço dinâmico
                    | Lotação do setor | Ajuste |
                    |---|---|
                    | < 25% | −10% (desconto) |
                    | 25% a 50% | Preço normal |
                    | 50% a 75% | +10% |
                    | ≥ 75% | +25% |
                    
                    Primeiros **30 minutos são grátis**. Após isso, cobra-se por hora cheia (arredonda pra cima).
                    """.trimIndent()
                )
                .contact(
                    Contact()
                        .name("Estapar Tech")
                        .email("tech@estapar.com.br")
                )
        )
        .tags(
            listOf(
                Tag().name("Webhook").description("Recebe eventos do simulador de garagem (ENTRY, PARKED, EXIT)"),
                Tag().name("Revenue").description("Consulta de faturamento por setor e data")
            )
        )
}
