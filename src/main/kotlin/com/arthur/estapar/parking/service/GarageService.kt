package com.arthur.estapar.parking.service

import com.arthur.estapar.parking.model.Garage
import com.arthur.estapar.parking.model.Spot
import com.arthur.estapar.parking.repository.GarageRepository
import com.arthur.estapar.parking.repository.SpotRepository
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import jakarta.annotation.PostConstruct

@Service
class GarageService(
    private val garageRepository: GarageRepository,
    private val spotRepository: SpotRepository,
    private val restTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(GarageService::class.java)

    @Value("\${garage.simulator.url}")
    private lateinit var simulatorUrl: String

    @PostConstruct
    fun init() {
        try {
            fetchAndStoreGarageConfig()
        } catch (ex: RestClientException) {
            log.error("Falha ao buscar configuração da garagem no simulador: ${ex.message}. " +
                    "Verifique se o simulador está rodando em $simulatorUrl")
        }
    }

    fun fetchAndStoreGarageConfig() {
        log.info("Buscando configuração da garagem em $simulatorUrl/garage")
        val garageConfig = restTemplate.getForObject("$simulatorUrl/garage", SimulatorGarageConfig::class.java)
            ?: throw IllegalStateException("Resposta vazia do simulador em $simulatorUrl/garage")

        garageConfig.garage.forEach { simGarage ->

            val existing = garageRepository.findBySector(simGarage.sector)
            if (existing == null) {
                garageRepository.save(
                    Garage(
                        sector = simGarage.sector,
                        basePrice = simGarage.basePrice,
                        maxCapacity = simGarage.maxCapacity
                    )
                )
                log.info("Setor ${simGarage.sector} cadastrado com preço base ${simGarage.basePrice}")
            }
        }

        garageConfig.spots.forEach { simSpot ->
            if (!spotRepository.existsById(simSpot.id)) {
                spotRepository.save(
                    Spot(
                        id = simSpot.id,
                        sector = simSpot.sector,
                        lat = simSpot.lat,
                        lng = simSpot.lng
                    )
                )
            }
        }
        log.info("Configuração da garagem carregada: ${garageConfig.garage.size} setor(es), ${garageConfig.spots.size} vaga(s)")
    }

    data class SimulatorGarageConfig(
        val garage: List<SimulatorGarage>,
        val spots: List<SimulatorSpot>
    )

    data class SimulatorGarage(
        val sector: String,
        val basePrice: Double,
        @JsonProperty("max_capacity")
        val maxCapacity: Int
    )

    data class SimulatorSpot(
        val id: Long,
        val sector: String,
        val lat: Double?,
        val lng: Double?
    )
}
