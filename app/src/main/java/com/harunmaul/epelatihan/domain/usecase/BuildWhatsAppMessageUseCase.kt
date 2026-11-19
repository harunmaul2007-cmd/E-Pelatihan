package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.WhatsAppMessageType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class BuildWhatsAppMessageUseCase @Inject constructor() {
    operator fun invoke(
        participant: Participant,
        training: Training,
        communityName: String,
        type: WhatsAppMessageType = WhatsAppMessageType.ZOOM_INFO,
    ): String {
        return when (type) {
            WhatsAppMessageType.ZOOM_INFO -> buildZoomAndScheduleMessage(
                participant = participant,
                training = training,
                communityName = communityName,
            )
            WhatsAppMessageType.RECORDING_AND_MATERIAL -> buildRecordingAndMaterialMessage(
                participant = participant,
                training = training,
                communityName = communityName,
            )
        }
    }

    private fun buildZoomAndScheduleMessage(
        participant: Participant,
        training: Training,
        communityName: String,
    ): String {
        val zoneId = try {
            ZoneId.of(training.timeZoneId)
        } catch (_: Exception) {
            ZoneId.of("Asia/Jakarta")
        }

        val idLocale = Locale.forLanguageTag("id-ID")
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", idLocale)
            .withZone(zoneId)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", idLocale)
            .withZone(zoneId)

        val dateStr = dateFormatter.format(training.startAt)
        val startTimeStr = timeFormatter.format(training.startAt)
        val endTimeStr = timeFormatter.format(training.endAt)

        val methodStr = when (training.meetingMethod) {
            MeetingMethod.ONLINE -> "Online (Zoom)"
            MeetingMethod.OFFLINE -> "Offline (Tatap Muka)"
        }

        val builder = StringBuilder()
        builder.append("Halo ${participant.name},\n\n")
        builder.append("Pembayaran Anda untuk ${training.name} sudah tercatat lunas.\n\n")
        builder.append("Tanggal: $dateStr\n")
        builder.append("Waktu: $startTimeStr–$endTimeStr ${training.timeZoneId}\n")
        builder.append("Pemateri: ${training.speaker}\n")
        builder.append("Metode: $methodStr\n")

        if (training.meetingMethod == MeetingMethod.ONLINE && !training.zoomUrl.isNullOrBlank()) {
            builder.append("Link Zoom: ${training.zoomUrl}\n")
        } else if (training.meetingMethod == MeetingMethod.OFFLINE && !training.location.isNullOrBlank()) {
            builder.append("Lokasi: ${training.location}\n")
        }

        builder.append("\nSalam,\n")
        builder.append(communityName.ifBlank { "E-Pelatihan" })

        return builder.toString()
    }

    private fun buildRecordingAndMaterialMessage(
        participant: Participant,
        training: Training,
        communityName: String,
    ): String {
        val recUrl = training.recordingUrl?.takeIf { it.isNotBlank() } ?: "-"
        val matUrl = training.materialUrl?.takeIf { it.isNotBlank() } ?: "-"

        val builder = StringBuilder()
        builder.append("Halo ${participant.name},\n\n")
        builder.append("Berikut link rekaman dan Materi Pelatihan ${training.name}\n")
        builder.append("Link Rekaman : $recUrl\n")
        builder.append("Link Materi : $matUrl\n\n")
        builder.append("Salam,\n")
        builder.append(communityName.ifBlank { "E-Pelatihan" })

        return builder.toString()
    }
}
