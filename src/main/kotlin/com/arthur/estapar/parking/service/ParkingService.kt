package com.arthur.estapar.parking.service

import com.arthur.estapar.parking.model.ParkingRecord
import com.arthur.estapar.parking.model.VehicleEvent
import com.arthur.estapar.parking.repository.GarageRepository
import com.arthur.estapar.parking.repository.ParkingRecordRepository
import com.arthur.estapar.parking.repository.SpotRepository
import com.arthur.estapar.parking.repository.VehicleEventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil

@Service
class ParkingService(
    private val vehicleEventRepository: VehicleEventRepository,
    private val spotRepository: SpotRepository,
    private val garageRepository: GarageRepository,
    private val parkingRecordRepository: ParkingRecordRepository
) {
    private val log = LoggerFactory.getLogger(ParkingService::class.java)

    /**
     * Evento ENTRY: registra a entrada, verifica lotação, ocupa uma vaga e calcula o preço dinâmico.
     * O setor será confirmado/atualizado quando chegar o evento PARKED com lat/lng.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun processEntry(event: VehicleEvent) {
        vehicleEventRepository.save(event)

        val entryTime = event.entryTime
            ?: throw IllegalArgumentException("entry_time obrigatório no evento ENTRY")

        // Verifica se o veículo já está estacionado (entrada duplicada)
        val existingRecord = parkingRecordRepository.findByLicensePlateAndExitTimeIsNull(event.licensePlate)
        if (existingRecord != null) {
            log.warn("Veículo ${event.licensePlate} já possui registro de entrada ativo. Ignorando duplicata.")
            return
        }

        val allGarages = garageRepository.findAll()
        if (allGarages.isEmpty()) throw IllegalStateException("Nenhum setor cadastrado")

        // Busca o primeiro setor que ainda tem vaga livre
        val garageWithSpot = allGarages.firstOrNull { garage ->
            spotRepository.findBySectorAndIsOccupiedFalse(garage.sector).isNotEmpty()
        } ?: throw SectorFullException("Todos os setores estão com lotação máxima")

        // findBySectorAndIsOccupiedFalse usa SELECT FOR UPDATE (PESSIMISTIC_WRITE).
        // Enquanto esta transação estiver aberta, nenhuma outra thread consegue ocupar
        // as mesmas vagas, eliminando a race condition em entradas simultâneas.
        val availableSpots = spotRepository.findBySectorAndIsOccupiedFalse(garageWithSpot.sector)
        if (availableSpots.isEmpty()) {
            log.warn("Estacionamento cheio no setor ${garageWithSpot.sector}. Veículo ${event.licensePlate} bloqueado.")
            throw SectorFullException("Setor ${garageWithSpot.sector} está com lotação máxima")
        }

        val spotToOccupy = availableSpots.first()
        spotToOccupy.isOccupied = true
        spotToOccupy.licensePlate = event.licensePlate
        spotRepository.save(spotToOccupy)

        val dynamicPricePerHour = calculateDynamicPrice(garageWithSpot.sector, garageWithSpot.basePrice)

        val parkingRecord = ParkingRecord(
            licensePlate = event.licensePlate,
            sector = garageWithSpot.sector,
            entryTime = entryTime,
            spot = spotToOccupy,
            pricePerHour = dynamicPricePerHour
        )
        parkingRecordRepository.save(parkingRecord)

        log.info("ENTRY | placa=${event.licensePlate} | setor=${garageWithSpot.sector} | vaga=${spotToOccupy.id} | preço/hora=$dynamicPricePerHour")
    }

    /**
     * Evento PARKED: o veículo informou sua posição (lat/lng).
     * Usamos as coordenadas para confirmar a vaga correta e atualizar o setor do registro.
     */
    @Transactional
    fun processParked(event: VehicleEvent) {
        vehicleEventRepository.save(event)

        val lat = event.lat ?: run {
            log.warn("PARKED sem lat/lng para veículo ${event.licensePlate}")
            return
        }
        val lng = event.lng!!

        // Busca lista em vez de único resultado — evita NonUniqueResultException
        // quando múltiplas vagas têm coordenadas idênticas ou muito próximas
        val candidates = spotRepository.findAllByLatLng(lat, lng)
        if (candidates.isEmpty()) {
            log.warn("Vaga não encontrada para lat=$lat, lng=$lng no evento PARKED")
            return
        }

        // Prioridade: vaga já associada à placa > vaga ocupada > primeira da lista
        val spot = candidates.firstOrNull { it.licensePlate == event.licensePlate }
            ?: candidates.firstOrNull { it.isOccupied }
            ?: candidates.first()

        // Atualiza a placa na vaga (garante consistência)
        spot.licensePlate = event.licensePlate
        spot.isOccupied = true
        spotRepository.save(spot)

        // Atualiza o setor no ParkingRecord se necessário
        val record = parkingRecordRepository.findByLicensePlateAndExitTimeIsNull(event.licensePlate)
        if (record != null && record.sector != spot.sector) {
            log.info("PARKED | Corrigindo setor do veículo ${event.licensePlate}: ${record.sector} → ${spot.sector}")
            val correctedRecord = record.copy(sector = spot.sector, spot = spot)
            parkingRecordRepository.delete(record)
            parkingRecordRepository.save(correctedRecord)
        }

        log.info("PARKED | placa=${event.licensePlate} | vaga=${spot.id} | setor=${spot.sector} | lat=$lat, lng=$lng")
    }

    /**
     * Evento EXIT: libera a vaga e calcula o valor final a cobrar.
     */
    @Transactional
    fun processExit(event: VehicleEvent) {
        vehicleEventRepository.save(event)

        val exitTime = event.exitTime
            ?: throw IllegalArgumentException("exit_time obrigatório no evento EXIT")

        val parkingRecord = parkingRecordRepository.findByLicensePlateAndExitTimeIsNull(event.licensePlate)
            ?: throw RecordNotFoundException("Registro ativo não encontrado para ${event.licensePlate}")

        val spot = parkingRecord.spot
            ?: throw IllegalStateException("Vaga não associada ao registro ${parkingRecord.id}")

        spot.isOccupied = false
        spot.licensePlate = null
        spotRepository.save(spot)

        val totalAmount = calculateFinalAmount(parkingRecord.entryTime, exitTime, parkingRecord.pricePerHour)

        parkingRecord.exitTime = exitTime
        parkingRecord.totalAmount = totalAmount
        parkingRecordRepository.save(parkingRecord)

        log.info("EXIT | placa=${event.licensePlate} | setor=${parkingRecord.sector} | duração=${Duration.between(parkingRecord.entryTime, exitTime).toMinutes()} min | valor=R\$$totalAmount")
    }

    // -------------------------------------------------------------------------
    // Regras de negócio
    // -------------------------------------------------------------------------

    fun calculateDynamicPrice(sector: String, basePrice: Double): Double {
        val occupancyPct = calculateOccupancyPercentage(sector)
        return when {
            occupancyPct < 25.0 -> basePrice * 0.90
            occupancyPct < 50.0 -> basePrice * 1.00
            occupancyPct < 75.0 -> basePrice * 1.10
            else                 -> basePrice * 1.25
        }
    }

    fun calculateFinalAmount(entryTime: LocalDateTime, exitTime: LocalDateTime, pricePerHour: Double): Double {
        val minutesParked = Duration.between(entryTime, exitTime).toMinutes()
        if (minutesParked <= 30) return 0.0
        val hours = ceil(minutesParked / 60.0)
        return hours * pricePerHour
    }

    fun calculateOccupancyPercentage(sector: String): Double {
        val total = spotRepository.countBySector(sector)
        if (total == 0L) return 0.0
        val occupied = spotRepository.countBySectorAndIsOccupiedTrue(sector)
        return (occupied.toDouble() / total.toDouble()) * 100.0
    }
}

// Exceções de domínio para retornar HTTP adequado via GlobalExceptionHandler
class SectorFullException(message: String) : RuntimeException(message)
class RecordNotFoundException(message: String) : RuntimeException(message)