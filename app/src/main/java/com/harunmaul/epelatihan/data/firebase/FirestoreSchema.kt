package com.harunmaul.epelatihan.data.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreCollections {
    const val APP_SETTINGS = "appSettings"
    const val USERS = "users"
    const val TRAININGS = "trainings"
    const val PARTICIPANTS = "participants"
    const val PARTICIPANT_PHONE_INDEX = "participantPhoneIndex"
    const val REGISTRATIONS = "registrations"
    const val WHATSAPP_OPEN_LOGS = "whatsappOpenLogs"
    const val AUDIT_LOGS = "auditLogs"
}

object FirestoreDocuments {
    const val GENERAL_SETTINGS = "general"
}

fun FirebaseFirestore.appSettingsDocument(): DocumentReference =
    collection(FirestoreCollections.APP_SETTINGS)
        .document(FirestoreDocuments.GENERAL_SETTINGS)
