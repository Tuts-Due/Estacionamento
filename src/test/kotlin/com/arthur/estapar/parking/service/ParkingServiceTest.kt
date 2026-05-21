package com.arthur.estapar.parking.service

import com.arthur.estapar.parking.model.Garage
import com.arthur.estapar.parking.model.ParkingRecord
import com.arthur.estapar.parking.model.Spot
import com.arthur.estapar.parking.model.VehicleEvent
import com.arthur.estapar.parking.repository.GarageRepository
import com.arthur.estapar.parking.repository.ParkingRecordRepository
import com.arthur.estapar.parking.repository.SpotRepository
import com.arthur.estapar.parking.repository.VehicleEventRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset


class ParkingServiceTest {

    private val vehicleEventRepository: VehicleEventRepository = mock()
    private val spotRepository: SpotRepository = mock()
    private val garageRepository: GarageRepository = mock()
    private val parkingRecordRepository: ParkingRecordRepository = mock()

    private lateinit var service: ParkingService

    @BeforeEach
    fun setUp() {
        service = ParkingService(
            vehicleEventRepository,
            spotRepository,
            garageRepository,
            parkingRecordRepository
        )
    }

    // -----------------------------------------------------------------------
    // Testes de calculateFinalAmount
    // -----------------------------------------------------------------------

    @Test
    fun `deve retornar zero para permanencia de ate 30 minutos`() {
        val entry = LocalDateTime.of(2025, 1, 1, 12, 0)
        val exit  = LocalDateTime.of(2025, 1, 1, 12, 30)
        val result = service.calculateFinalAmount(entry, exit, pricePerHour = 10.0)
        assertEquals(0.0, result)
    }

    @Test
    fun `deve cobrar 1 hora para permanencia de 31 minutos`() {
        val entry = LocalDateTime.of(2025, 1, 1, 12, 0)
        val exit  = LocalDateTime.of(2025, 1, 1, 12, 31)
        val result = service.calculateFinalAmount(entry, exit, pricePerHour = 10.0)
        assertEquals(10.0, result)
    }

    @Test
    fun `deve cobrar 2 horas para permanencia de 61 minutos`() {
        val entry = LocalDateTime.of(2025, 1, 1, 12, 0)
        val exit  = LocalDateTime.of(2025, 1, 1, 13, 1)
        val result = service.calculateFinalAmount(entry, exit, pricePerHour = 10.0)
        assertEquals(20.0, result)
    }

    @Test
    fun `deve cobrar exatamente 1 hora para permanencia de 60 minutos`() {
        val entry = LocalDateTime.of(2025, 1, 1, 12, 0)
        val exit  = LocalDateTime.of(2025, 1, 1, 13, 0)
        val result = service.calculateFinalAmount(entry, exit, pricePerHour = 10.0)
        assertEquals(10.0, result)
    }

    // -----------------------------------------------------------------------
    // Testes de calculateDynamicPrice
    // -----------------------------------------------------------------------

    @Test
    fun `deve aplicar desconto de 10 pct com lotacao abaixo de 25 pct`() {
        whenever(spotRepository.countBySector("A")).thenReturn(100L)
        whenever(spotRepository.countBySectorAndIsOccupiedTrue("A")).thenReturn(20L) // 20%
        val price = service.calculateDynamicPrice("A", basePrice = 10.0)
        assertEquals(9.0, price)
    }

    @Test
    fun `deve manter preco base com lotacao entre 25 e 50 pct`() {
        whenever(spotRepository.countBySector("A")).thenReturn(100L)
        whenever(spotRepository.countBySectorAndIsOccupiedTrue("A")).thenReturn(40L) // 40%
        val price = service.calculateDynamicPrice("A", basePrice = 10.0)
        assertEquals(10.0, price)
    }

    @Test
    fun `deve aumentar 10 pct com lotacao entre 50 e 75 pct`() {
        whenever(spotRepository.countBySector("A")).thenReturn(100L)
        whenever(spotRepository.countBySectorAndIsOccupiedTrue("A")).thenReturn(60L) // 60%
        val price = service.calculateDynamicPrice("A", basePrice = 10.0)
        assertEquals(11.0, price)
    }

    @Test
    fun `deve aumentar 25 pct com lotacao acima de 75 pct`() {
        whenever(spotRepository.countBySector("A")).thenReturn(100L)
        whenever(spotRepository.countBySectorAndIsOccupiedTrue("A")).thenReturn(80L) // 80%
        val price = service.calculateDynamicPrice("A", basePrice = 10.0)
        assertEquals(12.5, price)
    }

    // -----------------------------------------------------------------------
    // Testes de processEntry
    // -----------------------------------------------------------------------

    @Test
    fun `deve bloquear entrada quando setor estiver cheio`() {
        val garage = Garage(id = 1L, sector = "A", basePrice = 10.0, maxCapacity = 2)
        whenever(garageRepository.findAll()).thenReturn(listOf(garage))
        whenever(spotRepository.findBySectorAndIsOccupiedFalse("A")).thenReturn(emptyList())
        whenever(parkingRecordRepository.findByLicensePlateAndExitTimeIsNull(any())).thenReturn(null)
        whenever(vehicleEventRepository.save(any())).thenAnswer { it.arguments[0] }

        val event = VehicleEvent(
            licensePlate = "ABC1234",
            eventType = "ENTRY",
            entryTime = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)
        )

