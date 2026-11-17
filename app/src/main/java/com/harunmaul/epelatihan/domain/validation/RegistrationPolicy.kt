package com.harunmaul.epelatihan.domain.validation

import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.Training

sealed interface PriceResolution {
    data class Resolved(
        val pricingType: PricingType,
        val amount: Long,
    ) : PriceResolution

    data class Rejected(
        val issue: ValidationIssue,
    ) : PriceResolution
}

object RegistrationPricingPolicy {
    fun resolve(
        training: Training,
        selectedPricingType: PricingType?,
    ): PriceResolution {
        if (training.normalPrice < 0) {
            return rejected(
                field = ValidationField.NORMAL_PRICE,
                code = ValidationCode.NEGATIVE_NOT_ALLOWED,
                message = "Harga normal tidak boleh negatif",
            )
        }

        val earlyBirdPrice = training.earlyBirdPrice
        if (training.pricingScheme == PricingScheme.EARLY_BIRD_AND_NORMAL) {
            if (earlyBirdPrice == null) {
                return rejected(
                    field = ValidationField.EARLY_BIRD_PRICE,
                    code = ValidationCode.INVALID_STATE,
                    message = "Konfigurasi harga early bird belum lengkap",
                )
            }
            if (earlyBirdPrice < 0 || earlyBirdPrice >= training.normalPrice) {
                return rejected(
                    field = ValidationField.EARLY_BIRD_PRICE,
                    code = ValidationCode.INVALID_STATE,
                    message = "Konfigurasi harga early bird tidak valid",
                )
            }
        }

        return when (training.pricingScheme) {
            PricingScheme.NORMAL_ONLY -> {
                if (earlyBirdPrice != null) {
                    rejected(
                        field = ValidationField.EARLY_BIRD_PRICE,
                        code = ValidationCode.MUST_BE_EMPTY,
                        message = "Harga early bird harus kosong untuk skema harga normal",
                    )
                } else if (selectedPricingType == null || selectedPricingType == PricingType.NORMAL) {
                    PriceResolution.Resolved(PricingType.NORMAL, training.normalPrice)
                } else {
                    rejected(
                        field = ValidationField.PRICING_TYPE,
                        code = ValidationCode.NOT_AVAILABLE,
                        message = "Harga early bird tidak tersedia untuk pelatihan ini",
                    )
                }
            }

            PricingScheme.EARLY_BIRD_AND_NORMAL -> when (selectedPricingType) {
                null -> rejected(
                    field = ValidationField.PRICING_TYPE,
                    code = ValidationCode.REQUIRED,
                    message = "Pilih harga early bird atau harga normal",
                )

                PricingType.NORMAL -> PriceResolution.Resolved(
                    pricingType = PricingType.NORMAL,
                    amount = training.normalPrice,
                )

                PricingType.EARLY_BIRD -> PriceResolution.Resolved(
                    pricingType = PricingType.EARLY_BIRD,
                    amount = checkNotNull(earlyBirdPrice),
                )
            }
        }
    }

    private fun rejected(
        field: ValidationField,
        code: ValidationCode,
        message: String,
    ) = PriceResolution.Rejected(
        ValidationIssue(
            field = field,
            code = code,
            message = message,
        ),
    )
}

enum class RegistrationValidationMode {
    CREATE,
    STORED,
}

