/*
 * nilumio – Farben, Schrift und kleine Bausteine, die mehrere eigene Seiten teilen.
 * Copyright (C) 2026 nilumio, GPL-2.0.
 */
package com.github.damontecres.wholphin.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R

/** Alle nilumio-Farben und die Schrift an einem Ort. */
object Nilumio {
    // Logo-Verlauf Grün → Türkis → Lila
    val Mint = Color(0xFF8CFFC4)
    val Sky = Color(0xFF6CCDE4)
    val Lilac = Color(0xFF8079FF)
    val Gradient = listOf(Mint, Sky, Lilac)

    val Earth = Color(0xFF0B0F16)
    val Night = Color(0xFF070A12)
    val Gold = Color(0xFFF2C97D)

    /** Anteil Nachtblau im Verlauf der Fortschrittsbalken, damit sie nicht grell wirken. */
    private const val PROGRESS_DIM = .28f

    /** Logo-Verlauf, abgedunkelt: Fortschritt unter Covern, Zeitleiste und Spul-Leiste im Player. */
    val ProgressGradient = Gradient.map { lerp(it, Night, PROGRESS_DIM) }

    /** Kräftigerer Verlauf für die Symbole der Seitenleiste. */
    val DrawerGradient = listOf(Color(0xFF3DFFB0), Color(0xFF22C7D9), Color(0xFF7B6CFF))

    // Dunkle Flächen der eigenen Seiten (SyncPlay, Zufall, Menü-Editor)
    val Panel = Color(0xFF111726)
    val PanelFocused = Color(0xFF182033)
    val PanelHighlight = Color(0xFF243049)
    val Muted = Color(0xFF8E9AB0)
    val FocusRing = Color(0xFF22ABD8).copy(alpha = .62f)

    // Lila Karten (Farbton 250): Lobby-Karte, eigene Lobby, gewählter Weg bei Zufall
    val Card = Color(0xFF1E1938)
    val CardRaised = Color(0xFF29224F)
    val CardFocused = Color(0xFF392D76)
    val CardRing = Color(0xFF927EF1).copy(alpha = .7f)
    val CardMuted = Color(0xFFB5AFCF)
    val CardGlowStart = Color(0xFF8A75F0)
    val CardGlowEnd = Color(0xFFD55EED)

    /** Outfit (OFL, statische Schnitte 300/400 aus store/brand), im Player-Overlay. */
    val Outfit =
        FontFamily(
            Font(R.font.outfit_light, FontWeight.Light),
            Font(R.font.outfit_regular, FontWeight.Normal),
        )

    private val avatars = listOf(Mint, Sky, Lilac, Gold)

    /** Feste Farbe je Person, gleich in Benutzerauswahl und SyncPlay. */
    fun avatar(name: String?): Color = avatars[Math.floorMod(name.orEmpty().lowercase().hashCode(), avatars.size)]

    fun onAvatar(color: Color): Color = if (color == Lilac) Color.White else Night

    /** Farbe von [ProgressGradient] an der Stelle [fraction] (0 = links, 1 = rechts). */
    fun progressColorAt(fraction: Float): Color {
        val (start, middle, end) = ProgressGradient
        return if (fraction < .5f) lerp(start, middle, fraction * 2f) else lerp(middle, end, (fraction - .5f) * 2f)
    }
}

/** Fokusrahmen für Cover: weiß, 1,5 dp, mit leichtem Schein. */
@Composable
fun nilumioCardBorder() =
    CardDefaults.border(
        focusedBorder = Border(BorderStroke(1.5.dp, Color.White), shape = RoundedCornerShape(8.dp)),
    )

@Composable
fun nilumioCardGlow() = CardDefaults.glow(focusedGlow = Glow(elevationColor = Color.White.copy(alpha = .25f), elevation = 12.dp))

/** Fortschritt unter Covern im (abgedunkelten) Logo-Verlauf; die Farbe hängt von der Stelle ab (rechts Lila). */
fun Modifier.lichtkanteProgress(fraction: Float): Modifier =
    fillMaxWidth().drawBehind {
        drawRect(
            Brush.horizontalGradient(Nilumio.ProgressGradient, endX = size.width),
            size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
        )
    }

/** Seitenkopf der eigenen Seiten: großer Titel, darunter eine gedämpfte Zeile. */
@Composable
fun NilumioPageHeader(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    modifier: Modifier = Modifier,
) = Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
    Text(stringResource(title), fontSize = 34.sp, color = Color.White)
    Text(stringResource(subtitle), color = Nilumio.Muted, fontSize = 16.sp)
}

/** Abschnittsüberschrift in Großbuchstaben. */
@Composable
fun NilumioSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) = Text(text.uppercase(), color = Nilumio.Muted, fontSize = 13.sp, letterSpacing = 1.sp, modifier = modifier)

/** Fokussierbare Fläche der eigenen Seiten: dunkel, mit Fokusrahmen und leichtem Vergrößern. */
@Composable
fun NilumioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color = Nilumio.Panel,
    focusedContainerColor: Color = Nilumio.PanelFocused,
    ring: BorderStroke = BorderStroke(1.5.dp, Color.White),
    focusedScale: Float = 1.02f,
    content: @Composable BoxScope.() -> Unit,
) = Surface(
    onClick = onClick,
    shape = ClickableSurfaceDefaults.shape(shape),
    colors =
        ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = Color.White,
            focusedContainerColor = focusedContainerColor,
            focusedContentColor = Color.White,
        ),
    border = ClickableSurfaceDefaults.border(focusedBorder = Border(ring, inset = 0.dp, shape = shape)),
    scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
    modifier = modifier,
    content = content,
)
