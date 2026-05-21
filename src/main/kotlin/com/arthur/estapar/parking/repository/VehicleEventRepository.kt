package com.arthur.estapar.parking.repository

import com.arthur.estapar.parking.model.VehicleEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VehicleEventRepository : JpaRepository<VehicleEvent, Long>
