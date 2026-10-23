/*
 * nilumio – Seitenleiste: eigene Menüpunkte, Strich-Icons im Verlauf, dunkelgraue Hinterlegung.
 * Copyright (C) 2026 nilumio, GPL-2.0.
 */
package com.github.damontecres.wholphin.ui.nav

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawerItemColors
import androidx.tv.material3.NavigationDrawerItemDefaults
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.components.Nilumio
import com.github.damontecres.wholphin.ui.components.NilumioIcons
import org.jellyfin.sdk.model.api.CollectionType

/** Eigene Seiten als normale Menüpunkte; standardmäßig angeheftet (siehe NavDrawerService). */
sealed class NilumioNavDrawerItem(
    override val id: String,
    @param:StringRes private val title: Int,
    val destination: Destination,
) : NavDrawerItem {
    override fun name(context: Context): String = context.getString(title)

    object Shuffle : NilumioNavDrawerItem("n_shuffle", R.string.shuffle_title, Destination.Shuffle)

    object SyncPlay : NilumioNavDrawerItem("n_syncplay", R.string.syncplay_title, Destination.SyncPlay)
}

/** Strich-Icon je Menüpunkt statt der Font-Awesome-Zeichen. */
fun NavDrawerItem.nilumioIcon(): ImageVector =
    when (this) {
        NavDrawerItem.Favorites -> NilumioIcons.Favorites
        NavDrawerItem.More -> NilumioIcons.More
        NavDrawerItem.Discover -> NilumioIcons.Discover
        NilumioNavDrawerItem.Shuffle -> NilumioIcons.Dice
        NilumioNavDrawerItem.SyncPlay -> NilumioIcons.WatchTogether
        is ServerNavDrawerItem -> libraryIcon(type)
    }

private fun libraryIcon(type: CollectionType): ImageVector =
    when (type) {
        CollectionType.TVSHOWS -> NilumioIcons.Series
        CollectionType.HOMEVIDEOS -> NilumioIcons.HomeVideos
        CollectionType.LIVETV -> NilumioIcons.LiveTv
        CollectionType.MUSIC -> NilumioIcons.Music
        CollectionType.BOXSETS -> NilumioIcons.Collections
        CollectionType.PLAYLISTS -> NilumioIcons.Playlists
        else -> NilumioIcons.Movies
    }

/** Menüsymbol: im Ruhezustand wie bei Wholphin getönt, ausgewählt oder fokussiert weiß im Verlauf. */
@Composable
fun NilumioDrawerIcon(
    icon: ImageVector,
    selected: Boolean,
    focused: Boolean,
    drawerOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    val active = selected || focused
    Icon(
        icon,
        contentDescription = null,
        tint = if (active) Color.White else navItemColor(selected, focused, drawerOpen),
        modifier = modifier.size(DrawerIconSize).drawerGradient(active),
    )
}

/** Dunkelgraue statt weißer Hinterlegung, Schrift bleibt hell. */
@Composable
fun nilumioDrawerItemColors(containerColor: Color = Color.Transparent): NavigationDrawerItemColors =
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

/** Färbt den (weiß getönten) Inhalt im Verlauf der Seitenleiste ein. */
private fun Modifier.drawerGradient(enabled: Boolean): Modifier =
    if (!enabled) {
        this
    } else {
        graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithCache {
            val brush = Brush.linearGradient(Nilumio.DrawerGradient, start = Offset.Zero, end = Offset(size.width, size.height))
            onDrawWithContent {
                drawContent()
                drawRect(brush, blendMode = BlendMode.SrcIn)
            }
        }
    }
