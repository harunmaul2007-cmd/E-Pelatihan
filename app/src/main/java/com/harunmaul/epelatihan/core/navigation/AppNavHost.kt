package com.harunmaul.epelatihan.core.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.feature.auth.bootstrap.BootstrapAdminRoute
import com.harunmaul.epelatihan.feature.auth.forgotpassword.ForgotPasswordRoute
import com.harunmaul.epelatihan.feature.auth.login.LoginRoute
import com.harunmaul.epelatihan.feature.home.HomeRoute
import com.harunmaul.epelatihan.feature.participant.detail.ParticipantDetailRoute
import com.harunmaul.epelatihan.feature.participant.form.ParticipantFormRoute
import com.harunmaul.epelatihan.feature.participant.list.ParticipantListRoute
import com.harunmaul.epelatihan.feature.training.detail.TrainingDetailRoute
import com.harunmaul.epelatihan.feature.training.form.TrainingFormRoute
import com.harunmaul.epelatihan.feature.training.list.TrainingListRoute
import com.harunmaul.epelatihan.feature.usermanagement.UserListRoute
import kotlinx.serialization.Serializable

@Serializable
data object SplashDestination

@Serializable
data object LoginDestination

@Serializable
data object ForgotPasswordDestination

@Serializable
data object BootstrapAdminDestination

@Serializable
data object HomeDestination

@Serializable
data object UserListDestination

@Serializable
data object TrainingListDestination

@Serializable
data class TrainingDetailDestination(val trainingId: String)

@Serializable
data class TrainingFormDestination(val trainingId: String? = null)

@Serializable
data object ParticipantListDestination

@Serializable
data class ParticipantDetailDestination(val participantId: String)

@Serializable
data class ParticipantFormDestination(val participantId: String? = null, val initialPhone: String? = null)

@Composable
fun AppNavHost(
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is UserSession.Authenticated -> {
                navController.navigate(HomeDestination) {
                    popUpTo(0) { inclusive = true }
                }
            }
            UserSession.Unauthenticated -> {
                navController.navigate(LoginDestination) {
                    popUpTo(0) { inclusive = true }
                }
            }
            UserSession.Loading -> {
                // Tetap di SplashDestination
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = SplashDestination,
    ) {
        composable<SplashDestination> {
            SplashScreen()
        }

        composable<LoginDestination> {
            LoginRoute(
                onNavigateToForgotPassword = {
                    navController.navigate(ForgotPasswordDestination)
                },
                onNavigateToBootstrapAdmin = {
                    navController.navigate(BootstrapAdminDestination)
                },
            )
        }

        composable<ForgotPasswordDestination> {
            ForgotPasswordRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable<BootstrapAdminDestination> {
            BootstrapAdminRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable<HomeDestination> {
            HomeRoute(
                onNavigateToUserManagement = {
                    navController.navigate(UserListDestination)
                },
                onNavigateToTrainings = {
                    navController.navigate(TrainingListDestination)
                },
                onNavigateToParticipants = {
                    navController.navigate(ParticipantListDestination)
                },
            )
        }

        composable<UserListDestination> {
            UserListRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable<TrainingListDestination> {
            TrainingListRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { trainingId ->
                    navController.navigate(TrainingDetailDestination(trainingId))
                },
                onNavigateToCreate = {
                    navController.navigate(TrainingFormDestination(null))
                },
            )
        }

        composable<TrainingDetailDestination> {
            TrainingDetailRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { trainingId ->
                    navController.navigate(TrainingFormDestination(trainingId))
                },
            )
        }

        composable<TrainingFormDestination> {
            TrainingFormRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                },
            )
        }

        composable<ParticipantListDestination> {
            ParticipantListRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { participantId ->
                    navController.navigate(ParticipantDetailDestination(participantId))
                },
                onNavigateToCreate = {
                    navController.navigate(ParticipantFormDestination())
                },
            )
        }

        composable<ParticipantDetailDestination> {
            ParticipantDetailRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { participantId ->
                    navController.navigate(ParticipantFormDestination(participantId = participantId))
                },
            )
        }

        composable<ParticipantFormDestination> {
            ParticipantFormRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                },
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_epelatihan),
                contentDescription = stringResource(R.string.logo_content_description),
                modifier = Modifier.size(180.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
        }
    }
}
