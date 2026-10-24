/*
 * nilumio – „Menü anpassen“: zwei Listen („Im Menü“ / „Nicht im Menü“) statt der Schalterliste.
 * Copyright (C) 2026 nilumio, GPL-2.0.
 */
package com.github.damontecres.wholphin.ui.preferences

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.ui.components.BasicDialog
import com.github.damontecres.wholphin.ui.components.Nilumio
import com.github.damontecres.wholphin.ui.components.NilumioButton
import com.github.damontecres.wholphin.ui.components.NilumioIcons
import com.github.damontecres.wholphin.ui.components.NilumioSectionTitle
import com.github.damontecres.wholphin.ui.tryRequestFocus
import kotlinx.coroutines.delay

private enum class PinButton { UP, DOWN, REMOVE, ADD }

/** Eintrag und Knopf, auf dem der Fokus nach einer Änderung landen soll. */
private typealias FocusTarget = Pair<String, PinButton>

/** So oft wird der Fokus versucht, weil die geänderte Liste evtl. erst ein paar Frames später steht. */
private const val FOCUS_ATTEMPTS = 10
private const val FOCUS_RETRY_MS = 32L

@Composable
fun NavMenuEditorDialog(
    items: List<NavDrawerPin>,
    onUpdate: (List<NavDrawerPin>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val pinned = items.filter { it.pinned }
    val unpinned = items.filterNot { it.pinned }
    // Ein FocusRequester je Eintrag und Knopf, damit der Fokus nach dem Verschieben mitwandert
    val requesters = remember { mutableMapOf<FocusTarget, FocusRequester>() }

    fun requester(
        id: String,
        button: PinButton,
    ) = requesters.getOrPut(id to button) { FocusRequester() }
    var pendingFocus by remember { mutableStateOf<FocusTarget?>(null) }
    LaunchedEffect(items.isNotEmpty()) {
        if (pendingFocus == null) {
            pendingFocus = pinned.firstOrNull()?.let { it.id to PinButton.REMOVE } ?: unpinned.firstOrNull()?.let { it.id to PinButton.ADD }
        }
    }
    LaunchedEffect(pendingFocus, items) {
        val target = pendingFocus ?: return@LaunchedEffect
        repeat(FOCUS_ATTEMPTS) {
            if (requesters[target]?.tryRequestFocus() == true) return@LaunchedEffect
            delay(FOCUS_RETRY_MS)
        }
    }

    // Reihenfolge wird immer als „angeheftet, dann übrige“ gespeichert
    fun commit(
        newPinned: List<NavDrawerPin>,
        newUnpinned: List<NavDrawerPin>,
        focus: FocusTarget,
    ) {
        onUpdate(newPinned.map { it.copy(pinned = true) } + newUnpinned.map { it.copy(pinned = false) })
        pendingFocus = focus
    }

    BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        elevation = 3.dp,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .width(820.dp)
                    .height(460.dp)
                    .padding(24.dp),
        ) {
            Text(stringResource(R.string.nav_menu_title), fontSize = 24.sp, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                PinPanel(R.string.nav_menu_in, R.string.nav_menu_none_in, pinned.isEmpty(), Modifier.weight(1f)) {
                    pinned.forEachIndexed { index, item ->
                        key(item.id) {
                            PinRow(item.title) {
                                PinIconButton(
                                    NilumioIcons.ChevronUp,
                                    R.string.nav_menu_move_up,
                                    requester(item.id, PinButton.UP),
                                    index > 0,
                                ) {
                                    val list = pinned.toMutableList().apply { add(index - 1, removeAt(index)) }
                                    commit(list, unpinned, item.id to if (index - 1 == 0) PinButton.DOWN else PinButton.UP)
                                }
                                PinIconButton(
                                    NilumioIcons.ChevronDown,
                                    R.string.nav_menu_move_down,
                                    requester(item.id, PinButton.DOWN),
                                    index < pinned.lastIndex,
                                ) {
                                    val list = pinned.toMutableList().apply { add(index + 1, removeAt(index)) }
                                    commit(list, unpinned, item.id to if (index + 1 == pinned.lastIndex) PinButton.UP else PinButton.DOWN)
                                }
                                PinIconButton(NilumioIcons.ArrowRight, R.string.nav_menu_remove, requester(item.id, PinButton.REMOVE)) {
                                    val list = pinned - item
                                    // Fokus bleibt links auf dem nachrückenden Eintrag
                                    val next = list.getOrNull(index.coerceAtMost(list.lastIndex))
                                    commit(list, unpinned + item, next?.let { it.id to PinButton.REMOVE } ?: (item.id to PinButton.ADD))
                                }
                            }
                        }
                    }
                }
                PinPanel(R.string.nav_menu_out, R.string.nav_menu_all_in, unpinned.isEmpty(), Modifier.weight(1f)) {
                    unpinned.forEachIndexed { index, item ->
                        key(item.id) {
                            PinRow(item.title, buttonsFirst = true) {
                                PinIconButton(NilumioIcons.ArrowLeft, R.string.nav_menu_add, requester(item.id, PinButton.ADD)) {
                                    val list = unpinned - item
                                    val next = list.getOrNull(index.coerceAtMost(list.lastIndex))
                                    commit(pinned + item, list, next?.let { it.id to PinButton.ADD } ?: (item.id to PinButton.REMOVE))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinPanel(
    @StringRes title: Int,
    @StringRes emptyText: Int,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            modifier
                .fillMaxHeight()
                .background(Nilumio.Panel, RoundedCornerShape(14.dp))
                .padding(12.dp),
    ) {
        NilumioSectionTitle(stringResource(title), Modifier.padding(start = 8.dp, bottom = 4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (isEmpty) {
                Text(stringResource(emptyText), fontSize = 15.sp, color = Nilumio.Muted, modifier = Modifier.padding(8.dp))
            }
            content()
        }
    }
}

@Composable
private fun PinRow(
    title: String,
    buttonsFirst: Boolean = false,
    buttons: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
    ) {
        if (buttonsFirst) buttons()
        Text(
            text = title,
            fontSize = 17.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        if (!buttonsFirst) buttons()
    }
}

/** Icon-Knopf; inaktive Knöpfe bleiben fokussierbar, damit die Spalten für das Steuerkreuz stabil bleiben. */
@Composable
private fun PinIconButton(
    icon: ImageVector,
    @StringRes description: Int,
    focusRequester: FocusRequester,
    active: Boolean = true,
    onClick: () -> Unit,
) = NilumioButton(
    onClick = { if (active) onClick() },
    shape = RoundedCornerShape(10.dp),
    containerColor = Nilumio.PanelFocused,
    focusedContainerColor = Nilumio.PanelHighlight,
    focusedScale = 1f,
    modifier = Modifier.size(40.dp).focusRequester(focusRequester),
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(description),
            tint = if (active) Color.White else Color.White.copy(alpha = .25f),
            modifier = Modifier.size(22.dp),
        )
    }
}
