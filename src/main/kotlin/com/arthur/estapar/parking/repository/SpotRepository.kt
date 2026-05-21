package com.arthur.estapar.parking.repository

import com.arthur.estapar.parking.model.Spot
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SpotRepository : JpaRepository<Spot, Long> {

    /**
     * Busca vagas livres com LOCK PESSIMISTA (SELECT ... FOR UPDATE).
     * Garante que duas transações simultâneas não ocupem a mesma vaga.
     * O @Transactional em ParkingService.processEntry() mantém o lock até o commit.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Spot s WHERE s.sector = :sector AND s.isOccupied = false")
    fun findBySectorAndIsOccupiedFalse(sector: String): List<Spot>

    fun findByLicensePlate(licensePlate: String): Spot?

    fun countBySectorAndIsOccupiedTrue(sector: String): Long

    fun countBySector(sector: String): Long

    // Busca a vaga pelo lat/lng para confirmar setor no evento PARKED
    @Query("SELECT s FROM Spot s WHERE ABS(s.lat - :lat) < 0.0001 AND ABS(s.lng - :lng) < 0.0001")
    fun findByLatLng(lat: Double, lng: Double): Spot?
}
