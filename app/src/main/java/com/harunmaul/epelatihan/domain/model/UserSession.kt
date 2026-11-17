package com.harunmaul.epelatihan.domain.model

sealed interface UserSession {
    data object Loading : UserSession
    data object Unauthenticated : UserSession
    data class Authenticated(val profile: UserProfile) : UserSession
}
