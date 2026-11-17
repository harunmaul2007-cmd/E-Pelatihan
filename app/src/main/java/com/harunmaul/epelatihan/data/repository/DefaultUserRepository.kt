package com.harunmaul.epelatihan.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthenticationException
import com.harunmaul.epelatihan.data.firebase.FirebaseReferences
import com.harunmaul.epelatihan.data.firebase.model.UserProfileDocument
import com.harunmaul.epelatihan.data.mapper.toDomain
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class DefaultUserRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val references: FirebaseReferences,
    private val firebaseAuth: FirebaseAuth,
    private val auditLogRepository: AuditLogRepository,
) : UserRepository {

    override suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val snapshot = references.users.document(uid).get().await()
            if (!snapshot.exists()) return null
            val doc = snapshot.toObject(UserProfileDocument::class.java) ?: return null
            doc.id = snapshot.id
            doc.toDomain()
        } catch (_: Exception) {
            null
        }
    }

    override fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = references.users.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val doc = snapshot.toObject(UserProfileDocument::class.java)
                doc?.id = snapshot.id
                trySend(doc?.toDomain())
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    override fun observeUsers(): Flow<List<UserProfile>> = callbackFlow {
        val listener = references.users.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserProfileDocument::class.java)?.apply {
                        id = doc.id
                    }?.toDomain()
                }.sortedBy { it.displayName.lowercase() }
                trySend(users)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun hasAnyAdmin(): Boolean {
        return try {
            val appSettingsSnap = references.appSettings.get().await()
            if (appSettingsSnap.exists() && appSettingsSnap.getBoolean("hasAnyAdmin") == true) {
                return true
            }
            val snapshot = references.users
                .whereEqualTo("role", UserRole.ADMIN.name)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun bootstrapFirstAdmin(
        name: String,
        email: String,
        password: String,
    ): AppResult<UserProfile> {
        return try {
            if (hasAnyAdmin()) {
                return AppResult.Failure(
                    IllegalStateException("Admin pertama sudah terdaftar. Pendaftaran admin dibatasi.")
                )
            }

            val cleanEmail = email.trim()
            val cleanName = name.trim()

            val authResult = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val uid = authResult.user?.uid
                ?: return AppResult.Failure(AuthenticationException("Gagal membuat akun Admin pertama."))

            val payload = mapOf(
                "displayName" to cleanName,
                "email" to cleanEmail,
                "role" to UserRole.ADMIN.name,
                "active" to true,
                "invitationStatus" to InvitationStatus.ACTIVE.name,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            references.users.document(uid).set(payload).await()

            // Catat status keberadaan Admin pada appSettings agar dapat diverifikasi aman sebelum login
            references.appSettings.set(
                mapOf(
                    "hasAnyAdmin" to true,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge()
            ).await()

            auditLogRepository.recordLog(
                actorId = uid,
                action = "BOOTSTRAP_FIRST_ADMIN",
                entityType = "USER",
                entityId = uid,
                before = emptyMap(),
                after = mapOf(
                    "displayName" to cleanName,
                    "email" to cleanEmail,
                    "role" to UserRole.ADMIN.name,
                    "active" to true,
                ),
            )

            val createdProfile = getUserProfile(uid)
                ?: throw IllegalStateException("Profil Admin pertama tidak dapat dimuat.")
            AppResult.Success(createdProfile)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    private fun getSecondaryAuth(): FirebaseAuth {
        val defaultApp = FirebaseApp.getInstance()
        val secondaryAppName = "AdminUserInvitationApp"
        val secondaryApp = FirebaseApp.getApps(context).find { it.name == secondaryAppName }
            ?: FirebaseApp.initializeApp(context, defaultApp.options, secondaryAppName)
        return FirebaseAuth.getInstance(secondaryApp)
    }

    override suspend fun inviteUser(
        actorId: String,
        name: String,
        email: String,
        role: UserRole,
    ): AppResult<UserProfile> {
        return try {
            val cleanEmail = email.trim()
            val cleanName = name.trim()

            // Buat akun baru via instance Auth sekunder agar sesi Admin aktif tidak terganggu
            val secondaryAuth = getSecondaryAuth()
            val tempPassword = UUID.randomUUID().toString() + "!Aa1"

            val newUid: String = try {
                val authResult = secondaryAuth.createUserWithEmailAndPassword(cleanEmail, tempPassword).await()
                authResult.user?.uid ?: throw IllegalStateException("UID pengguna baru tidak ditemukan.")
            } catch (e: FirebaseAuthUserCollisionException) {
                // Email sudah terdaftar di Firebase Auth
                return AppResult.Failure(
                    AuthenticationException("Alamat email sudah terdaftar di sistem autentikasi.", e)
                )
            } finally {
                secondaryAuth.signOut()
            }

            // Kirimkan tautan penetapan password ke email calon pengguna (Admin tidak pernah mengetahui password)
            firebaseAuth.sendPasswordResetEmail(cleanEmail).await()

            // Simpan profil pengguna di Firestore
            val payload = mapOf(
                "displayName" to cleanName,
                "email" to cleanEmail,
                "role" to role.name,
                "active" to true,
                "invitationStatus" to InvitationStatus.INVITED.name,
                "createdBy" to actorId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedBy" to actorId,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            references.users.document(newUid).set(payload).await()

            // Catat audit log
            auditLogRepository.recordLog(
                actorId = actorId,
                action = "INVITE_USER",
                entityType = "USER",
                entityId = newUid,
                before = emptyMap(),
                after = mapOf(
                    "displayName" to cleanName,
                    "email" to cleanEmail,
                    "role" to role.name,
                    "active" to true,
                    "invitationStatus" to InvitationStatus.INVITED.name,
                ),
            )

            val profile = getUserProfile(newUid)
                ?: throw IllegalStateException("Gagal memuat profil pengguna yang baru diundang.")
            AppResult.Success(profile)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun resendInvitation(
        actorId: String,
        userId: String,
        email: String,
    ): AppResult<Unit> {
        return try {
            val cleanEmail = email.trim()
            firebaseAuth.sendPasswordResetEmail(cleanEmail).await()

            auditLogRepository.recordLog(
                actorId = actorId,
                action = "RESEND_INVITATION",
                entityType = "USER",
                entityId = userId,
                before = emptyMap(),
                after = mapOf("email" to cleanEmail),
            )

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun updateUserRole(
        actorId: String,
        userId: String,
        newRole: UserRole,
    ): AppResult<Unit> {
        return try {
            val userRef = references.users.document(userId)
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                return AppResult.Failure(IllegalStateException("Pengguna tidak ditemukan."))
            }

            val oldRole = snapshot.getString("role") ?: UserRole.USER.name

            userRef.update(
                mapOf(
                    "role" to newRole.name,
                    "updatedBy" to actorId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            auditLogRepository.recordLog(
                actorId = actorId,
                action = "UPDATE_USER_ROLE",
                entityType = "USER",
                entityId = userId,
                before = mapOf("role" to oldRole),
                after = mapOf("role" to newRole.name),
            )

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun updateUserStatus(
        actorId: String,
        userId: String,
        active: Boolean,
    ): AppResult<Unit> {
        return try {
            val userRef = references.users.document(userId)
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                return AppResult.Failure(IllegalStateException("Pengguna tidak ditemukan."))
            }

            val oldStatus = snapshot.getBoolean("active") ?: true

            userRef.update(
                mapOf(
                    "active" to active,
                    "updatedBy" to actorId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            auditLogRepository.recordLog(
                actorId = actorId,
                action = "UPDATE_USER_STATUS",
                entityType = "USER",
                entityId = userId,
                before = mapOf("active" to oldStatus),
                after = mapOf("active" to active),
            )

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }
}
