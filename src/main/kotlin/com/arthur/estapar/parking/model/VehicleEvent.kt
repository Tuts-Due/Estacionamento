package com.arthur.estapar.parking.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.OffsetDateTime

@Entity
data class VehicleEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("event_type")
    val eventType: String,

    // entry_time e exit_time chegam como ISO-8601 com offset (ex: 2025-01-01T12:00:00.000Z)
    @JsonProperty("entry_time")
    val entryTime: OffsetDateTime? = null,

    @JsonProperty("exit_time")
    val exitTime: OffsetDateTime? = null,

    val lat: Double? = null,
    val lng: Double? = null
)
