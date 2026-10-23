package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath

/**
 * nilumio-Zeichen: Kugel im Verlauf Grün → Türkis → Lila mit dunkler Erdkrümmung
 * und weichem Glühen dahinter. Geometrie wie in store/brand/nilumio-zeichen.svg.
 */
@Composable
fun NilumioMark(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f * 0.82f
        val c = center
        val glow = r * 1.7f
        drawCircle(
            brush = Brush.radialGradient(listOf(Nilumio.Sky.copy(alpha = .45f * alpha), Color.Transparent), center = c, radius = glow),
            radius = glow,
            center = c,
        )
        clipPath(Path().apply { addOval(Rect(c, r)) }) {
            drawCircle(
                brush = Brush.linearGradient(Nilumio.Gradient, start = Offset(c.x - r, 0f), end = Offset(c.x + r, 0f)),
                radius = r,
                center = c,
                alpha = alpha,
            )
            drawCircle(
                color = Nilumio.Earth,
                radius = r * 1.507f,
                center = Offset(c.x + r * .053f, c.y + r * 1.747f),
                alpha = alpha,
            )
        }
    }
}
