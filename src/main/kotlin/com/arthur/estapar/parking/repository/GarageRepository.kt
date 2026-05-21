package com.arthur.estapar.parking.repository

import com.arthur.estapar.parking.model.Garage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GarageRepository : JpaRepository<Garage, Long> {
    fun findBySector(sector: String): Garage?
}
