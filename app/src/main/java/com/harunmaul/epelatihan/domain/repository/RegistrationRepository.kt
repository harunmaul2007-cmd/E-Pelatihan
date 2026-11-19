package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.RegistrationDetailSnapshot
import com.harunmaul.epelatihan.domain.model.RegistrationListSnapshot
import kotlinx.coroutines.flow.Flow

interface RegistrationRepository {
    fun observeAllRegistrations(): Flow<AppResult<List<Registration>>> =
        kotlinx.coroutines.flow.flowOf(AppResult.Success(emptyList()))

    fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>>
    fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>>
    suspend fun getRegistration(registrationId: String): AppResult<Registration?>
    suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>>
    suspend fun createRegistration(registration: Registration): AppResult<Registration>
    suspend fun updateRegistrationPrice(
        actorId: String,
        registrationId: String,
        newPricingType: PricingType,
        newPriceAmount: Long,
    ): AppResult<Registration>
    suspend fun updatePaymentStatus(
        actorId: String,
        registrationId: String,
        newStatus: PaymentStatus,
    ): AppResult<Registration>
}
