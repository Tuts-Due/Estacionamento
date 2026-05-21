package com.arthur.estapar.parking.service

import com.arthur.estapar.parking.model.Garage
import com.arthur.estapar.parking.model.Spot
import com.arthur.estapar.parking.repository.GarageRepository
import com.arthur.estapar.parking.repository.SpotRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.web.client.RestTemplate
import kotlin.collections.get

class GarageServiceTest {

    private val garageRepository: GarageRepository = mock()
    private val spotRepository: SpotRepository = mock()
    private val restTemplate: RestTemplate = mock()

    private fun buildService(simulatorUrl: String = "http://localhost:8282"): GarageService {
        val service = GarageService(garageRepository, spotRepository, restTemplate)
        // injeta a URL via reflection (campo @Value não é populado fora do contexto Spring)
        val field = GarageService::class.java.getDeclaredField("simulatorUrl")
        field.isAccessible = true
        field.set(service, simulatorUrl)
        return service
    }

    @Test
    fun `deve salvar setor e vagas quando banco esta vazio`() {
        val simulatorResponse = GarageService.SimulatorGarageConfig(
            garage = listOf(GarageService.SimulatorGarage(sector = "A", basePrice = 10.0, maxCapacity = 5)),
            spots  = listOf(
                GarageService.SimulatorSpot(id = 1L, sector = "A", lat = -23.56, lng = -46.65),
                GarageService.SimulatorSpot(id = 2L, sector = "A", lat = -23.57, lng = -46.66)
            )
        )
        whenever(restTemplate.getForObject("http://localhost:8282/garage", GarageService.SimulatorGarageConfig::class.java))
            .thenReturn(simulatorResponse)
        whenever(garageRepository.findBySector("A")).thenReturn(null)
        whenever(spotRepository.existsById(1L)).thenReturn(false)
        whenever(spotRepository.existsById(2L)).thenReturn(false)
        whenever(garageRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(spotRepository.save(any())).thenAnswer { it.arguments[0] }

        buildService().fetchAndStoreGarageConfig()

        verify(garageRepository).save(any())
        verify(spotRepository, org.mockito.kotlin.times(2)).save(any())
    }

    @Test
    fun `nao deve duplicar setor se ja existir no banco`() {
        val existingGarage = Garage(id = 1L, sector = "A", basePrice = 10.0, maxCapacity = 5)
        val simulatorResponse = GarageService.SimulatorGarageConfig(
            garage = listOf(GarageService.SimulatorGarage(sector = "A", basePrice = 10.0, maxCapacity = 5)),
            spots  = emptyList()
        )
        whenever(restTemplate.getForObject("http://localhost:8282/garage", GarageService.SimulatorGarageConfig::class.java))
            .thenReturn(simulatorResponse)
        whenever(garageRepository.findBySector("A")).thenReturn(existingGarage)

        buildService().fetchAndStoreGarageConfig()

        // Setor já existe → não deve salvar de novo
        verify(garageRepository, never()).save(any())
    }

    @Test
    fun `nao deve duplicar vaga se ja existir no banco`() {
        val simulatorResponse = GarageService.SimulatorGarageConfig(
            garage = listOf(GarageService.SimulatorGarage(sector = "A", basePrice = 10.0, maxCapacity = 5)),
            spots  = listOf(GarageService.SimulatorSpot(id = 1L, sector = "A", lat = -23.56, lng = -46.65))
        )
        whenever(restTemplate.getForObject("http://localhost:8282/garage", GarageService.SimulatorGarageConfig::class.java))
            .thenReturn(simulatorResponse)
        whenever(garageRepository.findBySector("A")).thenReturn(null)
        whenever(garageRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(spotRepository.existsById(1L)).thenReturn(true) // vaga já existe

        buildService().fetchAndStoreGarageConfig()

        // Vaga já existe → não deve salvar de novo
        verify(spotRepository, never()).save(any())
    }

    @Test
    fun `deve propagar excecao quando simulador retorna null`() {
        whenever(restTemplate.getForObject("http://localhost:8282/garage", GarageService.SimulatorGarageConfig::class.java))
            .thenReturn(null)

        assertThrows<IllegalStateException> {
            buildService().fetchAndStoreGarageConfig()
        }
    }

    @Test
    fun `deve carregar multiplos setores`() {
        val simulatorResponse = GarageService.SimulatorGarageConfig(
            garage = listOf(
                GarageService.SimulatorGarage(sector = "A", basePrice = 10.0, maxCapacity = 50),
                GarageService.SimulatorGarage(sector = "B", basePrice = 15.0, maxCapacity = 30)
            ),
            spots = emptyList()
        )
        whenever(restTemplate.getForObject("http://localhost:8282/garage", GarageService.SimulatorGarageConfig::class.java))
            .thenReturn(simulatorResponse)
        whenever(garageRepository.findBySector(any())).thenReturn(null)
        whenever(garageRepository.save(any())).thenAnswer { it.arguments[0] }

        buildService().fetchAndStoreGarageConfig()

        verify(garageRepository, org.mockito.kotlin.times(2)).save(any())
    }
}
