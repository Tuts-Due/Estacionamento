package com.arthur.estapar.parking.service

import com.arthur.estapar.parking.model.Revenue
import com.arthur.estapar.parking.repository.ParkingRecordRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class RevenueService(
    private val parkingRecordRepository: ParkingRecordRepository
) {

    /**
     * Calcula a receita total de um setor em uma data.
     * Considera apenas registros com saída registrada naquela data (exitTime between).
     * Veículos ainda estacionados não entram no cálculo.
     */
    fun calculateRevenue(date: LocalDate, sector: String): Revenue {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)

        val records = parkingRecordRepository.findBySectorAndExitTimeBetween(sector, startOfDay, endOfDay)

        val totalAmount = records.sumOf { it.totalAmount ?: 0.0 }

        return Revenue(
            amount = totalAmount,
            currency = "BRL",
            timestamp = OffsetDateTime.now(ZoneOffset.UTC)
        )
    }
}
