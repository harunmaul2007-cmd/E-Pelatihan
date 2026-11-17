package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUsersUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    operator fun invoke(): Flow<List<UserProfile>> = userRepository.observeUsers()
}
