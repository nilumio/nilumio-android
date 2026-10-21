package com.github.damontecres.wholphin.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.tv.material3.Typography

private val base = Typography()

// Nordlicht: kräftige, eng gesetzte Titel für die Fernseh-Distanz
val AppTypography =
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.02).em),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.02).em),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).em),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    )
