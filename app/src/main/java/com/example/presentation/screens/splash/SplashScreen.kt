package com.example.presentation.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PlayerTeal
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06070B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stylized "G" in teal with a gold accent dot
            Text(
                text = "G.",
                color = PlayerTeal,
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-5).sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "GAMBIT",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Text(
                text = "CALCULATED RISK SIMULATOR",
                style = MaterialTheme.typography.labelSmall,
                color = PlayerTeal,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(500)
        onSplashFinished()
    }
}
