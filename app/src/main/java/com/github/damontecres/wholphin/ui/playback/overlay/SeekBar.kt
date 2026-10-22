package com.github.damontecres.wholphin.ui.playback.overlay

/*
 * Modified from https://github.com/android/tv-samples
 *
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.github.damontecres.wholphin.ui.components.NilumioColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.wholphin.ui.playback.ControllerViewState
import com.github.damontecres.wholphin.ui.playback.calculateSeekAccelerationMultiplier
import com.github.damontecres.wholphin.ui.playback.isDpadLeft
import com.github.damontecres.wholphin.ui.playback.isDpadRight
import kotlinx.coroutines.FlowPreview
import timber.log.Timber
import kotlin.time.Duration

/**
 * This is a seek bar which seeks by a percentage of the duration instead of a fixed amount of time
 *
 * For example, if [intervals] is 10, then each seek will be 10% of total media duration
 */
@Composable
fun SteppedSeekBarImpl(
    progress: Float,
    durationMs: Long,
    bufferedProgress: Float,
    onSeek: (Long) -> Unit,
    controllerViewState: ControllerViewState,
    modifier: Modifier = Modifier,
    intervals: Int = 10,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    var hasSeeked by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(progress) }
    val progressToUse = if (isFocused && hasSeeked) seekProgress else progress
    LaunchedEffect(isFocused) {
        if (!isFocused) hasSeeked = false
    }

    val offset = 1f / intervals

    val seek = { percent: Float ->
        onSeek((percent * durationMs).toLong())
    }

    SeekBarDisplay(
        enabled = enabled,
        progress = progressToUse,
        bufferedProgress = bufferedProgress,
        durationMs = durationMs,
        onLeft = { multiplier ->
            controllerViewState.pulseControls()
            seekProgress = (progressToUse - offset * multiplier).coerceAtLeast(0f)
            hasSeeked = true
            seek(seekProgress)
        },
        onRight = { multiplier ->
            controllerViewState.pulseControls()
            seekProgress = (progressToUse + offset * multiplier).coerceAtMost(1f)
            hasSeeked = true
            seek(seekProgress)
        },
        interactionSource = interactionSource,
        modifier = modifier,
    )
}

/**
 * A read-only seek bar for showing progress without taking focus or handling D-Pad input.
 *
 * This is used by overlays where seeking is handled elsewhere and the bar is only a visual indicator.
 */
@Composable
fun StaticSeekBarImpl(
    progress: Float,
    durationMs: Long,
    bufferedProgress: Float,
    modifier: Modifier = Modifier,
) {
    SeekBarDisplay(
        enabled = false,
        progress = progress,
        bufferedProgress = bufferedProgress,
        durationMs = durationMs,
        onLeft = {},
        onRight = {},
        interactionSource = remember { MutableInteractionSource() },
        modifier = modifier,
    )
}

/**
 * A seek (or scrubber) bar which seeks forward or back a fixed amount of time per move
 */
@OptIn(FlowPreview::class)
@Composable
fun IntervalSeekBarImpl(
    progress: Float,
    durationMs: Long,
    bufferedProgress: Float,
    onSeek: (Long) -> Unit,
    controllerViewState: ControllerViewState,
    seekBack: Duration,
    seekForward: Duration,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    var hasSeeked by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableLongStateOf((progress * durationMs).toLong()) }
//    val progressToUse by remember { derivedStateOf { if (isFocused && hasSeeked) seekPositionMs else (progress * durationMs).toLong() } }
    val progressToUse =
        if (isFocused && hasSeeked) seekPositionMs else (progress * durationMs).toLong()

    LaunchedEffect(isFocused) {
        if (!isFocused) hasSeeked = false
    }

    SeekBarDisplay(
        enabled = enabled,
        progress = (progressToUse.toDouble() / durationMs).toFloat(),
        bufferedProgress = bufferedProgress,
        durationMs = durationMs,
        onLeft = { multiplier ->
            controllerViewState.pulseControls()
            seekPositionMs =
                (progressToUse - seekBack.inWholeMilliseconds * multiplier).coerceAtLeast(0L)
            hasSeeked = true
            onSeek(seekPositionMs)
        },
        onRight = { multiplier ->
            controllerViewState.pulseControls()
            seekPositionMs =
                (progressToUse + seekForward.inWholeMilliseconds * multiplier)
                    .coerceAtMost(durationMs)
            hasSeeked = true
            onSeek(seekPositionMs)
        },
        interactionSource = interactionSource,
        modifier = modifier,
    )
}

/**
 * Actually renders the seek bar. It has callbacks for when the user moves to the left or right
 *
 * @see IntervalSeekBarImpl
 * @see SteppedSeekBarImpl
 */
