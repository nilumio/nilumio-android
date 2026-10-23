@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.github.damontecres.wholphin.ui.playback.overlay

import android.view.Gravity
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.AppColors
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.ui.PreviewTvSpec
import com.github.damontecres.wholphin.ui.components.Button
import com.github.damontecres.wholphin.ui.components.Nilumio
import com.github.damontecres.wholphin.ui.components.SelectedLeadingContent
import com.github.damontecres.wholphin.ui.components.TextButton
import com.github.damontecres.wholphin.ui.formatDuration
import com.github.damontecres.wholphin.ui.indexOfFirstOrNull
import com.github.damontecres.wholphin.ui.playback.ControllerViewState
import com.github.damontecres.wholphin.ui.playback.PlaybackDialogType
import com.github.damontecres.wholphin.ui.seekBack
import com.github.damontecres.wholphin.ui.seekForward
import com.github.damontecres.wholphin.ui.skipStringRes
import com.github.damontecres.wholphin.ui.theme.LocalTheme
import com.github.damontecres.wholphin.ui.theme.WholphinTheme
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.util.ExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Possible actions the user can take during playback
 */
sealed interface PlaybackAction {
    data object ShowDebug : PlaybackAction

    data object ShowPlaylist : PlaybackAction

    data object ShowVideoFilterDialog : PlaybackAction

    data object SearchCaptions : PlaybackAction

    data class ToggleCaptions(
        val index: Int,
    ) : PlaybackAction

    data class ToggleAudio(
        val index: Int,
    ) : PlaybackAction

    data class PlaybackSpeed(
        val value: Float,
    ) : PlaybackAction

    data class Scale(
        val scale: ContentScale,
    ) : PlaybackAction

    data object Previous : PlaybackAction

    data object Next : PlaybackAction
}

/**
 * UI for actual playback controls such as seek bar, play/pause and seek buttons
 */
