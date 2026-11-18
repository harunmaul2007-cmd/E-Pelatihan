package com.harunmaul.epelatihan.feature.report

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.NetworkException
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.RegistrationDetailSnapshot
import com.harunmaul.epelatihan.domain.model.RegistrationListSnapshot
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.usecase.GenerateMonthlyReportUseCase
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyReportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val dummyReportMay = MonthlyReport(
        month = YearMonth.of(2026, 5),
        trainingCount = 2,
        registrationCount = 5,
        paidCount = 3,
        unpaidCount = 2,
        earlyBirdCount = 2,
        normalCount = 3,
        totalBilled = 2_000_000,
        totalIncome = 1_200_000,
        outstanding = 800_000,
        trainings = emptyList(),
        isFromCache = false,
    )

    private val dummyReportJune = MonthlyReport(
        month = YearMonth.of(2026, 6),
        trainingCount = 1,
        registrationCount = 2,
        paidCount = 2,
        unpaidCount = 0,
        earlyBirdCount = 0,
        normalCount = 2,
        totalBilled = 1_000_000,
        totalIncome = 1_000_000,
        outstanding = 0,
        trainings = emptyList(),
        isFromCache = false,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class DummyTrainingRepo : TrainingRepository {
        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
            flowOf(AppResult.Success(TrainingListSnapshot(emptyList(), false)))
        override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
            flowOf(AppResult.Failure(NoSuchElementException()))
        override suspend fun getTraining(trainingId: String): AppResult<Training?> = AppResult.Success(null)
        override suspend fun createTraining(training: Training): AppResult<Training> = AppResult.Success(training)
        override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> = AppResult.Success(training)
        override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class DummyRegistrationRepo : RegistrationRepository {
        override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> =
            flowOf(AppResult.Success(RegistrationListSnapshot(emptyList(), false)))
        override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> =
            flowOf(AppResult.Failure(NoSuchElementException()))
        override suspend fun getRegistration(registrationId: String): AppResult<Registration?> = AppResult.Success(null)
        override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> =
            AppResult.Success(emptyList())
        override suspend fun createRegistration(registration: Registration): AppResult<Registration> =
            AppResult.Success(registration)
        override suspend fun updateRegistrationPrice(
            actorId: String,
            registrationId: String,
            newPricingType: PricingType,
            newPriceAmount: Long,
        ): AppResult<Registration> = error("Not needed")
        override suspend fun updatePaymentStatus(
            actorId: String,
            registrationId: String,
            newStatus: PaymentStatus,
        ): AppResult<Registration> = error("Not needed")
    }

    private class DummyAuthRepo : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(UserSession.Unauthenticated)
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    private class FakeGenerateMonthlyReportUseCase(
        private val reportMap: Map<YearMonth, MonthlyReport> = emptyMap(),
        private val shouldFail: Boolean = false,
    ) : GenerateMonthlyReportUseCase(
        DummyTrainingRepo(),
        DummyRegistrationRepo(),
        DummyAuthRepo(),
    ) {
        var lastRequestedMonth: YearMonth? = null

        override suspend fun invoke(targetMonth: YearMonth): AppResult<MonthlyReport> {
            lastRequestedMonth = targetMonth
            if (shouldFail) {
                return AppResult.Failure(NetworkException("Koneksi gagal"))
            }
            val report = reportMap[targetMonth] ?: MonthlyReport(
                month = targetMonth,
                trainingCount = 0,
                registrationCount = 0,
                paidCount = 0,
                unpaidCount = 0,
                earlyBirdCount = 0,
                normalCount = 0,
                totalBilled = 0,
                totalIncome = 0,
                outstanding = 0,
                trainings = emptyList(),
                isFromCache = false,
            )
            return AppResult.Success(report)
        }
    }

    @Test
    fun `init loads report for current month automatically`() = runTest(testDispatcher) {
        val currentMonth = YearMonth.now()
        val fakeUseCase = FakeGenerateMonthlyReportUseCase(
            mapOf(currentMonth to dummyReportMay.copy(month = currentMonth)),
        )

        val viewModel = MonthlyReportViewModel(fakeUseCase)

        // Initially loading
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.report)
        assertEquals(currentMonth, viewModel.uiState.value.selectedMonth)
        assertEquals(currentMonth, fakeUseCase.lastRequestedMonth)
    }

    @Test
    fun `selectMonth updates selectedMonth and triggers loadReport`() = runTest(testDispatcher) {
        val may = YearMonth.of(2026, 5)
        val june = YearMonth.of(2026, 6)
        val fakeUseCase = FakeGenerateMonthlyReportUseCase(
            mapOf(may to dummyReportMay, june to dummyReportJune),
        )

        val viewModel = MonthlyReportViewModel(fakeUseCase)
        advanceUntilIdle()

        viewModel.selectMonth(may)
        advanceUntilIdle()

        assertEquals(may, viewModel.uiState.value.selectedMonth)
        assertEquals(dummyReportMay, viewModel.uiState.value.report)
        assertEquals(may, fakeUseCase.lastRequestedMonth)

        viewModel.selectMonth(june)
        advanceUntilIdle()

        assertEquals(june, viewModel.uiState.value.selectedMonth)
        assertEquals(dummyReportJune, viewModel.uiState.value.report)
        assertEquals(june, fakeUseCase.lastRequestedMonth)
    }

    @Test
    fun `previousMonth and nextMonth step month by one correctly`() = runTest(testDispatcher) {
        val fakeUseCase = FakeGenerateMonthlyReportUseCase()
        val viewModel = MonthlyReportViewModel(fakeUseCase)
        advanceUntilIdle()

        val baseMonth = YearMonth.of(2026, 5)
        viewModel.selectMonth(baseMonth)
        advanceUntilIdle()

        viewModel.previousMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 4), viewModel.uiState.value.selectedMonth)
        assertEquals(YearMonth.of(2026, 4), fakeUseCase.lastRequestedMonth)

        viewModel.nextMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 5), viewModel.uiState.value.selectedMonth)
        assertEquals(YearMonth.of(2026, 5), fakeUseCase.lastRequestedMonth)
    }

    @Test
    fun `loadReport failure populates errorMessage and stops loading`() = runTest(testDispatcher) {
        val fakeUseCase = FakeGenerateMonthlyReportUseCase(shouldFail = true)
        val viewModel = MonthlyReportViewModel(fakeUseCase)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Koneksi gagal", viewModel.uiState.value.errorMessage)

        viewModel.clearErrorMessage()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
