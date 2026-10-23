package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.R

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
        val sky =
            Brush.linearGradient(
                listOf(NilumioColors.Mint, NilumioColors.Sky, NilumioColors.Lilac),
                start = Offset(c.x - r, 0f),
                end = Offset(c.x + r, 0f),
            )
        drawCircle(
            brush =
                Brush.radialGradient(
                    listOf(NilumioColors.Sky.copy(alpha = .45f * alpha), Color.Transparent),
                    center = c,
                    radius = r * 1.7f,
                ),
            radius = r * 1.7f,
            center = c,
        )
        val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        clipPath(disc) {
            drawCircle(brush = sky, radius = r, center = c, alpha = alpha)
            drawCircle(
                color = NilumioColors.Earth,
                radius = r * 1.507f,
                center = Offset(c.x + r * .053f, c.y + r * 1.747f),
                alpha = alpha,
            )
        }
    }
}

object NilumioColors {
    val Mint = Color(0xFF8CFFC4)
    val Sky = Color(0xFF6CCDE4)
    val Lilac = Color(0xFF8079FF)
    val Earth = Color(0xFF0B0F16)
    val Night = Color(0xFF070A12)
    val Gold = Color(0xFFF2C97D)

    private val avatars = listOf(Mint, Sky, Lilac, Gold)

    /** Feste Farbe je Person, gleich in Benutzerauswahl und SyncPlay. */
    fun avatar(name: String?): Color = avatars[Math.floorMod((name ?: "").lowercase().hashCode(), avatars.size)]

    fun onAvatar(color: Color): Color = if (color == Lilac) Color.White else Night
}

/** Outfit (OFL, statische Schnitte 300/400 aus store/brand), bisher nur im Player-Overlay. */
val NilumioOutfit =
    FontFamily(
        Font(R.font.outfit_light, FontWeight.Light),
        Font(R.font.outfit_regular, FontWeight.Normal),
    )

/** Fokusrahmen für Cover: weiß, 1,5 dp, mit leichtem Schein. */
@Composable
fun nilumioCardBorder() =
    androidx.tv.material3.CardDefaults.border(
        focusedBorder =
            androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            ),
    )

@Composable
fun nilumioCardGlow() =
    androidx.tv.material3.CardDefaults.glow(
        focusedGlow = androidx.tv.material3.Glow(elevationColor = Color.White.copy(alpha = .25f), elevation = 12.dp),
    )
