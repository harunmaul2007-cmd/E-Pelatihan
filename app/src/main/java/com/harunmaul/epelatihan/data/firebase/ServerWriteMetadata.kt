package com.harunmaul.epelatihan.data.firebase

import com.google.firebase.firestore.FieldValue

object ServerWriteMetadata {
    fun forCreate(actorId: String): Map<String, Any> = mapOf(
        "createdBy" to actorId,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedBy" to actorId,
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun forUpdate(actorId: String): Map<String, Any> = mapOf(
        "updatedBy" to actorId,
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun timestamp(fieldName: String): Map<String, Any> = mapOf(
        fieldName to FieldValue.serverTimestamp(),
    )
}
