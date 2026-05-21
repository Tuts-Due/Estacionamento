package com.arthur.estapar.parking.repository

import com.arthur.estapar.parking.model.ParkingRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ParkingRecordRepository : JpaRepository<ParkingRecord, Long> {

    fun findByLicensePlateAndExitTimeIsNull(licensePlate: String): ParkingRecord?

    // Receita: apenas registros com saída registrada (totalAmount preenchido)
    fun findBySectorAndExitTimeBetween(
        sector: String,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<ParkingRecord>
}
