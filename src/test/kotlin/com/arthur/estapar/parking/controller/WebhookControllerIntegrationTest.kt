package com.arthur.estapar.parking.controller

import com.arthur.estapar.parking.service.ParkingService
import com.arthur.estapar.parking.service.RecordNotFoundException
import com.arthur.estapar.parking.service.SectorFullException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(WebhookController::class)
class WebhookControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var parkingService: ParkingService

    @Test
    fun `deve retornar 200 para evento ENTRY valido`() {
        val payload = """
            {
              "license_plate": "ZUL0001",
              "entry_time": "2025-01-01T12:00:00.000Z",
              "event_type": "ENTRY"
            }
        """.trimIndent()

        mockMvc.post("/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `deve retornar 200 para evento PARKED valido`() {
        val payload = """
            {
              "license_plate": "ZUL0001",
              "lat": -23.561684,
              "lng": -46.655981,
              "event_type": "PARKED"
            }
        """.trimIndent()

        mockMvc.post("/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `deve retornar 200 para evento EXIT valido`() {
        val payload = """
            {
              "license_plate": "ZUL0001",
              "exit_time": "2025-01-01T13:30:00.000Z",
              "event_type": "EXIT"
            }
        """.trimIndent()

        mockMvc.post("/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `deve retornar 400 para tipo de evento desconhecido`() {
        val payload = """
            {
              "license_plate": "ZUL0001",
              "event_type": "UNKNOWN"
            }
        """.trimIndent()

        mockMvc.post("/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `deve retornar 409 quando setor estiver cheio`() {
        doThrow(SectorFullException("Setor A cheio")).whenever(parkingService).processEntry(any())

        val payload = """
            {
              "license_plate": "ZUL0001",
              "entry_time": "2025-01-01T12:00:00.000Z",
              "event_type": "ENTRY"
            }
        """.trimIndent()

        mockMvc.post("/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect { status { isConflict() } }
    }

    @Test
    fun `deve retornar 404 ao sair sem registro de entrada`() {
        doThrow(RecordNotFoundException("Registro não encontrado")).whenever(parkingService).processExit(any())

        val payload = """
            {
              "license_plate": "ZUL9999",
              "exit_time": "2025-01-01T13:00:00.000Z",
              "event_type": "EXIT"
            }
        """.trimIndent()

        mockMvc.post("/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect { status { isNotFound() } }
    }
}
