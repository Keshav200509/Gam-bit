package com.example.presentation.screens.intro

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.presentation.screens.onboarding.OnboardingFlow

@Composable
fun IntroScreen(
    onBeginClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingFlow(
        onFinished = onBeginClicked,
        modifier = modifier
    )
}
