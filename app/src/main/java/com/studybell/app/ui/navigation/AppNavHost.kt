package com.studybell.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studybell.app.container
import com.studybell.app.core.theme.StudyColors
import com.studybell.app.ui.editor.EditorScreen
import com.studybell.app.ui.home.HomeScreen
import com.studybell.app.ui.onboarding.OnboardingScreen
import com.studybell.app.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{alarmId}"
    fun editor(id: Long) = "editor/$id"
}

@Composable
fun AppNavHost() {
    val container = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var startRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startRoute = if (container.settingsStore.settings.first().onboardingDone) Routes.HOME else Routes.ONBOARDING
    }
    val start = startRoute
    if (start == null) {
        Box(Modifier.fillMaxSize().background(StudyColors.Background))
        return
    }

    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                scope.launch { container.settingsStore.setOnboardingDone(true) }
                if (nav.previousBackStackEntry != null) nav.popBackStack()
                else nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onAddAlarm = { nav.navigate(Routes.editor(-1L)) },
                onEditAlarm = { nav.navigate(Routes.editor(it)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            EditorScreen(alarmId = entry.arguments?.getLong("alarmId") ?: -1L, onClose = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() }, onOpenSetup = { nav.navigate(Routes.ONBOARDING) })
        }
    }
}
