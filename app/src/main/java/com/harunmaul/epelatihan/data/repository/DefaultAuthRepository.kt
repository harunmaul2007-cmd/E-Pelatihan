package com.harunmaul.epelatihan.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthenticationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.core.common.NetworkException
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
) : AuthRepository {

    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeSession(): Flow<UserSession> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user == null) {
                trySend(UserSession.Unauthenticated)
            } else {
                authScope.launch {
                    try {
                        val profile = userRepository.getUserProfile(user.uid)
                        if (profile == null || !profile.active) {
                            firebaseAuth.signOut()
                            trySend(UserSession.Unauthenticated)
                        } else {
                            trySend(UserSession.Authenticated(profile))
                        }
                    } catch (_: Exception) {
                        trySend(UserSession.Unauthenticated)
                    }
                }
            }
        }

        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): AppResult<UserProfile> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user
                ?: return AppResult.Failure(AuthenticationException("Gagal melakukan autentikasi."))

            val profile = userRepository.getUserProfile(user.uid)
            if (profile == null) {
                firebaseAuth.signOut()
                return AppResult.Failure(
                    InactiveAccountException("Akun tidak memiliki profil lembaga. Hubungi Admin.")
                )
            }
            if (!profile.active) {
                firebaseAuth.signOut()
                return AppResult.Failure(
                    InactiveAccountException("Akun Anda telah dinonaktifkan. Hubungi Admin.")
                )
            }

            AppResult.Success(profile)
        } catch (e: FirebaseAuthInvalidUserException) {
            AppResult.Failure(AuthenticationException("Email tidak terdaftar atau telah dinonaktifkan.", e))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AppResult.Failure(AuthenticationException("Email atau kata sandi salah.", e))
        } catch (e: FirebaseNetworkException) {
            AppResult.Failure(NetworkException("Koneksi internet bermasalah. Periksa koneksi Anda.", e))
        } catch (e: InactiveAccountException) {
            AppResult.Failure(e)
        } catch (e: Exception) {
            AppResult.Failure(
                AuthenticationException(e.localizedMessage ?: "Terjadi kesalahan saat masuk.", e)
            )
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            AppResult.Success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            AppResult.Failure(AuthenticationException("Email tidak terdaftar.", e))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AppResult.Failure(AuthenticationException("Format email tidak valid.", e))
        } catch (e: FirebaseNetworkException) {
            AppResult.Failure(NetworkException("Koneksi internet bermasalah. Periksa koneksi Anda.", e))
        } catch (e: Exception) {
            AppResult.Failure(
                AuthenticationException(e.localizedMessage ?: "Gagal mengirim tautan reset kata sandi.", e)
            )
        }
    }

    override suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        return userRepository.getUserProfile(uid)
    }
}
