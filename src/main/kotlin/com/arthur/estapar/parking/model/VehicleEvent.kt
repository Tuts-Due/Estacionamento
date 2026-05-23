package com.arthur.estapar.parking.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
data class VehicleEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @JsonProperty("license_plate")
    val licensePlate: String,

    @JsonProperty("event_type")
    val eventType: String,

    @JsonProperty("entry_time")
    val entryTime: LocalDateTime? = null,

    @JsonProperty("exit_time")
    val exitTime: LocalDateTime? = null,

    val lat: Double? = null,
    val lng: Double? = null
)