/*
 * nilumio – Player-Steuerung „Lichtkante“ (Vorlage „Lichtkante groß“, Variante 13): Titel und
 * Metazeile, randlose Zeitleiste im Logo-Verlauf, eine Knopfzeile mit Zeit, Wiedergabe und Spuren.
 * Copyright (C) 2026 nilumio, GPL-2.0.
 */
package com.github.damontecres.wholphin.ui.playback.overlay

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.components.Button
import com.github.damontecres.wholphin.ui.components.Nilumio
import com.github.damontecres.wholphin.ui.formatDuration
import com.github.damontecres.wholphin.ui.joinNotBlank
import com.github.damontecres.wholphin.ui.seasonEpisodePadded
import com.github.damontecres.wholphin.ui.seekBack
import com.github.damontecres.wholphin.ui.seekForward
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.extensions.ticks
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Größenfaktor --k der Vorlage. Maße sind Vorlage-em × k in dp (1 em = 8 dp bei 960 dp Breite). */
internal const val LICHTKANTE_K = 2f

/** Faktor nur für die Knopfzeile (Symbole, Abstände, Zeit): 80 % von [LICHTKANTE_K]. */
internal const val LICHTKANTE_BUTTON_K = 1.6f

/** Seitenrand von Titel, Zeit und Knöpfen (5 em). */
internal val LichtkanteEdge = 40.dp

/** Sichtbarer Abstand zwischen den Symbolen 1,3 em × k (abzüglich Knopfrand). */
internal val LichtkanteButtonSpacing = (5.4f * LICHTKANTE_BUTTON_K).dp

/** Abstand Zeitleiste → Knopfzeile; deren Mitte bleibt an der Stelle der Vorlage, auch wenn die Knöpfe kleiner sind. */
internal val LichtkanteRowGap = (10.22f * LICHTKANTE_K + 11.3f * (LICHTKANTE_K - LICHTKANTE_BUTTON_K)).dp

/** Abstand Titelblock → Zeitleiste. */
internal val LichtkanteTitleGap = (20f + 6.48f * LICHTKANTE_K).dp

/** Rechte Symbole bündig mit dem Rand von Titel und Zeit, nicht mit der Knopffläche. */
internal val LichtkanteIconInset = (2.5f * LICHTKANTE_BUTTON_K).dp

/** Höhe der Zeitleiste (Platz für Kugel und Schein). */
internal val LichtkanteSeekBarHeight = (8f * LICHTKANTE_K).dp

private val LichtkanteText = Color(0xFFEEF2F8)

/** Gedämpftes Weiß wie --muted der Vorlage. */
internal val LichtkanteMuted = LichtkanteText.copy(alpha = .62f)

/** Sprungweite der Knöpfe; Links/Rechts auf der Zeitleiste springt weiter um die Einstellung. */
private const val BUTTON_SKIP_SECONDS = 30

private const val POSITION_POLL_MS = 250L

/** Schein unter dem Fortschritt: Breite in dp und Deckkraft, von außen nach innen (passend zum abgedunkelten Verlauf). */
private val SeekGlow = listOf(12f to .045f, 7.5f to .075f, 5f to .12f)

/** Deckkraft des Hofs um die Kugel. */
private const val KNOB_HALO_ALPHA = .4f

/** Lässt das Element links und rechts um [edge] über den Rand hinausragen (randlose Zeitleiste). */
internal fun Modifier.bleedHorizontally(edge: Dp) =
    layout { measurable, constraints ->
        val px = edge.roundToPx()
        val width = constraints.maxWidth + 2 * px
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-px, 0) }
    }

/** Titel groß und leicht, darunter die gedämpfte Metazeile (ohne Endzeit). */
@Composable
internal fun LichtkanteTitle(
    title: String?,
    subtitle: String?,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    title?.let {
        Text(
            text = it,
            color = LichtkanteText,
            fontFamily = Nilumio.Outfit,
            fontWeight = FontWeight.Light,
            fontSize = titleTextSize,
            lineHeight = titleTextSize * 1.05f,
            letterSpacing = titleTextSize * .005f,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.fillMaxWidth(.75f),
        )
    }
    subtitle?.let {
        Text(
            text = it,
            color = LichtkanteMuted,
            fontFamily = Nilumio.Outfit,
            fontSize = subtitleTextSize,
            lineHeight = subtitleTextSize * 1.45f,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = (.75f * LICHTKANTE_K).dp).fillMaxWidth(.75f),
        )
    }
}

