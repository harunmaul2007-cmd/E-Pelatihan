package com.harunmaul.epelatihan.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseReferences @Inject constructor(
    val firestore: FirebaseFirestore,
) {
    val appSettings: DocumentReference = firestore.appSettingsDocument()
    val users: CollectionReference = firestore.collection(FirestoreCollections.USERS)
    val trainings: CollectionReference = firestore.collection(FirestoreCollections.TRAININGS)
    val participants: CollectionReference = firestore.collection(FirestoreCollections.PARTICIPANTS)
    val participantPhoneIndex: CollectionReference =
        firestore.collection(FirestoreCollections.PARTICIPANT_PHONE_INDEX)
    val registrations: CollectionReference = firestore.collection(FirestoreCollections.REGISTRATIONS)
    val whatsappOpenLogs: CollectionReference =
        firestore.collection(FirestoreCollections.WHATSAPP_OPEN_LOGS)
    val auditLogs: CollectionReference = firestore.collection(FirestoreCollections.AUDIT_LOGS)
}