@Composable
private fun SeekBarDisplay(
    progress: Float,
    bufferedProgress: Float,
    durationMs: Long,
    onLeft: (Int) -> Unit,
    onRight: (Int) -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface

    val isFocused by interactionSource.collectIsFocusedAsState()
    var leftHandledByRepeat by remember { mutableStateOf(false) }
    var rightHandledByRepeat by remember { mutableStateOf(false) }
    // nilumio „Lichtkante groß“: Linie 0,18 em × k, Kugel 1 em × k; mit Fokus etwas kräftiger
    val focusScale by animateFloatAsState(targetValue = if (isFocused) 1.4f else 1f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height((8f * LICHTKANTE_K).dp)
                    .onPreviewKeyEvent { event ->
                        if (!isDpadLeft(event) && !isDpadRight(event)) {
                            Timber.v("Ignoring %s", event)
                            return@onPreviewKeyEvent false
                        }
                        if (isDpadLeft(event)) {
                            when (event.type) {
                                KeyEventType.KeyDown -> {
                                    val repeatCount = event.nativeKeyEvent.repeatCount
                                    if (repeatCount > 0) {
                                        leftHandledByRepeat = true
                                        onLeft.invoke(
                                            calculateSeekAccelerationMultiplier(
                                                repeatCount = repeatCount,
                                                durationMs = durationMs,
                                            ),
                                        )
                                    } else {
                                        leftHandledByRepeat = false
                                    }
                                }

                                KeyEventType.KeyUp -> {
                                    if (!leftHandledByRepeat) {
                                        onLeft.invoke(1)
                                    }
                                    leftHandledByRepeat = false
                                }

                                else -> {
                                    return@onPreviewKeyEvent false
                                }
                            }
                            return@onPreviewKeyEvent true
                        } else {
                            when (event.type) {
                                KeyEventType.KeyDown -> {
                                    val repeatCount = event.nativeKeyEvent.repeatCount
                                    if (repeatCount > 0) {
                                        rightHandledByRepeat = true
                                        onRight.invoke(
                                            calculateSeekAccelerationMultiplier(
                                                repeatCount = repeatCount,
                                                durationMs = durationMs,
                                            ),
                                        )
                                    } else {
                                        rightHandledByRepeat = false
                                    }
                                }

                                KeyEventType.KeyUp -> {
                                    if (!rightHandledByRepeat) {
                                        onRight.invoke(1)
                                    }
                                    rightHandledByRepeat = false
                                }

                                else -> {
                                    return@onPreviewKeyEvent false
                                }
                            }
                            return@onPreviewKeyEvent true
                        }
                    }.focusable(enabled = enabled, interactionSource = interactionSource),
            onDraw = {
                // nilumio „Lichtkante“: dünne Linie, Fortschritt im Logo-Verlauf mit Schein, Position als Kugel
                val yOffset = size.height.div(2)
                val line = (1.44f * LICHTKANTE_K).dp.toPx() * focusScale
                val end = size.width.times(progress)
                // Verlauf über den gespielten Teil wie in der Vorlage, Schein weich in drei Lagen
                val logo = Brush.horizontalGradient(listOf(NilumioColors.Mint, NilumioColors.Sky, NilumioColors.Lilac), endX = end.coerceAtLeast(1f))
                drawLine(onSurface.copy(alpha = 0.18f), Offset(0f, yOffset), Offset(size.width, yOffset), line, StrokeCap.Round)
                drawLine(onSurface.copy(alpha = 0.1f), Offset(0f, yOffset), Offset(size.width.times(bufferedProgress), yOffset), line, StrokeCap.Round)
                for ((w, a) in listOf(12f to .07f, 7.5f to .12f, 5f to .2f)) {
                    drawLine(NilumioColors.Sky, Offset(0f, yOffset), Offset(end, yOffset), w.dp.toPx(), StrokeCap.Round, alpha = a)
                }
                drawLine(logo, Offset(0f, yOffset), Offset(end, yOffset), line, StrokeCap.Round)
                val r = (4f * LICHTKANTE_K).dp.toPx() * focusScale
                val c = Offset(end, yOffset)
                drawCircle(Brush.radialGradient(listOf(NilumioColors.Sky.copy(alpha = .6f), Color.Transparent), c, r + 12.8.dp.toPx()), r + 12.8.dp.toPx(), c)
                drawCircle(
                    Brush.radialGradient(
                        0f to NilumioColors.Mint,
                        .45f to NilumioColors.Sky,
                        1f to NilumioColors.Lilac,
                        center = Offset(c.x - r * .2f, c.y - r * .3f),
                        radius = r * 1.77f,
                    ),
                    r,
                    c,
                )
            },
        )
    }
}
