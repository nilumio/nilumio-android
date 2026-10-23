/*
 * nilumio – Seite „SyncPlay“, Copyright (C) 2026 nilumio, GPL-2.0.
 * Aufbau angelehnt an die Gruppenauswahl von jellyfin-web (src/plugins/syncPlay/ui/groupSelectionMenu.js, GPL-2.0).
 * Design „Lounge“: große Karte zum Erstellen, offene Gruppen als Karten mit Gesichtern.
 */
package com.github.damontecres.wholphin.ui.syncplay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.services.syncplay.SyncPlayViewModel
import com.github.damontecres.wholphin.ui.components.Nilumio
import com.github.damontecres.wholphin.ui.components.NilumioButton
import com.github.damontecres.wholphin.ui.components.NilumioIcons
import com.github.damontecres.wholphin.ui.components.NilumioMark
import com.github.damontecres.wholphin.ui.components.NilumioPageHeader
import com.github.damontecres.wholphin.ui.components.NilumioSectionTitle
import com.github.damontecres.wholphin.ui.tryRequestFocus
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupStateType
import kotlin.time.Duration.Companion.seconds

/** So oft wird die Gruppenliste neu geladen, solange die Seite offen ist. */
private val GroupsRefreshInterval = 5.seconds

/** Höchstens so viele Gesichter je Gruppenkarte. */
private const val MAX_FACES = 4

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
        while (true) {
            manager.refreshGroups()
            delay(GroupsRefreshInterval)
        }
    }
    val current = group
    // Die Seite bleibt beim Erstellen offen: links wird die Karte zur Lobby, rechts steht die eigene Lobby in der Liste
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.verticalScroll(rememberScrollState()).padding(top = 40.dp, end = 48.dp, bottom = 32.dp),
    ) {
        NilumioPageHeader(R.string.syncplay_title, R.string.syncplay_subtitle)
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp), modifier = Modifier.fillMaxWidth()) {
            LobbyCard(
                group = current,
                onCreate = manager::createGroup,
                onChoose = viewModel.navigationManager::goToHome,
                onLeave = manager::leaveGroup,
                focusRequester = focusRequester,
                modifier = Modifier.weight(1.15f),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NilumioSectionTitle(stringResource(R.string.syncplay_join))
                    LiveLabel(stringResource(R.string.syncplay_live))
                }
                if (groups.isEmpty()) {
                    Text(stringResource(R.string.syncplay_no_groups), color = Nilumio.Muted, fontSize = 16.sp)
                }
                // eigene Lobby zuerst und hervorgehoben
                groups.sortedByDescending { it.groupId == current?.groupId }.forEach {
                    key(it.groupId) {
                        val mine = it.groupId == current?.groupId
                        GroupCard(it, mine, Modifier.animateContentSize()) {
                            if (mine) viewModel.navigationManager.goToHome() else manager.joinGroup(it.groupId)
                        }
                    }
                }
            }
        }
    }
}

/** Linke Karte: „Neue Lobby erstellen“; nach dem Erstellen verschwindet der Inhalt ins Logo und die Lobby erscheint. */
@Composable
private fun LobbyCard(
    group: GroupInfoDto?,
    onCreate: () -> Unit,
    onChoose: () -> Unit,
    onLeave: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    // Logo „schluckt“ beim Wechsel kurz auf
    val gulp = remember { Animatable(1f) }
    LaunchedEffect(group != null) {
        if (group != null) {
            gulp.animateTo(1.5f, tween(260))
            gulp.animateTo(1f, spring(dampingRatio = .45f))
        }
    }
    Box(
        modifier
            .heightIn(min = 300.dp)
            .background(Nilumio.Card, shape)
            .background(Brush.radialGradient(listOf(Nilumio.CardGlowStart.copy(alpha = .16f), Color.Transparent), Offset.Zero, 700f), shape)
            .background(
                Brush.radialGradient(listOf(Nilumio.CardGlowEnd.copy(alpha = .18f), Color.Transparent), Offset(1400f, 900f), 900f),
                shape,
            ).padding(28.dp),
    ) {
        NilumioMark(
            modifier =
                Modifier.size(56.dp).graphicsLayer {
                    scaleX = gulp.value
                    scaleY = gulp.value
                },
        )
        AnimatedContent(
            targetState = group,
            contentKey = { it != null },
            transitionSpec = {
                // Inhalt wird ins Logo oben links gesaugt, die Lobby blendet danach auf
                fadeIn(tween(450, delayMillis = 450)).togetherWith(
                    scaleOut(tween(550, easing = FastOutLinearInEasing), targetScale = .05f, transformOrigin = TransformOrigin(0f, 0f)) +
                        fadeOut(tween(550)),
                )
            },
            label = "lobby",
        ) { g ->
            if (g == null) {
                // Nur der Titel ist der Knopf, die Erklärung steht darunter
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 72.dp)) {
                    CardButton(onCreate, Modifier.focusRequester(focusRequester)) {
                        Text(
                            stringResource(R.string.syncplay_create),
                            fontSize = 24.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                    Text(stringResource(R.string.syncplay_create_hint), fontSize = 16.sp, color = Nilumio.CardMuted)
                }
            } else {
                Lobby(g, onChoose, onLeave, focusRequester)
            }
        }
    }
}

