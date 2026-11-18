package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildWhatsAppMessageUseCaseTest {

    private val useCase = BuildWhatsAppMessageUseCase()

    private val participant = Participant(
        id = "p-1",
        name = "Budi Hartono",
        nameSort = "budi hartono",
        phoneDisplay = "0812-3456-7890",
        phoneNormalized = "6281234567890",
        email = "budi@example.com",
        notes = "Instansi ABC",
        active = true,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val onlineTraining = Training(
        id = "t-1",
        name = "Pelatihan Android Jetpack Compose",
        description = "Deskripsi pelatihan",
        startAt = Instant.parse("2026-05-10T02:00:00Z"), // 09:00 WIB
        endAt = Instant.parse("2026-05-10T05:00:00Z"), // 12:00 WIB
        timeZoneId = "Asia/Jakarta",
        speaker = "Dr. Harun",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = "https://zoom.us/j/999888",
        location = null,
        recordingUrl = "https://drive.google.com/recording",
        materialUrl = "https://drive.google.com/material.pdf",
        pricingScheme = PricingScheme.NORMAL_ONLY,
        normalPrice = 150000,
        earlyBirdPrice = null,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val offlineTraining = onlineTraining.copy(
        name = "Workshop Flutter Offline",
        meetingMethod = MeetingMethod.OFFLINE,
        zoomUrl = null,
        location = "Gedung Serbaguna Lt. 2, Jakarta",
        recordingUrl = null,
        materialUrl = null,
    )

    @Test
    fun `build message for online training contains all required info and links`() {
        val message = useCase(
            participant = participant,
            training = onlineTraining,
            communityName = "Komunitas Android Indonesia",
        )

        assertTrue(message.contains("Halo Budi Hartono"))
        assertTrue(message.contains("Pelatihan Android Jetpack Compose"))
        assertTrue(message.contains("sudah tercatat lunas"))
        assertTrue(message.contains("Dr. Harun"))
        assertTrue(message.contains("Online (Zoom)"))
        assertTrue(message.contains("Link Zoom: https://zoom.us/j/999888"))
        assertTrue(message.contains("Link Rekaman: https://drive.google.com/recording"))
        assertTrue(message.contains("Link Materi: https://drive.google.com/material.pdf"))
        assertTrue(message.contains("Komunitas Android Indonesia"))
    }

    @Test
    fun `build message for offline training contains location and does not contain zoom link`() {
        val message = useCase(
            participant = participant,
            training = offlineTraining,
            communityName = "Komunitas Android Indonesia",
        )

        assertTrue(message.contains("Halo Budi Hartono"))
        assertTrue(message.contains("Workshop Flutter Offline"))
        assertTrue(message.contains("Offline (Tatap Muka)"))
        assertTrue(message.contains("Lokasi: Gedung Serbaguna Lt. 2, Jakarta"))
        assertTrue(!message.contains("Link Zoom"))
    }
}
