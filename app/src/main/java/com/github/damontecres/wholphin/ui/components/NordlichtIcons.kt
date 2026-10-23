package com.github.damontecres.wholphin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.tv.material3.NavigationDrawerItemColors
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Feine Strich-Icons für die Seitenleiste (24er-Raster, dünne runde Linien).
 * Jede Zeile ist ein SVG-Pfad mit optionaler Deckkraft; eingefärbt wird über den Icon-Tint.
 */
object NordlichtIcons {
    private const val STROKE = 1.3f

    private fun icon(
        name: String,
        vararg paths: Pair<String, Float>,
        strokeWidth: Float = STROKE,
    ): ImageVector =
        ImageVector
            .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { (d, alpha) ->
                    addPath(
                        pathData = addPathNodes(d),
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeAlpha = alpha,
                        strokeLineWidth = strokeWidth,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }.build()

    private const val RING_SEARCH = "M16.7,10.5a6.2,6.2 0 1,1 -12.4,0a6.2,6.2 0 1,1 12.4,0"

    val Search =
        icon(
            "nl_search",
            RING_SEARCH to 1f,
            "M15.2,15.2L20,20" to 1f,
            "M7.6,9.2a3.2,3.2 0 0,1 2.4,-1.9" to .6f,
        )

    val Home =
        icon(
            "nl_home",
            "M4,11.2L12,4.5l8,6.7" to 1f,
            "M6.3,9.6V19.5h11.4V9.6" to 1f,
            "M10,19.5v-4.6h4v4.6" to 1f,
        )

    val Movies =
        icon(
            "nl_movies",
            "M5.7,5.5h12.6a2.2,2.2 0 0,1 2.2,2.2v8.6a2.2,2.2 0 0,1 -2.2,2.2H5.7a2.2,2.2 0 0,1 -2.2,-2.2V7.7a2.2,2.2 0 0,1 2.2,-2.2z" to 1f,
            "M3.5,9.5h17M3.5,14.5h17M8,5.5v4M12,5.5v4M16,5.5v4" to .7f,
        )

    val Series =
        icon(
            "nl_series",
            "M5.7,7h12.6a2.2,2.2 0 0,1 2.2,2.2v7.1a2.2,2.2 0 0,1 -2.2,2.2H5.7a2.2,2.2 0 0,1 -2.2,-2.2V9.2A2.2,2.2 0 0,1 5.7,7z" to 1f,
            "M8.5,3.8L12,7l3.5,-3.2" to 1f,
            "M7,21h10" to .7f,
        )

    val LiveTv =
        icon(
            "nl_livetv",
            "M13.8,12a1.8,1.8 0 1,1 -3.6,0a1.8,1.8 0 1,1 3.6,0" to 1f,
            "M8.3,15.7a5.2,5.2 0 0,1 0,-7.4M15.7,8.3a5.2,5.2 0 0,1 0,7.4" to 1f,
            "M5.5,18.5a9.2,9.2 0 0,1 0,-13M18.5,5.5a9.2,9.2 0 0,1 0,13" to .6f,
        )

    val Favorites =
        icon(
            "nl_favorites",
            "M12,19.6s-7.2,-4.4 -7.2,-9.6A3.9,3.9 0 0,1 12,7.6a3.9,3.9 0 0,1 7.2,2.4c0,5.2 -7.2,9.6 -7.2,9.6z" to 1f,
        )

    val Settings =
        icon(
            "nl_settings",
            "M14.6,12a2.6,2.6 0 1,1 -5.2,0a2.6,2.6 0 1,1 5.2,0" to 1f,
            "M12,3.5v2.2M12,18.3v2.2M3.5,12h2.2M18.3,12h2.2M6,6l1.6,1.6M16.4,16.4L18,18M6,18l1.6,-1.6M16.4,7.6L18,6" to 1f,
        )

    val More = icon("nl_more", "M6,12h0.01M12,12h0.01M18,12h0.01" to 1f, strokeWidth = 2.4f)

    val Discover =
        icon(
            "nl_discover",
            RING_SEARCH to 1f,
            "M15.2,15.2L20,20" to 1f,
            "M8.3,10.5h4.4M10.5,8.3v4.4" to .8f,
        )

    val Music =
        icon(
            "nl_music",
            "M9,17.5V6.5l10,-2v11" to 1f,
            "M9,17.5a2,2 0 1,1 -4,0a2,2 0 1,1 4,0" to 1f,
            "M19,15.5a2,2 0 1,1 -4,0a2,2 0 1,1 4,0" to 1f,
        )

    val HomeVideos =
        icon(
            "nl_homevideos",
            "M4.5,7.5h9a1.5,1.5 0 0,1 1.5,1.5v6a1.5,1.5 0 0,1 -1.5,1.5h-9A1.5,1.5 0 0,1 3,15V9A1.5,1.5 0 0,1 4.5,7.5z" to 1f,
            "M15,10.5l5,-2.5v8l-5,-2.5" to 1f,
        )

    val Collections =
        icon(
            "nl_collections",
            "M3.5,7.5a1.5,1.5 0 0,1 1.5,-1.5h4l2,2h8a1.5,1.5 0 0,1 1.5,1.5v8.5a1.5,1.5 0 0,1 -1.5,1.5H5a1.5,1.5 0 0,1 -1.5,-1.5z" to 1f,
        )

    val Playlists = icon("nl_playlists", "M4,7h11M4,12h11M4,17h7" to 1f, "M18,14v6M15,17h6" to .8f)

    val NowPlaying = icon("nl_nowplaying", "M8,5.5v13l10,-6.5z" to 1f)

    /** SyncPlay: zwei Personen. */
    val WatchTogether =
        icon(
            "nl_watchtogether",
            "M11.6,8.8a2.6,2.6 0 1,1 -5.2,0a2.6,2.6 0 1,1 5.2,0" to 1f,
            "M4,19c0,-3 2.2,-5 5,-5s5,2 5,5" to 1f,
            "M18.2,9.4a2.2,2.2 0 1,1 -4.4,0a2.2,2.2 0 1,1 4.4,0" to .7f,
            "M15.4,14.2c2.6,-0.3 4.6,1.6 4.6,4.8" to .7f,
        )
}

/** Färbt den Inhalt (z. B. ein weiß getöntes Icon) im nordlicht-Verlauf Grün → Türkis → Lila ein. */
fun Modifier.nordlichtGradient(enabled: Boolean): Modifier =
    if (!enabled) {
        this
    } else {
        this
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val brush =
                    Brush.linearGradient(
                        listOf(NordlichtColors.Green, NordlichtColors.Teal, NordlichtColors.Violet),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    )
                onDrawWithContent {
                    drawContent()
                    drawRect(brush, blendMode = BlendMode.SrcIn)
                }
            }
    }

/** Design A: dunkelgraue statt weiße Hinterlegung, Schrift bleibt hell. */
@Composable
fun nordlichtDrawerItemColors(containerColor: Color = Color.Transparent): NavigationDrawerItemColors =
    NavigationDrawerItemDefaults.colors(
        containerColor = containerColor,
        focusedContainerColor = Color.White.copy(alpha = 0.10f),
        focusedContentColor = Color.White,
        pressedContainerColor = Color.White.copy(alpha = 0.14f),
        pressedContentColor = Color.White,
        selectedContainerColor = Color.White.copy(alpha = 0.06f),
        selectedContentColor = Color.White,
        focusedSelectedContainerColor = Color.White.copy(alpha = 0.12f),
        focusedSelectedContentColor = Color.White,
    )