@Composable
private fun Lobby(
    group: GroupInfoDto,
    onChoose: () -> Unit,
    onLeave: () -> Unit,
    focusRequester: FocusRequester,
) = Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(start = 76.dp)) {
    LiveLabel(stringResource(R.string.syncplay_lobby_open))
    Text(group.groupName, fontSize = 28.sp, color = Color.White)
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        group.participants.forEach { name ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Avatar(name, 50.dp)
                Text(name, color = Color.White, fontSize = 14.sp)
            }
        }
    }
    // Knöpfe untereinander, gleich breit
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.width(IntrinsicSize.Max)) {
        CardButton(onChoose, Modifier.fillMaxWidth().focusRequester(focusRequester)) {
            ButtonLabel(NilumioIcons.NowPlaying, stringResource(R.string.syncplay_choose))
        }
        CardButton(onLeave, Modifier.fillMaxWidth()) { ButtonLabel(NilumioIcons.Leave, stringResource(R.string.syncplay_leave)) }
    }
    val alone = group.participants.size <= 1
    Text(
        stringResource(if (alone) R.string.syncplay_lobby_hint else R.string.syncplay_hint),
        color = Nilumio.CardMuted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

@Composable
private fun ButtonLabel(
    icon: ImageVector,
    text: String,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
) {
    Icon(icon, contentDescription = null, tint = LocalContentColor.current, modifier = Modifier.size(22.dp))
    Text(text, fontSize = 17.sp)
}

/** Knöpfe im Farbton der Karte: Ton in Ton, ausgewählt heller mit Ring. */
@Composable
private fun CardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    plain: Boolean = false,
    content: @Composable () -> Unit,
) = NilumioButton(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    containerColor = if (plain) Color.Transparent else Nilumio.CardRaised,
    focusedContainerColor = Nilumio.CardFocused,
    ring = BorderStroke(3.dp, Nilumio.CardRing),
    focusedScale = 1.03f,
    modifier = modifier,
) { content() }

@Composable
private fun GroupCard(
    group: GroupInfoDto,
    mine: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) = NilumioButton(
    onClick = onClick,
    containerColor = if (mine) Nilumio.CardRaised else Nilumio.Panel,
    focusedContainerColor = if (mine) Nilumio.CardFocused else Nilumio.PanelFocused,
    ring = BorderStroke(3.dp, Nilumio.FocusRing),
    modifier = modifier.fillMaxWidth(),
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        Column(Modifier.weight(1f)) {
            Text(group.groupName, fontSize = 19.sp, color = Color.White)
            val state =
                when {
                    mine -> R.string.syncplay_state_mine
                    group.state == GroupStateType.PLAYING -> R.string.syncplay_state_playing
                    else -> R.string.syncplay_state_waiting
                }
            Text(stringResource(state), fontSize = 14.sp, color = Nilumio.Muted)
        }
        Row {
            group.participants.take(MAX_FACES).forEachIndexed { i, name ->
                Avatar(name, 34.dp, Modifier.offset(x = (-10 * i).dp))
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
    val color = Nilumio.avatar(name)
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size).background(color, CircleShape)) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            color = Nilumio.onAvatar(color),
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * .4f).sp,
        )
    }
}

@Composable
private fun LiveLabel(text: String) =
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(Nilumio.Mint, CircleShape))
        Text(text, color = Nilumio.Mint, fontSize = 14.sp)
    }
