package com.harunmaul.epelatihan.core.common

sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AuthenticationException(message: String, cause: Throwable? = null) : AppException(message, cause)

class InactiveAccountException(
    message: String = "Akun tidak memiliki profil lembaga yang aktif.",
    cause: Throwable? = null,
) : AppException(message, cause)

class AuthorizationException(
    message: String = "Anda tidak memiliki akses untuk melakukan tindakan ini.",
    cause: Throwable? = null,
) : AppException(message, cause)

class ValidationException(message: String) : AppException(message)

class NetworkException(
    message: String = "Koneksi internet bermasalah. Periksa koneksi Anda.",
    cause: Throwable? = null,
) : AppException(message, cause)

class DuplicateParticipantPhoneException(
    message: String = "Nomor WhatsApp sudah terdaftar untuk peserta lain.",
    cause: Throwable? = null,
) : AppException(message, cause)

class DuplicateRegistrationException(
    message: String = "Peserta sudah terdaftar pada pelatihan ini.",
    cause: Throwable? = null,
) : AppException(message, cause)