/** Metazeile: bei Folgen „S08F04 · Folgentitel“, sonst „Jahr · Genre · Dauer“. */
@Composable
internal fun rememberPlayerMeta(item: BaseItem?): String? {
    val resources = LocalResources.current
    return remember(item) { item?.let { playerMeta(it, resources) } }
}

private fun playerMeta(
    item: BaseItem,
    resources: Resources,
): String? =
    if (item.type == BaseItemKind.EPISODE) {
        listOf(item.data.seasonEpisodePadded, item.name).joinNotBlank(" · ")
    } else {
        listOf(
            item.data.productionYear?.toString(),
            item.data.genres?.firstOrNull(),
            item.data.runTimeTicks
                ?.ticks
                ?.inWholeMinutes
                ?.takeIf { it > 0 }
                ?.let { resources.formatDuration(it.minutes) },
        ).joinNotBlank(" · ")
    }

/** Zeitangabe links in der Knopfzeile, aktualisiert sich selbst. */
@Composable
internal fun PlayerTimecode(
    player: Player,
    modifier: Modifier = Modifier,
) {
    var positionMs by remember(player) { mutableLongStateOf(player.currentPosition) }
    LaunchedEffect(player) {
        while (isActive) {
            positionMs = player.currentPosition
            delay(POSITION_POLL_MS)
        }
    }
    LichtkanteTimecode(positionMs, player.duration, modifier)
}

/** Eine Angabe „4:31 / 12:14“ mit festen Ziffernbreiten statt „4m 31s“ links und Restzeit rechts. */
@Composable
internal fun LichtkanteTimecode(
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val positionSec = positionMs / 1000
    val durationSec = durationMs / 1000
    val text =
        remember(positionSec, durationSec) {
            if (durationMs > 0) "${formatTimecode(positionSec)} / ${formatTimecode(durationSec)}" else formatTimecode(positionSec)
        }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = text,
            color = LichtkanteMuted,
            fontFamily = Nilumio.Outfit,
            fontSize = (10.56f * LICHTKANTE_BUTTON_K).sp,
            style = TextStyle(fontFeatureSettings = "tnum"),
            modifier = modifier,
        )
    }
}

/** „1:02:03“ bzw. „4:05“. */
internal fun formatTimecode(totalSec: Long): String {
    val h = totalSec / 3600
    val m = totalSec % 3600 / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
}

/** Zeitleiste: dünne Linie, Fortschritt im Logo-Verlauf mit Schein, Position als Kugel in der Farbe ihrer Stelle. */
internal fun DrawScope.drawLichtkanteSeekBar(
    progress: Float,
    bufferedProgress: Float,
    trackColor: Color,
    focusScale: Float,
    lineScale: Float,
) {
    val y = size.height / 2
    val line = (1.44f * LICHTKANTE_K).dp.toPx() * focusScale * lineScale
    val start = Offset(0f, y)
    val end = Offset(size.width * progress, y)
    // Verlauf über die ganze Breite: links Mint, rechts Lila
    val logo = Brush.horizontalGradient(Nilumio.ProgressGradient, endX = size.width)
    drawLine(trackColor.copy(alpha = 0.18f), start, Offset(size.width, y), line, StrokeCap.Round)
    drawLine(trackColor.copy(alpha = 0.1f), start, Offset(size.width * bufferedProgress, y), line, StrokeCap.Round)
    // Schein im selben Verlauf, damit die Leiste nicht bläulich wirkt
    for ((width, alpha) in SeekGlow) {
        drawLine(logo, start, end, width.dp.toPx() * lineScale, StrokeCap.Round, alpha = alpha)
    }
    drawLine(logo, start, end, line, StrokeCap.Round)
    val here = Nilumio.progressColorAt(progress)
    val r = (2.6f * LICHTKANTE_K).dp.toPx() * focusScale
    drawCircle(Brush.radialGradient(listOf(here.copy(alpha = KNOB_HALO_ALPHA), Color.Transparent), end, r * 2.6f), r * 2.6f, end)
    drawCircle(
        Brush.radialGradient(
            0f to lerp(here, Color.White, .45f),
            1f to here,
            center = Offset(end.x - r * .25f, end.y - r * .3f),
            radius = r * 1.4f,
        ),
        r,
        end,
    )
}

