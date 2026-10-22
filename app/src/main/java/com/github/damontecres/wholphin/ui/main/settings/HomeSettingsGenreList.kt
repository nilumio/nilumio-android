package com.github.damontecres.wholphin.ui.main.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.tryRequestFocus
import java.util.UUID

data class GenreOption(
    val name: String,
    val id: UUID,
)

/**
 * Alle Genres des Servers, anfangs alle ausgewählt. Oben legt ein Knopf für jedes
 * ausgewählte Genre eine eigene Reihe mit Filmen und Serien an.
 */
@Composable
fun HomeSettingsGenreList(
    genres: List<GenreOption>?,
    onAdd: (List<GenreOption>) -> Unit,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester = remember { FocusRequester() },
) {
    var selected by remember(genres) { mutableStateOf(genres.orEmpty().map { it.id }.toSet()) }
    LaunchedEffect(genres) { if (!genres.isNullOrEmpty()) firstFocus.tryRequestFocus() }
    Column(modifier = modifier) {
        TitleText(stringResource(R.string.add_row_for, stringResource(R.string.by_genre)))
        when {
            genres == null -> Text(stringResource(R.string.loading))
            genres.isEmpty() -> Text(stringResource(R.string.no_results))
            else ->
                HomeSettingsLazyColumn(
                    modifier =
                        modifier
                            .fillMaxHeight()
                            .focusRestorer(firstFocus),
                ) {
                    item {
                        HomeSettingsListItem(
                            selected = false,
                            enabled = selected.isNotEmpty(),
                            headlineText = pluralStringResource(R.plurals.add_genre_rows, selected.size, selected.size),
                            onClick = { onAdd.invoke(genres.filter { it.id in selected }) },
                            modifier = Modifier.focusRequester(firstFocus),
                        )
                    }
                    item {
                        val all = selected.size == genres.size
                        HomeSettingsListItem(
                            selected = false,
                            headlineText = stringResource(if (all) R.string.select_none else R.string.select_all),
                            onClick = { selected = if (all) emptySet() else genres.map { it.id }.toSet() },
                        )
                    }
                    itemsIndexed(genres) { _, genre ->
                        val isOn = genre.id in selected
                        HomeSettingsListItem(
                            selected = isOn,
                            headlineText = genre.name,
                            trailingContent = { Text(if (isOn) "✓" else "") },
                            onClick = { selected = if (isOn) selected - genre.id else selected + genre.id },
                        )
                    }
                }
        }
    }
}