        assertThrows<SectorFullException> {
            service.processEntry(event)
        }
    }

    @Test
    fun `deve ignorar entrada duplicada para mesmo veiculo`() {
        val existingRecord = ParkingRecord(
            licensePlate = "ABC1234",
            sector = "A",
            entryTime = LocalDateTime.now(),
            pricePerHour = 10.0
        )
        whenever(vehicleEventRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(parkingRecordRepository.findByLicensePlateAndExitTimeIsNull("ABC1234"))
            .thenReturn(existingRecord)

        val event = VehicleEvent(
            licensePlate = "ABC1234",
            eventType = "ENTRY",
            entryTime = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)
        )

        // Não deve lançar exceção, apenas ignorar
        service.processEntry(event)

        // Não deve tentar salvar novo registro
        verify(parkingRecordRepository, org.mockito.kotlin.never()).save(
            org.mockito.kotlin.argThat<ParkingRecord> { id == 0L }
        )
    }

    // -----------------------------------------------------------------------
    // Testes de processExit
    // -----------------------------------------------------------------------

    @Test
    fun `deve lancar excecao ao processar EXIT sem registro ativo`() {
        whenever(vehicleEventRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(parkingRecordRepository.findByLicensePlateAndExitTimeIsNull("XYZ9999"))
            .thenReturn(null)

        val event = VehicleEvent(
            licensePlate = "XYZ9999",
            eventType = "EXIT",
            exitTime = OffsetDateTime.of(2025, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC)
        )

        assertThrows<RecordNotFoundException> {
            service.processExit(event)
        }
    }

    @Test
    fun `deve calcular valor correto e liberar vaga no EXIT`() {
        val spot = Spot(id = 1L, sector = "A", lat = -23.56, lng = -46.65, isOccupied = true, licensePlate = "ABC1234")
        val entry = LocalDateTime.of(2025, 1, 1, 12, 0)
        val record = ParkingRecord(
            id = 1L,
            licensePlate = "ABC1234",
            sector = "A",
            entryTime = entry,
            spot = spot,
            pricePerHour = 10.0
        )

        whenever(vehicleEventRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(parkingRecordRepository.findByLicensePlateAndExitTimeIsNull("ABC1234")).thenReturn(record)
        whenever(spotRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(parkingRecordRepository.save(any())).thenAnswer { it.arguments[0] }

        val exitEvent = VehicleEvent(
            licensePlate = "ABC1234",
            eventType = "EXIT",
            exitTime = OffsetDateTime.of(2025, 1, 1, 13, 31, 0, 0, ZoneOffset.UTC) // 91 min → 2h
        )

        service.processExit(exitEvent)

        verify(spotRepository).save(
            org.mockito.kotlin.argThat<Spot> { !isOccupied }
        )
        assertNotNull(record.exitTime)
        assertEquals(20.0, record.totalAmount) // 2h × R$10
    }


    // -----------------------------------------------------------------------
    // Testes de processParked
    // -----------------------------------------------------------------------


    @Test
    fun `deve confirmar vaga correta pelo lat lng no PARKED`() {
        val spot = Spot(id = 5L, sector = "A", lat = -23.561684, lng = -46.655981, isOccupied = false)
        val record = ParkingRecord(
            id = 1L, licensePlate = "ZUL0001", sector = "A",
            entryTime = LocalDateTime.now(), pricePerHour = 10.0, spot = spot
        )

        whenever(vehicleEventRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(spotRepository.findByLatLng(-23.561684, -46.655981)).thenReturn(spot)
        whenever(spotRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(parkingRecordRepository.findByLicensePlateAndExitTimeIsNull("ZUL0001")).thenReturn(record)

        val event = VehicleEvent(
            licensePlate = "ZUL0001",
            eventType = "PARKED",
            lat = -23.561684,
            lng = -46.655981
        )

        service.processParked(event)

        verify(spotRepository).save(
            org.mockito.kotlin.argThat<Spot> {
                licensePlate == "ZUL0001" && isOccupied
            }
        )
    }

    @Test
    fun `deve ignorar PARKED quando vaga nao e encontrada pelo lat lng`() {
        whenever(vehicleEventRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(spotRepository.findByLatLng(any(), any())).thenReturn(null)

        val event = VehicleEvent(
            licensePlate = "ZUL0001",
            eventType = "PARKED",
            lat = -99.0,
            lng = -99.0
        )

        // Não deve lançar exceção — apenas logar e retornar
        service.processParked(event)

        verify(spotRepository, never()).save(any())
    }

    @Test
    fun `deve ignorar PARKED sem lat ou lng`() {
        whenever(vehicleEventRepository.save(any())).thenAnswer { it.arguments[0] }

        val event = VehicleEvent(
            licensePlate = "ZUL0001",
            eventType = "PARKED",
            lat = null,
            lng = null
        )

        service.processParked(event)

        verify(spotRepository, never()).findByLatLng(any(), any())
    }

}


