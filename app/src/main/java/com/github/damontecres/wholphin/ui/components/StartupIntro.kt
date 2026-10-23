package com.github.damontecres.wholphin.ui.components

import android.net.Uri
import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.github.damontecres.wholphin.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Meldet, dass die erste Seite nach dem Start fertig geladen ist
 * (Startseite oder eine Anmeldeseite), damit die Start-Animation enden darf.
 */
object StartupSignal {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    fun markReady() {
        if (!_ready.value) Timber.i("Startup content ready")
        _ready.value = true
    }
}

/**
 * Spielt die nilumio-Animation, während die App im Hintergrund lädt.
 *
 * Das Video läuft auf einer eigenen SurfaceView-Ebene, die der Fernseher direkt aus dem
 * Hardware-Decoder ausgibt. So ruckelt es nicht, auch wenn die App gleichzeitig die Startseite
 * aufbaut. Am Ende übernimmt ein Standbild des Schlussbilds, das stehen bleibt, bis
 * [StartupSignal] die geladene Startseite meldet, und dann mit dem Overlay ausgeblendet wird.
 */
@Composable
fun StartupIntro(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ready by StartupSignal.ready.collectAsState()
    var videoDone by remember { mutableStateOf(false) }
    val player =
        remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(
                    MediaItem.fromUri(
                        Uri.parse("android.resource://${context.packageName}/${R.raw.nilumio_intro}"),
                    ),
                )
                prepare()
                playWhenReady = true
            }
        }
    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) videoDone = true
                }

                override fun onPlayerError(error: PlaybackException) {
                    Timber.w(error, "Startup intro failed")
                    videoDone = true
                }
            }
        player.addListener(listener)
        val analytics =
            object : AnalyticsListener {
                override fun onDroppedVideoFrames(
                    eventTime: AnalyticsListener.EventTime,
                    droppedFrames: Int,
                    elapsedMs: Long,
                ) {
                    Timber.i("Startup intro dropped %d frames in %d ms", droppedFrames, elapsedMs)
                }
            }
        player.addAnalyticsListener(analytics)
        onDispose {
            player.removeAnalyticsListener(analytics)
            player.removeListener(listener)
            player.release()
        }
    }
    // Sicherheitsnetz: nie länger als 15 s auf die Startseite warten
    LaunchedEffect(Unit) {
        delay(15.seconds)
        videoDone = true
        StartupSignal.markReady()
    }
    LaunchedEffect(videoDone) {
        if (videoDone) player.stop()
    }
    LaunchedEffect(videoDone, ready) {
        if (videoDone && ready) {
            // kurz atmen lassen, damit die Startseite schon gezeichnet ist
            delay(200.milliseconds)
            onFinished()
        }
    }
    Box(modifier = modifier.fillMaxSize().background(Nilumio.Night)) {
        if (!videoDone) {
            AndroidView(
                factory = { ctx -> SurfaceView(ctx).also { player.setVideoSurfaceView(it) } },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Standbild statt SurfaceView: lässt sich weich ausblenden
            Image(
                painter = painterResource(R.drawable.nilumio_intro_final),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
