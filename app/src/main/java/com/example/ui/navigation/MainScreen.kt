package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.navigation.ScreenType

@Composable
fun MainScreen(viewModel: MainViewModel) {
    AnimatedContent(
        targetState = viewModel.currentScreen.value,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(300)
            )
        },
        label = "MainContent"
    ) { screen ->
        when (screen) {
            ScreenType.Splash -> {
                SplashScreen(onTimeout = { viewModel.navigateTo(ScreenType.Login) })
            }
            else -> {
                Text("Screen not implemented yet: $screen")
            }
        }
    }
}
