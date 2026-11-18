package com.harunmaul.epelatihan.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.DuplicateRegistrationException
import com.harunmaul.epelatihan.data.firebase.FirebaseReferences
import com.harunmaul.epelatihan.data.firebase.model.RegistrationDocument
import com.harunmaul.epelatihan.data.mapper.toDomain
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.RegistrationDetailSnapshot
import com.harunmaul.epelatihan.domain.model.RegistrationListSnapshot
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class DefaultRegistrationRepository @Inject constructor(
    private val references: FirebaseReferences,
    private val auditLogRepository: AuditLogRepository,
) : RegistrationRepository {

    override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> = callbackFlow {
        val query = references.registrations
            .whereEqualTo("trainingId", trainingId)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppResult.Failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val registrations = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(RegistrationDocument::class.java)?.apply {
                            id = doc.id
                        }?.toDomain()
                    } catch (_: Exception) {
                        null
                    }
                }.sortedWith(compareBy { it.createdAt ?: java.time.Instant.MIN })
                val isFromCache = snapshot.metadata.isFromCache
                trySend(AppResult.Success(RegistrationListSnapshot(registrations, isFromCache)))
            } else {
                trySend(AppResult.Success(RegistrationListSnapshot(emptyList(), false)))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> = callbackFlow {
        val listener = references.registrations.document(registrationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val registration = try {
                        snapshot.toObject(RegistrationDocument::class.java)?.apply {
                            id = snapshot.id
                        }?.toDomain()
                    } catch (_: Exception) {
                        null
                    }
                    val isFromCache = snapshot.metadata.isFromCache
                    trySend(AppResult.Success(RegistrationDetailSnapshot(registration, isFromCache)))
                } else {
                    trySend(AppResult.Success(RegistrationDetailSnapshot(null, false)))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getRegistration(registrationId: String): AppResult<Registration?> {
        return try {
            val snapshot = references.registrations.document(registrationId).get().await()
            if (!snapshot.exists()) {
                AppResult.Success(null)
            } else {
                val registration = snapshot.toObject(RegistrationDocument::class.java)?.apply {
                    id = snapshot.id
                }?.toDomain()
                AppResult.Success(registration)
            }
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> {
        return try {
            val snapshot = references.registrations
                .whereEqualTo("trainingId", trainingId)
                .get()
                .await()
            val registrations = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(RegistrationDocument::class.java)?.apply {
                        id = doc.id
                    }?.toDomain()
                } catch (_: Exception) {
                    null
                }
            }
            AppResult.Success(registrations)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun createRegistration(registration: Registration): AppResult<Registration> {
        return try {
            val docRef = references.registrations.document(registration.id)

            references.firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    throw DuplicateRegistrationException("Peserta sudah terdaftar pada pelatihan ini.")
                }

                val payload = mapOf(
                    "trainingId" to registration.trainingId,
                    "participantId" to registration.participantId,
                    "pricingType" to registration.pricingType.name,
                    "priceAmount" to registration.priceAmount,
                    "paymentStatus" to PaymentStatus.BELUM_BAYAR.name,
                    "paidAt" to null,
                    "paidBy" to null,
                    "createdBy" to registration.createdBy,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to registration.createdBy,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                transaction.set(docRef, payload)
            }.await()

            auditLogRepository.recordLog(
                actorId = registration.createdBy,
                action = "CREATE_REGISTRATION",
                entityType = "REGISTRATION",
                entityId = registration.id,
                before = emptyMap(),
                after = mapOf(
                    "trainingId" to registration.trainingId,
                    "participantId" to registration.participantId,
                    "pricingType" to registration.pricingType.name,
                    "priceAmount" to registration.priceAmount,
                ),
            )

            AppResult.Success(registration)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun updateRegistrationPrice(
        actorId: String,
        registrationId: String,
        newPricingType: PricingType,
        newPriceAmount: Long,
    ): AppResult<Registration> {
        return try {
            val docRef = references.registrations.document(registrationId)
            var updatedRegistration: Registration? = null

            references.firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw NoSuchElementException("Pendaftaran tidak ditemukan.")
                }

                val currentDoc = snapshot.toObject(RegistrationDocument::class.java)?.apply {
                    id = snapshot.id
                } ?: throw IllegalStateException("Format data pendaftaran tidak valid.")

                val oldPricingType = currentDoc.pricingType
                val oldPriceAmount = currentDoc.priceAmount

                val payload = mapOf(
                    "pricingType" to newPricingType.name,
                    "priceAmount" to newPriceAmount,
                    "updatedBy" to actorId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                transaction.update(docRef, payload)

                val updatedDoc = currentDoc.copy(
                    pricingType = newPricingType.name,
                    priceAmount = newPriceAmount,
                    updatedBy = actorId,
                )
                updatedRegistration = updatedDoc.toDomain()
            }.await()

            auditLogRepository.recordLog(
                actorId = actorId,
                action = "UPDATE_REGISTRATION_PRICE",
                entityType = "REGISTRATION",
                entityId = registrationId,
                before = mapOf(
                    "pricingType" to updatedRegistration?.pricingType?.name,
                ),
                after = mapOf(
                    "pricingType" to newPricingType.name,
                    "priceAmount" to newPriceAmount,
                ),
            )

            AppResult.Success(checkNotNull(updatedRegistration))
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun updatePaymentStatus(
        actorId: String,
        registrationId: String,
        newStatus: PaymentStatus,
    ): AppResult<Registration> {
        return try {
            val docRef = references.registrations.document(registrationId)
            var updatedRegistration: Registration? = null
            var oldStatus: String? = null
            var oldPaidAt: String? = null
            var oldPaidBy: String? = null

            references.firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw NoSuchElementException("Pendaftaran tidak ditemukan.")
                }

                val currentDoc = snapshot.toObject(RegistrationDocument::class.java)?.apply {
                    id = snapshot.id
                } ?: throw IllegalStateException("Format data pendaftaran tidak valid.")

                oldStatus = currentDoc.paymentStatus
                oldPaidAt = currentDoc.paidAt?.toDate()?.toInstant()?.toString()
                oldPaidBy = currentDoc.paidBy

                val payload = mutableMapOf<String, Any?>(
                    "paymentStatus" to newStatus.name,
                    "updatedBy" to actorId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                if (newStatus == PaymentStatus.LUNAS) {
                    payload["paidAt"] = FieldValue.serverTimestamp()
                    payload["paidBy"] = actorId
                } else {
                    payload["paidAt"] = null
                    payload["paidBy"] = null
                }

                transaction.update(docRef, payload)

                val updatedDoc = currentDoc.copy(
                    paymentStatus = newStatus.name,
                    paidBy = if (newStatus == PaymentStatus.LUNAS) actorId else null,
                    updatedBy = actorId,
                )
                updatedRegistration = updatedDoc.toDomain()
            }.await()

            auditLogRepository.recordLog(
                actorId = actorId,
                action = "UPDATE_PAYMENT_STATUS",
                entityType = "REGISTRATION",
                entityId = registrationId,
                before = mapOf(
                    "paymentStatus" to oldStatus,
                    "paidAt" to oldPaidAt,
                    "paidBy" to oldPaidBy,
                ),
                after = mapOf(
                    "paymentStatus" to newStatus.name,
                    "paidAt" to if (newStatus == PaymentStatus.LUNAS) "serverTimestamp" else null,
                    "paidBy" to if (newStatus == PaymentStatus.LUNAS) actorId else null,
                ),
            )

            AppResult.Success(checkNotNull(updatedRegistration))
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }
}
