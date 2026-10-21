package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Nordlicht-Farben aus dem Logo: Grün, Türkis, Lila */
object NordlichtColors {
    val Green = Color(0xFF3DFFB0)
    val Teal = Color(0xFF22C7D9)
    val Violet = Color(0xFF7B6CFF)

    /** Dunkles Lila für Fortschrittsbalken */
    val Progress = Color(0xFF4A3DB8)
}

/**
 * Lichtkante „Fein mit Hauch“: feine Verlaufslinie am oberen Bildrand
 * und ein ganz schwacher türkiser Schimmer darunter (ca. 12 % der Bildhöhe).
 */
@Composable
fun NordlichtEdge(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(maxHeight * 0.12f)
                    .background(
                        Brush.verticalGradient(
                            listOf(NordlichtColors.Teal.copy(alpha = 0.10f), Color.Transparent),
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(NordlichtColors.Green, NordlichtColors.Teal, NordlichtColors.Violet),
                        ),
                    ),
        )
    }
}
