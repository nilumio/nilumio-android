/*
 * nilumio – Seite „Watch Party“ (SyncPlay), Copyright (C) 2026 nilumio, GPL-2.0.
 * Aufbau angelehnt an die Gruppenauswahl von jellyfin-web (src/plugins/syncPlay/ui/groupSelectionMenu.js, GPL-2.0).
 * Design „Lounge“: große Karte zum Erstellen, offene Gruppen als Karten mit Gesichtern.
 */
package com.github.damontecres.wholphin.ui.syncplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.services.syncplay.SyncPlayViewModel
import com.github.damontecres.wholphin.ui.components.NilumioColors
import com.github.damontecres.wholphin.ui.components.NilumioMark
import com.github.damontecres.wholphin.ui.tryRequestFocus
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupStateType
import kotlin.time.Duration.Companion.seconds

private val Panel = Color(0xFF111726)
private val Muted = Color(0xFF8E9AB0)
private val FocusRing = Color(0xFF22ABD8).copy(alpha = .62f)

@Composable
fun SyncPlayPage(
    modifier: Modifier = Modifier,
    viewModel: SyncPlayViewModel = hiltViewModel(),
) {
    val manager = viewModel.manager
    val group by manager.group.collectAsState()
    val groups by manager.groups.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(group) {
        focusRequester.tryRequestFocus()
        // Gruppenliste laufend aktuell halten, solange die Seite offen ist
        while (true) {
            manager.refreshGroups()
            delay(5.seconds)
        }
    }
    val current = group
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.padding(top = 40.dp, end = 48.dp),
    ) {
        if (current != null) {
            item { LiveLabel(stringResource(R.string.syncplay_active)) }
            item { Text(current.groupName, fontSize = 34.sp, color = Color.White) }
            item { Text(stringResource(R.string.syncplay_hint), color = Muted, fontSize = 16.sp) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                    current.participants.forEachIndexed { i, name ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Avatar(name, 72.dp)
                            Text(name, color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PillButton(stringResource(R.string.syncplay_choose), { viewModel.navigationManager.goToHome() }, Modifier.focusRequester(focusRequester), primary = true)
                    PillButton(stringResource(R.string.syncplay_leave), { manager.leaveGroup() })
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.syncplay_title), fontSize = 34.sp, color = Color.White)
                    Text(stringResource(R.string.syncplay_subtitle), color = Muted, fontSize = 16.sp)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(36.dp), modifier = Modifier.fillMaxWidth()) {
                    CreateCard({ manager.createGroup() }, Modifier.weight(1.05f).focusRequester(focusRequester))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.syncplay_join).uppercase(), color = Muted, fontSize = 13.sp, letterSpacing = 1.sp)
                            LiveLabel(stringResource(R.string.syncplay_live))
                        }
                        if (groups.isEmpty()) {
                            Text(stringResource(R.string.syncplay_no_groups), color = Muted, fontSize = 16.sp)
                        }
                        groups.forEach { GroupCard(it) { manager.joinGroup(it.groupId) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Panel, focusedContainerColor = Panel),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, FocusRing), inset = 0.dp, shape = shape)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .background(Brush.radialGradient(listOf(NilumioColors.Mint.copy(alpha = .16f), Color.Transparent), radius = 700f))
                .background(Brush.linearGradient(listOf(Color.Transparent, NilumioColors.Lilac.copy(alpha = .22f))))
                .padding(28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NilumioMark(modifier = Modifier.size(56.dp))
                Text(stringResource(R.string.syncplay_create), fontSize = 26.sp, color = Color.White)
                Text(stringResource(R.string.syncplay_create_hint), fontSize = 15.sp, color = Muted)
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: GroupInfoDto,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Panel, focusedContainerColor = Color(0xFF182033)),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, FocusRing), inset = 0.dp, shape = shape)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Column(Modifier.weight(1f)) {
                Text(group.groupName, fontSize = 19.sp, color = Color.White)
                Text(
                    stringResource(if (group.state == GroupStateType.PLAYING) R.string.syncplay_state_playing else R.string.syncplay_state_waiting),
                    fontSize = 14.sp,
                    color = Muted,
                )
            }
            Row {
                group.participants.take(4).forEachIndexed { i, name ->
                    Avatar(name, 34.dp, Modifier.offset(x = (-10 * i).dp))
                }
            }
        }
    }
}

@Composable
private fun Avatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val color = NilumioColors.avatar(name)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).background(color, CircleShape),
    ) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            color = NilumioColors.onAvatar(color),
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * .4f).sp,
        )
    }
}

@Composable
private fun LiveLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(NilumioColors.Mint, CircleShape))
        Text(text, color = NilumioColors.Mint, fontSize = 14.sp)
    }
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (primary) MaterialTheme.colorScheme.primary.copy(alpha = .9f) else Color(0xFF182033),
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = NilumioColors.Night,
            ),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, FocusRing), inset = 0.dp, shape = shape)),
        modifier = modifier,
    ) {
        Text(text, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
    }
}