object RegistrationValidator {
    fun validate(
        registration: Registration,
        training: Training,
        mode: RegistrationValidationMode = RegistrationValidationMode.STORED,
    ): ValidationResult {
        val issues = buildList {
            if (registration.trainingId.isBlank()) {
                add(required(ValidationField.TRAINING_ID, "Pelatihan wajib dipilih"))
            } else if (registration.trainingId != training.id) {
                add(
                    ValidationIssue(
                        field = ValidationField.TRAINING_ID,
                        code = ValidationCode.VALUE_MISMATCH,
                        message = "Pelatihan pada pendaftaran tidak sesuai",
                    ),
                )
            }
            if (registration.participantId.isBlank()) {
                add(required(ValidationField.PARTICIPANT_ID, "Peserta wajib dipilih"))
            }
            if (registration.id.isBlank()) {
                add(required(ValidationField.REGISTRATION_ID, "ID pendaftaran wajib tersedia"))
            } else if (registration.trainingId.isNotBlank() && registration.participantId.isNotBlank()) {
                val expectedId = RegistrationIdFactory.create(
                    trainingId = registration.trainingId,
                    participantId = registration.participantId,
                )
                if (registration.id != expectedId) {
                    add(
                        ValidationIssue(
                            field = ValidationField.REGISTRATION_ID,
                            code = ValidationCode.VALUE_MISMATCH,
                            message = "ID pendaftaran tidak sesuai dengan pelatihan dan peserta",
                        ),
                    )
                }
            }
            if (registration.createdBy.isBlank() || registration.updatedBy.isBlank()) {
                add(required(ValidationField.ACTOR_ID, "Identitas pengguna wajib tersedia"))
            }
            if (registration.priceAmount < 0) {
                add(
                    ValidationIssue(
                        field = ValidationField.PRICE_AMOUNT,
                        code = ValidationCode.NEGATIVE_NOT_ALLOWED,
                        message = "Harga pendaftaran tidak boleh negatif",
                    ),
                )
            }

            when (val expected = RegistrationPricingPolicy.resolve(training, registration.pricingType)) {
                is PriceResolution.Rejected -> add(expected.issue)
                is PriceResolution.Resolved -> {
                    if (registration.priceAmount != expected.amount) {
                        add(
                            ValidationIssue(
                                field = ValidationField.PRICE_AMOUNT,
                                code = ValidationCode.VALUE_MISMATCH,
                                message = "Harga pendaftaran tidak sesuai dengan master pelatihan",
                            ),
                        )
                    }
                }
            }

            if (mode == RegistrationValidationMode.CREATE &&
                registration.paymentStatus != PaymentStatus.BELUM_BAYAR
            ) {
                add(
                    ValidationIssue(
                        field = ValidationField.PAYMENT_STATUS,
                        code = ValidationCode.INVALID_STATE,
                        message = "Pendaftaran baru harus berstatus belum bayar",
                    ),
                )
            }

            when (registration.paymentStatus) {
                PaymentStatus.BELUM_BAYAR -> {
                    if (registration.paidAt != null) {
                        add(mustBeEmpty(ValidationField.PAID_AT, "Tanggal pembayaran"))
                    }
                    if (registration.paidBy != null) {
                        add(mustBeEmpty(ValidationField.PAID_BY, "Petugas pembayaran"))
                    }
                }

                PaymentStatus.LUNAS -> {
                    if (registration.paidAt == null) {
                        add(required(ValidationField.PAID_AT, "Tanggal pembayaran wajib tersedia"))
                    }
                    if (registration.paidBy.isNullOrBlank()) {
                        add(required(ValidationField.PAID_BY, "Petugas pembayaran wajib tersedia"))
                    }
                }
            }
        }

        return issues.asValidationResult()
    }

    private fun required(field: ValidationField, message: String) = ValidationIssue(
        field = field,
        code = ValidationCode.REQUIRED,
        message = message,
    )

    private fun mustBeEmpty(field: ValidationField, label: String) = ValidationIssue(
        field = field,
        code = ValidationCode.MUST_BE_EMPTY,
        message = "$label harus kosong ketika peserta belum bayar",
    )
}

sealed interface RegistrationCreationResult {
    data class Created(val registration: Registration) : RegistrationCreationResult
    data class Rejected(val issues: List<ValidationIssue>) : RegistrationCreationResult
}

object RegistrationFactory {
    fun create(
        training: Training,
        participantId: String,
        selectedPricingType: PricingType?,
        actorId: String,
    ): RegistrationCreationResult {
        val preliminaryIssues = buildList {
            if (training.id.isBlank()) {
                add(
                    ValidationIssue(
                        field = ValidationField.TRAINING_ID,
                        code = ValidationCode.REQUIRED,
                        message = "Pelatihan wajib tersedia sebelum pendaftaran dibuat",
                    ),
                )
            }
            if (participantId.isBlank()) {
                add(
                    ValidationIssue(
                        field = ValidationField.PARTICIPANT_ID,
                        code = ValidationCode.REQUIRED,
                        message = "Peserta wajib dipilih",
                    ),
                )
            }
            if (actorId.isBlank()) {
                add(
                    ValidationIssue(
                        field = ValidationField.ACTOR_ID,
                        code = ValidationCode.REQUIRED,
                        message = "Identitas pengguna wajib tersedia",
                    ),
                )
            }
        }
        if (preliminaryIssues.isNotEmpty()) {
            return RegistrationCreationResult.Rejected(preliminaryIssues)
        }

        return when (val price = RegistrationPricingPolicy.resolve(training, selectedPricingType)) {
            is PriceResolution.Rejected -> RegistrationCreationResult.Rejected(listOf(price.issue))
            is PriceResolution.Resolved -> RegistrationCreationResult.Created(
                Registration(
                    id = RegistrationIdFactory.create(training.id, participantId),
                    trainingId = training.id,
                    participantId = participantId,
                    pricingType = price.pricingType,
                    priceAmount = price.amount,
                    paymentStatus = PaymentStatus.BELUM_BAYAR,
                    paidAt = null,
                    paidBy = null,
                    createdBy = actorId,
                    createdAt = null,
                    updatedBy = actorId,
                    updatedAt = null,
                ),
            )
        }
    }
}
