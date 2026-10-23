/*
 * nilumio – Seite „Zufall“: zufällige Folge einer Serie, kürzlich Gesehenes kommt selten dran.
 * Copyright (C) 2026 nilumio, GPL-2.0.
 */
package com.github.damontecres.wholphin.ui.shuffle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.ui.components.NordlichtIcons
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.tryRequestFocus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class ShuffleViewModel
    @Inject
    constructor(
        private val api: ApiClient,
        private val serverRepository: ServerRepository,
        private val navigationManager: NavigationManager,
    ) : ViewModel() {
        /** Serien, zuletzt geschaute zuerst; null = lädt. */
        val series = MutableStateFlow<List<BaseItemDto>?>(null)

        /** Film-Genres; null = lädt. */
        val genres = MutableStateFlow<List<BaseItemDto>?>(null)

        fun load() {
            if (series.value != null) return
            viewModelScope.launchIO {
                genres.value =
                    runCatching {
                        api.genresApi
                            .getGenres(
                                userId = serverRepository.currentUser?.id,
                                includeItemTypes = listOf(BaseItemKind.MOVIE),
                                sortBy = listOf(ItemSortBy.SORT_NAME),
                            ).content.items
                    }.getOrDefault(emptyList())
            }
            viewModelScope.launchIO {
                series.value =
                    runCatching {
                        api.itemsApi
                            .getItems(
                                userId = serverRepository.currentUser?.id,
                                includeItemTypes = listOf(BaseItemKind.SERIES),
                                recursive = true,
                                sortBy = listOf(ItemSortBy.DATE_PLAYED, ItemSortBy.SORT_NAME),
                                sortOrder = listOf(SortOrder.DESCENDING, SortOrder.ASCENDING),
                            ).content.items
                    }.onFailure { Timber.w(it, "Zufall: Serien laden fehlgeschlagen") }
                        .getOrDefault(emptyList())
            }
        }

        /** Spielt eine gewichtet zufällige Folge der Serie (oder einer zufälligen Serie). */
        fun playRandom(seriesId: UUID?) {
            viewModelScope.launchIO {
                val id = seriesId ?: series.value?.randomOrNull()?.id ?: return@launchIO
                runCatching {
                    val episodes =
                        api.itemsApi
                            .getItems(
                                userId = serverRepository.currentUser?.id,
                                parentId = id,
                                includeItemTypes = listOf(BaseItemKind.EPISODE),
                                recursive = true,
                                enableUserData = true,
                            ).content.items
                    pickWeighted(episodes)?.let { navigationManager.navigateTo(Destination.Playback(it.id, 0L)) }
                }.onFailure { Timber.w(it, "Zufall: Folge wählen fehlgeschlagen") }
            }
        }

        /** Spielt einen gewichtet zufälligen Film, optional aus einem Genre. */
        fun playRandomMovie(genreId: UUID?) {
            viewModelScope.launchIO {
                runCatching {
                    val movies =
                        api.itemsApi
                            .getItems(
                                userId = serverRepository.currentUser?.id,
                                includeItemTypes = listOf(BaseItemKind.MOVIE),
                                genreIds = genreId?.let { listOf(it) },
                                recursive = true,
                                enableUserData = true,
                            ).content.items
                    pickWeighted(movies)?.let { navigationManager.navigateTo(Destination.Playback(it.id, 0L)) }
                }.onFailure { Timber.w(it, "Zufall: Film wählen fehlgeschlagen") }
            }
        }

        companion object {
            /**
             * Gewicht je Folge: nie gesehen = 1. Gesehen in den letzten 14 Tagen = fast nie,
             * danach steigt das Gewicht langsam wieder an und ist nach etwa einem halben Jahr voll.
             */
            fun weight(
                lastPlayed: LocalDateTime?,
                now: LocalDateTime = LocalDateTime.now(),
            ): Double {
                if (lastPlayed == null) return 1.0
                val days = Duration.between(lastPlayed, now).toDays().coerceAtLeast(0)
                if (days < 14) return 0.01
                return (0.05 + (days / 180.0)).coerceAtMost(1.0)
            }

            fun pickWeighted(
                episodes: List<BaseItemDto>,
                random: Random = Random.Default,
            ): BaseItemDto? {
                if (episodes.isEmpty()) return null
                val weights = episodes.map { weight(it.userData?.lastPlayedDate) }
                var r = random.nextDouble() * weights.sum()
                episodes.forEachIndexed { i, e ->
                    r -= weights[i]
                    if (r <= 0) return e
                }
                return episodes.last()
            }
        }
    }

private val Panel = Color(0xFF111726)
private val Muted = Color(0xFF8E9AB0)
private val FocusRing = Color.White

@Composable
fun ShufflePage(
    modifier: Modifier = Modifier,
    viewModel: ShuffleViewModel = hiltViewModel(),
) {
    val series by viewModel.series.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        viewModel.load()
        focus.tryRequestFocus()
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(top = 40.dp, end = 48.dp, bottom = 32.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                Text(stringResource(R.string.shuffle_title), fontSize = 34.sp, color = Color.White)
                Text(stringResource(R.string.shuffle_subtitle), color = Muted, fontSize = 16.sp)
            }
        }
        item {
            // Überrasch mich: Film oder Folge, je zur Hälfte
            ShuffleRow(stringResource(R.string.shuffle_any), big = true, Modifier.focusRequester(focus)) {
                if (Random.nextBoolean()) viewModel.playRandom(null) else viewModel.playRandomMovie(null)
            }
        }
        // Filme nach Genre
        item { SectionTitle(stringResource(R.string.shuffle_movies)) }
        item { ShuffleRow(stringResource(R.string.shuffle_any_movie), big = false) { viewModel.playRandomMovie(null) } }
        items(genres.orEmpty(), key = { "g" + it.id }) { g ->
            ShuffleRow(g.name ?: "", big = false) { viewModel.playRandomMovie(g.id) }
        }
        // Serien: zufällige Folge
        item { SectionTitle(stringResource(R.string.shuffle_series)) }
        val list = series
        if (list == null) {
            item { Text(stringResource(R.string.loading), color = Muted) }
        } else {
            items(list, key = { "s" + it.id }) { s ->
                ShuffleRow(s.name ?: "", big = false) { viewModel.playRandom(s.id) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text.uppercase(), color = Muted, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 16.dp))

@Composable
private fun ShuffleRow(
    text: String,
    big: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Panel, focusedContainerColor = Color(0xFF182033)),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.5.dp, FocusRing), inset = 0.dp, shape = shape)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        modifier = modifier.fillMaxWidth(.6f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = if (big) 18.dp else 12.dp),
        ) {
            Icon(NordlichtIcons.Dice, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (big) 28.dp else 20.dp))
            Text(text, fontSize = if (big) 22.sp else 18.sp, color = Color.White)
        }
    }
}
