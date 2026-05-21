package com.arthur.estapar.parking.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "garage")
data class Garage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val sector: String,

    @Column(nullable = false)
    val basePrice: Double,

    @Column(nullable = false)
    val maxCapacity: Int
)
