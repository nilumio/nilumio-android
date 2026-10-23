package com.github.damontecres.wholphin.ui.playback.overlay

import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import com.github.damontecres.wholphin.ui.components.NilumioOutfit
import com.github.damontecres.wholphin.ui.formatDuration
import com.github.damontecres.wholphin.ui.joinNotBlank
import com.github.damontecres.wholphin.ui.seasonEpisodePadded
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration.Companion.minutes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.formatTime
import com.github.damontecres.wholphin.ui.playback.ControllerViewState
import com.github.damontecres.wholphin.ui.playback.PlaybackDialogType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jellyfin.sdk.model.api.MediaSegmentDto
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlaybackController(
    item: BaseItem?,
    nextState: OverlayViewState?,
    player: Player,
    controllerViewState: ControllerViewState,
    showPlay: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    seekEnabled: Boolean,
    seekBack: Duration,
    skipBackOnResume: Duration?,
    seekForward: Duration,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    onClickPlaybackDialogType: (PlaybackDialogType) -> Unit,
    onSeekBarChange: (Long) -> Unit,
    currentSegment: MediaSegmentDto?,
    onChangeState: (OverlayViewState) -> Unit,
    modifier: Modifier = Modifier,
    seekBarInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    // nilumio „Lichtkante“: Inhalt 40 dp vom Rand, Metazeile statt Wholphin-Untertitel
    val resources = LocalResources.current
    val meta = remember(item) { item?.let { playerMeta(it, resources) } }
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier.padding(horizontal = LichtkanteEdge),
    ) {
        Controller(
            title = item?.title,
            subtitle = meta,
            player = player,
            controllerViewState = controllerViewState,
            showPlay = showPlay,
            previousEnabled = previousEnabled,
            nextEnabled = nextEnabled,
            seekEnabled = seekEnabled,
            seekBack = seekBack,
            skipBackOnResume = skipBackOnResume,
            seekForward = seekForward,
            onPlaybackActionClick = onPlaybackActionClick,
            onClickPlaybackDialogType = onClickPlaybackDialogType,
            onSeekProgress = onSeekBarChange,
            seekBarInteractionSource = seekBarInteractionSource,
            currentSegment = currentSegment,
            modifier = Modifier,
        )
        when (nextState) {
            OverlayViewState.CHAPTERS -> {
                // nilumio: kein sichtbarer Hinweis, nur Fokusziel für ↓
                Spacer(
                    modifier =
                        Modifier
                            .size(1.dp)
                            .onFocusChanged {
                                if (it.isFocused) onChangeState.invoke(nextState)
                            }.focusable(),
                )
            }

            OverlayViewState.QUEUE -> {
                // nilumio: kein sichtbarer Hinweis, nur Fokusziel für ↓
                Spacer(
                    modifier =
                        Modifier
                            .size(1.dp)
                            .onFocusChanged {
                                if (it.isFocused) onChangeState.invoke(nextState)
                            }.focusable(),
                )
            }

            else -> {
                Unit
            }
        }
    }
}

// nilumio: Größen der Vorlage „Lichtkante groß“ (Titel 2,6 em × k, Metazeile 1 em × k)
internal val titleTextSize = (20.8f * LICHTKANTE_K).sp
internal val subtitleTextSize = (9.6f * LICHTKANTE_K).sp

/**
 * A wrapper for the playback controls to show title and other information, plus the actual controls
 *
 * @see PlaybackControls
 */
@Composable
fun Controller(
    title: String?,
    player: Player,
    controllerViewState: ControllerViewState,
    showPlay: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    seekEnabled: Boolean,
    seekBack: Duration,
    skipBackOnResume: Duration?,
    seekForward: Duration,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    onClickPlaybackDialogType: (PlaybackDialogType) -> Unit,
    onSeekProgress: (Long) -> Unit,
    currentSegment: MediaSegmentDto?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    seekBarInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val seekBarFocused by seekBarInteractionSource.collectIsFocusedAsState()
    val verticalOffset by animateDpAsState(
        targetValue = if (seekBarFocused) (-32).dp else 0.dp,
        label = "TitleBumpOffset",
        animationSpec =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = Dp.VisibilityThreshold,
            ),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy((20f + 6.48f * LICHTKANTE_K).dp),
        modifier = modifier,
    ) {
        // nilumio „Lichtkante“: Titel groß und leicht, darunter gedämpfte Metazeile (ohne Endzeit)
        Column(
            modifier = Modifier.offset(y = verticalOffset),
        ) {
            title?.let {
                Text(
                    text = it,
                    color = Color(0xFFEEF2F8),
                    fontFamily = NilumioOutfit,
                    fontWeight = FontWeight.Light,
                    fontSize = titleTextSize,
                    lineHeight = titleTextSize * 1.05f,
                    letterSpacing = titleTextSize * .005f,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.fillMaxWidth(.75f),
                )
            }
            if (subtitle != null) Text(
                text = subtitle,
                color = LichtkanteMuted,
                fontFamily = NilumioOutfit,
                fontSize = subtitleTextSize,
                lineHeight = subtitleTextSize * 1.45f,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = (.75f * LICHTKANTE_K).dp).fillMaxWidth(.75f),
            )
        }
        PlaybackControls(
            modifier = Modifier.fillMaxWidth(),
            player = player,
            onPlaybackActionClick = onPlaybackActionClick,
            controllerViewState = controllerViewState,
            onSeekProgress = {
                onSeekProgress(it)
            },
            showPlay = showPlay,
            previousEnabled = previousEnabled,
            nextEnabled = nextEnabled,
            seekEnabled = seekEnabled,
            seekBarInteractionSource = seekBarInteractionSource,
            seekBarIntervals = 16,
            seekBack = seekBack,
            seekForward = seekForward,
            skipBackOnResume = skipBackOnResume,
            currentSegment = currentSegment,
            onClickPlaybackDialogType = onClickPlaybackDialogType,
        )
    }
}

/** nilumio: Serie „S08F04 · Folgentitel“, sonst „Jahr · Genre · Dauer“. */
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
