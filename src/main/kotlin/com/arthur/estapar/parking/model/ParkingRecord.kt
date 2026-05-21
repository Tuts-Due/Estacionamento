package com.arthur.estapar.parking.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "parking_record")
data class ParkingRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val licensePlate: String,

    @Column(nullable = false)
    val sector: String,

    @Column(nullable = false)
    val entryTime: LocalDateTime,

    var exitTime: LocalDateTime? = null,

    @ManyToOne
    @JoinColumn(name = "spot_id")
    var spot: Spot? = null,

    // Preço por hora calculado dinamicamente no momento da entrada
    @Column(nullable = false)
    val pricePerHour: Double,

    // Valor total cobrado — preenchido apenas na saída
    var totalAmount: Double? = null
)