@OptIn(UnstableApi::class)
@Composable
fun PlaybackControls(
    player: Player,
    controllerViewState: ControllerViewState,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    onClickPlaybackDialogType: (PlaybackDialogType) -> Unit,
    onSeekProgress: (Long) -> Unit,
    showPlay: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    seekEnabled: Boolean,
    seekBarIntervals: Int,
    seekBack: Duration,
    skipBackOnResume: Duration?,
    seekForward: Duration,
    currentSegment: MediaSegmentDto?,
    modifier: Modifier = Modifier,
    initialFocusRequester: FocusRequester = remember { FocusRequester() },
    seekBarInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val scope = rememberCoroutineScope()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val onControllerInteraction = {
        scope.launch(ExceptionHandler()) {
            bringIntoViewRequester.bringIntoView()
        }
        controllerViewState.pulseControls()
    }
    LaunchedEffect(controllerViewState.controlsVisible) {
        if (controllerViewState.controlsVisible) {
            initialFocusRequester.tryRequestFocus()
        }
    }
    // nilumio „Lichtkante“ (Lichtkante.kt): Linie über volle Breite, darunter eine Zeile: Zeit, Wiedergabe, Spuren und Einstellungen
    Column(
        modifier = modifier.bringIntoViewRequester(bringIntoViewRequester),
        verticalArrangement = Arrangement.spacedBy(LichtkanteRowGap),
    ) {
        SeekBar(
            player = player,
            controllerViewState = controllerViewState,
            onSeekProgress = onSeekProgress,
            interactionSource = seekBarInteractionSource,
            isEnabled = seekEnabled,
            intervals = seekBarIntervals,
            seekBack = seekBack,
            seekForward = seekForward,
            modifier = Modifier.bleedHorizontally(LichtkanteEdge), // nilumio: randlos, der Rest behält den Rand
        )
        // nilumio: Zeit links, Play/Pause genau in der Bildmitte
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            PlayerTimecode(player, Modifier.align(Alignment.CenterStart))
            PlaybackButtons(
                player = player,
                initialFocusRequester = initialFocusRequester,
                onControllerInteraction = onControllerInteraction,
                onPlaybackActionClick = onPlaybackActionClick,
                showPlay = showPlay,
                previousEnabled = previousEnabled,
                nextEnabled = nextEnabled,
                seekBack = seekBack,
                seekForward = seekForward,
                skipBackOnResume = skipBackOnResume,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                currentSegment?.let { segment ->
                    TextButton(
                        stringRes = segment.type.skipStringRes,
                        onClick = {
                            player.seekTo(segment.endTicks.ticks.inWholeMilliseconds)
                        },
                        modifier = Modifier.padding(end = 24.dp),
                    )
                }
                RightPlaybackButtons(
                    onControllerInteraction = onControllerInteraction,
                    onClickPlaybackDialogType = onClickPlaybackDialogType,
                    modifier = Modifier.offset(x = LichtkanteIconInset),
                )
            }
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SeekBar(
    player: Player,
    isEnabled: Boolean,
    intervals: Int,
    controllerViewState: ControllerViewState,
    onSeekProgress: (Long) -> Unit,
    seekBack: Duration,
    seekForward: Duration,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    var bufferedProgress by remember(player) { mutableFloatStateOf(player.bufferedPosition.toFloat() / player.duration) }
    var progress by remember(player) { mutableFloatStateOf(player.currentPosition.toFloat() / player.duration) }
    LaunchedEffect(player) {
        while (isActive) {
            bufferedProgress = player.bufferedPosition.toFloat() / player.duration
            progress = player.currentPosition.toFloat() / player.duration
            delay(250L)
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IntervalSeekBarImpl(
            progress = progress,
            bufferedProgress = bufferedProgress,
            onSeek = {
                onSeekProgress(it)
            },
            controllerViewState = controllerViewState,
//            intervals = intervals,
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            enabled = isEnabled,
            durationMs = player.duration,
            seekBack = seekBack,
            seekForward = seekForward,
        )
    }
}

@Composable
fun SeekTimecodes(
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) = LichtkanteTimecode(positionMs, durationMs, modifier) // nilumio

val buttonSpacing = LichtkanteButtonSpacing // nilumio

@Composable
fun LeftPlaybackButtons(
    onControllerInteraction: () -> Unit,
    onClickPlaybackDialogType: (PlaybackDialogType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
    ) {
        PlaybackButton(
            iconRes = R.drawable.vector_settings,
            onClick = {
                onControllerInteraction.invoke()
                onClickPlaybackDialogType.invoke(PlaybackDialogType.SETTINGS)
            },
            enabled = true,
            onControllerInteraction = onControllerInteraction,
            modifier = Modifier,
        )
    }
}

@Composable
fun RightPlaybackButtons(
    onControllerInteraction: () -> Unit,
    onClickPlaybackDialogType: (PlaybackDialogType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
        verticalAlignment = Alignment.CenterVertically, // nilumio: alle Knöpfe auf einer Mittellinie
    ) {
        // Captions
        PlaybackButton(
            enabled = true,
            iconRes = R.drawable.nilumio_player_cc,
            onClick = {
                onControllerInteraction.invoke()
                onClickPlaybackDialogType.invoke(PlaybackDialogType.CAPTIONS)
            },
            onControllerInteraction = onControllerInteraction,
            modifier = Modifier,
        )
        // Audio
        PlaybackButton(
            enabled = true,
            iconRes = R.drawable.nilumio_player_audio,
            onClick = {
                onControllerInteraction.invoke()
                onClickPlaybackDialogType.invoke(PlaybackDialogType.AUDIO)
            },
            onControllerInteraction = onControllerInteraction,
            modifier = Modifier,
        )
        // nilumio: Einstellungen rechts außen statt links
        PlaybackButton(
            iconRes = R.drawable.nilumio_player_settings,
            onClick = {
                onControllerInteraction.invoke()
                onClickPlaybackDialogType.invoke(PlaybackDialogType.SETTINGS)
            },
            enabled = true,
            onControllerInteraction = onControllerInteraction,
            modifier = Modifier,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlaybackButtons(
    player: Player,
    initialFocusRequester: FocusRequester,
    onControllerInteraction: () -> Unit,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    showPlay: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    seekBack: Duration,
    skipBackOnResume: Duration?,
    seekForward: Duration,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
            verticalAlignment = Alignment.CenterVertically, // nilumio: Play/Pause mittig zu den anderen
        ) {
            PlaybackButton(
                iconRes = R.drawable.nilumio_player_prev,
                onClick = {
                    onControllerInteraction.invoke()
                    onPlaybackActionClick.invoke(PlaybackAction.Previous)
                },
                enabled = previousEnabled,
                onControllerInteraction = onControllerInteraction,
            )
            // nilumio: Knöpfe springen 30 s, Links/Rechts auf der Zeitleiste weiter um die Einstellung (10 s)
            PlaybackSkipButton(forward = false, player = player, onControllerInteraction = onControllerInteraction)
            PlaybackButton(
                modifier = Modifier.focusRequester(initialFocusRequester),
                iconRes = if (showPlay) R.drawable.nilumio_player_play else R.drawable.nilumio_player_pause,
                big = true,
                onClick = {
                    onControllerInteraction.invoke()
                    if (showPlay) {
                        player.play()
                        skipBackOnResume?.let {
                            player.seekBack(it)
                        }
                    } else {
                        player.pause()
                    }
                },
                onControllerInteraction = onControllerInteraction,
            )
            PlaybackSkipButton(forward = true, player = player, onControllerInteraction = onControllerInteraction)
            PlaybackButton(
                iconRes = R.drawable.nilumio_player_next,
                onClick = {
                    onControllerInteraction.invoke()
                    onPlaybackActionClick.invoke(PlaybackAction.Next)
                },
                enabled = nextEnabled,
                onControllerInteraction = onControllerInteraction,
            )
        }
    }
}

@Composable
fun PlaybackButton(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    onControllerInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    big: Boolean = false,
) = LichtkanteIconButton(iconRes, onClick, onControllerInteraction, modifier, enabled, interactionSource, big) // nilumio

@Composable
fun PlaybackFaButton(
    @StringRes iconRes: Int,
    onClick: () -> Unit,
    onControllerInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    textColor: Color = Color.Unspecified,
) {
    Button(
        enabled = enabled,
        onClick = onClick,
//        shape = ButtonDefaults.shape(CircleShape),
        // nilumio „Lichtkante“: Symbole ohne Fläche, ausgewählt hell mit Mint-Schein
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = Color.White.copy(alpha = .62f),
                focusedContainerColor = Color.White.copy(alpha = .1f),
                focusedContentColor = Color.White,
            ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(Nilumio.Mint.copy(alpha = .45f), 10.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
        contentPadding = PaddingValues(4.dp),
        interactionSource = interactionSource,
        modifier =
            modifier
                .size(36.dp, 36.dp)
                .onFocusChanged { onControllerInteraction.invoke() },
    ) {
        Text(
            text = stringResource(iconRes),
            fontSize = 18.sp,
            fontFamily = FontAwesome,
            textAlign = TextAlign.Center,
            color =
                if (textColor.isSpecified) textColor else LocalContentColor.current,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically),
        )
    }
}

/**
 * Show a dialog aligned to the bottom of the screen instead of centered.
 *
 * @param gravity The [Gravity] to align the dialog to left or right
 */
@Composable
fun <T> BottomDialog(
    choices: List<BottomDialogItem<T>>,
    onDismissRequest: () -> Unit,
    onSelectChoice: (Int, BottomDialogItem<T>) -> Unit,
    gravity: Int,
    currentChoice: BottomDialogItem<T>? = null,
) {
    val focusRequesters = remember(choices.size) { List(choices.size) { FocusRequester() } }
    if (currentChoice != null) {
        LaunchedEffect(Unit) {
            choices.indexOfFirstOrNull { it == currentChoice }?.let {
                focusRequesters.getOrNull(it)?.tryRequestFocus()
            }
        }
    }
    // TODO enforcing a width ends up ignore the gravity
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        dialogWindowProvider?.window?.let { window ->
            window.setGravity(Gravity.BOTTOM or gravity) // Move down, by default dialogs are in the centre
            window.setDimAmount(0f) // Remove dimmed background of ongoing playback
        }

        Box(
            modifier =
                Modifier
                    .wrapContentSize()
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(8.dp),
                    ),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
//                        .widthIn(max = 240.dp)
                        .wrapContentWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(choices) { index, choice ->
                    val interactionSource = remember { MutableInteractionSource() }
                    ListItem(
                        selected = choice == currentChoice,
                        enabled = choice.enabled,
                        onClick = {
                            onDismissRequest()
                            onSelectChoice(index, choice)
                        },
                        leadingContent = {
                            SelectedLeadingContent(choice == currentChoice)
                        },
                        headlineContent = {
                            Text(
                                text = choice.headline,
                            )
                        },
                        supportingContent = {
                            choice.supporting?.let {
                                Text(
                                    text = it,
                                )
                            }
                        },
                        interactionSource = interactionSource,
                        modifier = Modifier.focusRequester(focusRequesters[index]),
                    )
                }
            }
        }
    }
}

data class BottomDialogItem<T>(
    val data: T,
    val headline: String,
    val supporting: String?,
    val enabled: Boolean = true,
)

@PreviewTvSpec
@Composable
private fun ButtonPreview() {
    WholphinTheme {
        Row(Modifier.background(Color.Red)) {
            PlaybackButton(
                iconRes = R.drawable.baseline_play_arrow_24,
                onClick = {},
                onControllerInteraction = {},
            )
            PlaybackFaButton(
                iconRes = R.string.fa_volume_high,
                onClick = {},
                onControllerInteraction = {},
                textColor = Color.Green,
            )
        }
    }
}
