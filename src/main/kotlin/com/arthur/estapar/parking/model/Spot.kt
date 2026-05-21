package com.arthur.estapar.parking.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "spot")
data class Spot(
    // ID vem do simulador, não é gerado pelo banco
    @Id
    val id: Long,

    @Column(nullable = false)
    val sector: String,

    val lat: Double? = null,
    val lng: Double? = null,

    @Column(nullable = false)
    var isOccupied: Boolean = false,

    var licensePlate: String? = null
)
