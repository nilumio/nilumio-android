/*
 * nilumio – Seite „Zufall“: zufälliger Film oder zufällige Folge, kürzlich Gesehenes kommt selten dran.
 * Copyright (C) 2026 nilumio, GPL-2.0.
 */
package com.github.damontecres.wholphin.ui.shuffle

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.ui.components.Nilumio
import com.github.damontecres.wholphin.ui.components.NilumioButton
import com.github.damontecres.wholphin.ui.components.NilumioIcons
import com.github.damontecres.wholphin.ui.components.NilumioPageHeader
import com.github.damontecres.wholphin.ui.components.NilumioSectionTitle
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

        private val userId get() = serverRepository.currentUser?.id

        fun load() {
            if (series.value != null) return
            viewModelScope.launchIO {
                genres.value =
                    runCatching {
                        api.genresApi
                            .getGenres(
                                userId = userId,
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
                                userId = userId,
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
        fun playRandom(seriesId: UUID?) =
            playPicked("Folge") {
                val id = seriesId ?: series.value?.randomOrNull()?.id ?: return@playPicked null
                val episodes =
                    api.itemsApi
                        .getItems(
                            userId = userId,
                            parentId = id,
                            includeItemTypes = listOf(BaseItemKind.EPISODE),
                            recursive = true,
                            enableUserData = true,
                        ).content.items
                pickWeighted(episodes)
            }

        /** Startet Staffel 1, Folge 1 einer zufälligen Serie (kürzlich Geschautes seltener). */
        fun playRandomFirstEpisode() =
            playPicked("erste Folge") {
                val pick = pickWeighted(series.value.orEmpty()) ?: return@playPicked null
                api.itemsApi
                    .getItems(
                        userId = userId,
                        parentId = pick.id,
                        includeItemTypes = listOf(BaseItemKind.EPISODE),
                        recursive = true,
                        sortBy = listOf(ItemSortBy.PARENT_INDEX_NUMBER, ItemSortBy.INDEX_NUMBER),
                    ).content.items
                    .firstOrNull { it.parentIndexNumber == 1 }
            }

        /** Spielt einen gewichtet zufälligen Film, optional aus einem Genre. */
        fun playRandomMovie(genreId: UUID?) =
            playPicked("Film") {
                val movies =
                    api.itemsApi
                        .getItems(
                            userId = userId,
                            includeItemTypes = listOf(BaseItemKind.MOVIE),
                            genreIds = genreId?.let { listOf(it) },
                            recursive = true,
                            enableUserData = true,
                        ).content.items
                pickWeighted(movies)
            }

        /** Wählt im Hintergrund etwas aus und spielt es von vorn ab; Fehler landen nur im Log. */
        private fun playPicked(
            what: String,
            pick: suspend () -> BaseItemDto?,
        ) = viewModelScope.launchIO {
            runCatching { pick()?.let { navigationManager.navigateTo(Destination.Playback(it.id, 0L)) } }
                .onFailure { Timber.w(it, "Zufall: %s wählen fehlgeschlagen", what) }
        }

        companion object {
            private const val BLOCKED_DAYS = 14
            private const val BLOCKED_WEIGHT = 0.01
            private const val RETURN_START_WEIGHT = 0.05
            private const val FULL_WEIGHT_DAYS = 180.0

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
                if (days < BLOCKED_DAYS) return BLOCKED_WEIGHT
                return (RETURN_START_WEIGHT + days / FULL_WEIGHT_DAYS).coerceAtMost(1.0)
            }

            /** Zieht ein Objekt, Wahrscheinlichkeit proportional zu [weight] des zuletzt Gesehenen. */
            fun pickWeighted(
                items: List<BaseItemDto>,
                random: Random = Random.Default,
            ): BaseItemDto? {
                if (items.isEmpty()) return null
                val weights = items.map { weight(it.userData?.lastPlayedDate) }
                var r = random.nextDouble() * weights.sum()
                items.forEachIndexed { i, item ->
                    r -= weights[i]
                    if (r <= 0) return item
                }
                return items.last()
            }
        }
    }

@Composable
fun ShufflePage(
    modifier: Modifier = Modifier,
    viewModel: ShuffleViewModel = hiltViewModel(),
) {
    val series by viewModel.series.collectAsState()
    val genres by viewModel.genres.collectAsState()
    var mode by rememberSaveable { mutableStateOf<ShuffleMode?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        viewModel.load()
        focus.tryRequestFocus()
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(top = 40.dp, end = 48.dp, bottom = 32.dp),
    ) {
        item { NilumioPageHeader(R.string.shuffle_title, R.string.shuffle_subtitle, Modifier.padding(bottom = 12.dp)) }
        // Zwei Wege oben, darunter die Liste zum gewählten Weg
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShuffleButton(R.string.shuffle_movies_button, mode == ShuffleMode.MOVIES, Modifier.focusRequester(focus)) {
                    mode = ShuffleMode.MOVIES
                }
                ShuffleButton(R.string.shuffle_series, mode == ShuffleMode.SERIES) { mode = ShuffleMode.SERIES }
            }
        }
        when (mode) {
            ShuffleMode.MOVIES -> {
                item { SectionTitle(R.string.shuffle_movies) }
                item { ShuffleRow(stringResource(R.string.shuffle_any_movie)) { viewModel.playRandomMovie(null) } }
                items(genres.orEmpty(), key = { "g" + it.id }) { g ->
                    ShuffleRow(g.name.orEmpty()) { viewModel.playRandomMovie(g.id) }
                }
            }

            ShuffleMode.SERIES -> {
                item { SectionTitle(R.string.shuffle_series) }
                item { ShuffleRow(stringResource(R.string.shuffle_first_episode)) { viewModel.playRandomFirstEpisode() } }
                val list = series
                if (list == null) {
                    item { Text(stringResource(R.string.loading), color = Nilumio.Muted) }
                } else {
                    items(list, key = { "s" + it.id }) { s ->
                        ShuffleRow(s.name.orEmpty()) { viewModel.playRandom(s.id) }
                    }
                }
            }

            null -> {
                Unit
            }
        }
    }
}

private enum class ShuffleMode { MOVIES, SERIES }

@Composable
private fun SectionTitle(
    @StringRes text: Int,
) = NilumioSectionTitle(stringResource(text), Modifier.padding(top = 16.dp))

/** Großer Knopf für einen der beiden Wege; der gewählte ist lila hinterlegt. */
@Composable
private fun ShuffleButton(
    @StringRes text: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = NilumioButton(
    onClick = onClick,
    containerColor = if (selected) Nilumio.CardRaised else Nilumio.Panel,
    focusedContainerColor = if (selected) Nilumio.CardFocused else Nilumio.PanelFocused,
    modifier = modifier,
) { DiceLabel(stringResource(text), big = true) }

/** Zeile der Liste: startet sofort etwas Zufälliges. */
@Composable
private fun ShuffleRow(
    text: String,
    onClick: () -> Unit,
) = NilumioButton(onClick = onClick, modifier = Modifier.fillMaxWidth(.6f)) { DiceLabel(text, big = false) }

@Composable
private fun DiceLabel(
    text: String,
    big: Boolean,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.padding(horizontal = 18.dp, vertical = if (big) 18.dp else 12.dp),
) {
    Icon(NilumioIcons.Dice, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (big) 28.dp else 20.dp))
    Text(text, fontSize = if (big) 22.sp else 18.sp, color = Color.White)
}
