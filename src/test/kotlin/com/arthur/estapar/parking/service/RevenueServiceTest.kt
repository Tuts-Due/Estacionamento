package com.arthur.estapar.parking.service

import com.arthur.estapar.parking.model.ParkingRecord
import com.arthur.estapar.parking.repository.ParkingRecordRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

class RevenueServiceTest {

    private val parkingRecordRepository: ParkingRecordRepository = mock()
    private val service = RevenueService(parkingRecordRepository)

    @Test
    fun `deve retornar receita zero quando nenhum veiculo saiu no dia`() {
        whenever(parkingRecordRepository.findBySectorAndExitTimeBetween(eq("A"), any(), any()))
            .thenReturn(emptyList())

        val revenue = service.calculateRevenue(LocalDate.of(2025, 1, 1), "A")

        assertEquals(0.0, revenue.amount)
        assertEquals("BRL", revenue.currency)
    }

    @Test
    fun `deve somar apenas totalAmount de veiculos que ja sairam`() {
        val recordSaiu = ParkingRecord(
            licensePlate = "ABC1111",
            sector = "A",
            entryTime = LocalDateTime.of(2025, 1, 1, 10, 0),
            exitTime = LocalDateTime.of(2025, 1, 1, 12, 0),
            pricePerHour = 10.0,
            totalAmount = 20.0
        )

        whenever(parkingRecordRepository.findBySectorAndExitTimeBetween(eq("A"), any(), any()))
            .thenReturn(listOf(recordSaiu))

        val revenue = service.calculateRevenue(LocalDate.of(2025, 1, 1), "A")

        assertEquals(20.0, revenue.amount)
    }

    @Test
    fun `deve somar receita de multiplos veiculos`() {
        val records = listOf(
            ParkingRecord(licensePlate = "AAA0001", sector = "A",
                entryTime = LocalDateTime.now(), exitTime = LocalDateTime.now(),
                pricePerHour = 10.0, totalAmount = 10.0),
            ParkingRecord(licensePlate = "BBB0002", sector = "A",
                entryTime = LocalDateTime.now(), exitTime = LocalDateTime.now(),
                pricePerHour = 10.0, totalAmount = 20.0),
            ParkingRecord(licensePlate = "CCC0003", sector = "A",
                entryTime = LocalDateTime.now(), exitTime = LocalDateTime.now(),
                pricePerHour = 10.0, totalAmount = 30.0)
        )
        whenever(parkingRecordRepository.findBySectorAndExitTimeBetween(eq("A"), any(), any()))
            .thenReturn(records)

        val revenue = service.calculateRevenue(LocalDate.of(2025, 1, 1), "A")

        assertEquals(60.0, revenue.amount)
    }
}