/** Inhalt von [PlaybackButton]: Symbol im grauen Kreis, ausgewählt weiß mit Mint-Schein; Play/Pause größer. */
@Composable
internal fun LichtkanteIconButton(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    onControllerInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    big: Boolean = false,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LichtkanteButton(onClick, onControllerInteraction, modifier, enabled, source, big) {
        LichtkanteIcon(iconRes, focused, Modifier.fillMaxSize())
    }
}

/** Kreispfeil mit Sekundenzahl, vorwärts gespiegelt; springt [BUTTON_SKIP_SECONDS] weit. */
@Composable
internal fun PlaybackSkipButton(
    forward: Boolean,
    player: Player,
    onControllerInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val onClick = {
        onControllerInteraction()
        if (forward) player.seekForward(BUTTON_SKIP_SECONDS.seconds) else player.seekBack(BUTTON_SKIP_SECONDS.seconds)
    }
    LichtkanteButton(onClick, onControllerInteraction, modifier, true, source) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            LichtkanteIcon(
                iconRes = R.drawable.nilumio_player_skip,
                focused = focused,
                modifier = Modifier.fillMaxSize().graphicsLayer { if (forward) scaleX = -1f },
            )
            // Zahl wie in der Vorlage: 7 von 24 Einheiten, Grundlinie bei 15,5
            Text(
                text = BUTTON_SKIP_SECONDS.toString(),
                fontFamily = Nilumio.Outfit,
                fontSize = (5.13f * LICHTKANTE_BUTTON_K).sp,
                lineHeight = (5.13f * LICHTKANTE_BUTTON_K).sp,
                color = LocalContentColor.current,
                style =
                    TextStyle(
                        shadow = if (focused) Shadow(Nilumio.Mint, blurRadius = 8f * LICHTKANTE_BUTTON_K) else null,
                        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                    ),
                modifier = Modifier.padding(top = (.5f * LICHTKANTE_BUTTON_K).dp),
            )
        }
    }
}

/** Grau hinterlegter Kreis, Symbol gedämpft; ausgewählt heller. */
@Composable
private fun LichtkanteButton(
    onClick: () -> Unit,
    onControllerInteraction: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    big: Boolean = false,
    content: @Composable () -> Unit,
) = Button(
    enabled = enabled,
    onClick = onClick,
    colors =
        ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = .14f),
            contentColor = Color.White.copy(alpha = .85f),
            focusedContainerColor = Color.White.copy(alpha = .32f),
            focusedContentColor = Color.White,
            pressedContainerColor = Color.White.copy(alpha = .4f),
            pressedContentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = .25f),
        ),
    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
    contentPadding = PaddingValues(((if (big) 9.5f else 7f) * LICHTKANTE_BUTTON_K).dp),
    interactionSource = interactionSource,
    modifier =
        modifier
            .size(((if (big) 38f else 30f) * LICHTKANTE_BUTTON_K).dp)
            .onFocusChanged { onControllerInteraction.invoke() },
) {
    content()
}

/** Mint-Schein hinter dem Symbol wie drop-shadow in der Vorlage (Weichzeichnen ab Android 12). */
@Composable
private fun LichtkanteIcon(
    @DrawableRes iconRes: Int,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val painter = painterResource(iconRes)
    Box(modifier) {
        if (focused) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = Nilumio.Mint.copy(alpha = .9f),
                modifier = Modifier.matchParentSize().blur((3.5f * LICHTKANTE_BUTTON_K).dp, BlurredEdgeTreatment.Unbounded),
            )
        }
        Icon(painter = painter, contentDescription = null, tint = LocalContentColor.current, modifier = Modifier.matchParentSize())
    }
}
