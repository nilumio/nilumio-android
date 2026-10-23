/*
 * nilumio – SyncPlay, Copyright (C) 2026 nilumio, GPL-2.0.
 *
 * Befehlsplanung (scheduleUnpause/schedulePause/scheduleSeek, Bereit-Meldung nach Laden/Seek/Puffern)
 * portiert aus jellyfin-web src/plugins/syncPlay/core/PlaybackCore.js und QueueCore.js
 * (https://github.com/jellyfin/jellyfin-web, GPL-2.0), siehe THIRD_PARTY_NOTICES.md.
 */
package com.github.damontecres.wholphin.services.syncplay

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.damontecres.wholphin.services.NavigationManager
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SendCommandType
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/** Abweichung, ab der vor dem Abspielen bzw. nach dem Pausieren auf die Gruppenposition gesprungen wird. */
private const val UNPAUSE_TOLERANCE_MS = 400L
private const val PAUSE_TOLERANCE_MS = 50L

/** So lange nach einem Serverbefehl gilt Puffern als Folge des Befehls, nicht als Stocken. */
private const val BUFFERING_GRACE_MS = 2_000L

/** Nach einem Server-Seek „bereit“ melden, falls der Player gar nicht erst puffert. */
private const val SEEK_READY_FALLBACK_MS = 300L

@HiltViewModel
class SyncPlayViewModel
    @Inject
    constructor(
        val manager: SyncPlayManager,
        val navigationManager: NavigationManager,
    ) : ViewModel()

/**
 * Liefert in einer SyncPlay-Gruppe einen [SyncPlayPlayer] um [player], sonst [player] unverändert.
 */
@Composable
fun rememberSyncPlayPlayer(player: Player): Player {
    val manager = hiltViewModel<SyncPlayViewModel>().manager
    val group by manager.group.collectAsState()
    if (group == null) return player
    val syncPlayer = remember(player) { SyncPlayPlayer(player, manager) }
    DisposableEffect(syncPlayer) { onDispose { syncPlayer.dispose() } }
    return syncPlayer
}

/**
 * Hülle um den echten Player: Play/Pause/Spulen der Bedienung gehen als Anfrage an den Server,
 * ausgeführt wird erst der Befehl, den der Server an alle Gruppenmitglieder schickt.
 */
@OptIn(UnstableApi::class)
class SyncPlayPlayer(
    private val raw: Player,
    private val manager: SyncPlayManager,
) : ForwardingSimpleBasePlayer(raw),
    Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var commandJob: Job? = null
    private var requestedItem: UUID? = null

    /** Wir müssen dem Server noch „bereit“ melden (nach Laden, Server-Seek oder Puffern). */
    private var pendingReady = true
    private var pauseWhenReady = true
    private var ignoreBufferingUntil = 0L

    init {
        raw.addListener(this)
        scope.launch { manager.playQueue.collect { onQueueOrItemChanged() } }
        scope.launch { manager.lastCommand.filterNotNull().collect { apply(it) } }
    }

    fun dispose() {
        raw.removeListener(this)
        scope.cancel()
    }

    // Eigene Bedienung → Anfrage an den Server
    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) manager.requestUnpause() else manager.requestPause()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> {
        if (mediaItemIndex != raw.currentMediaItemIndex && mediaItemIndex != C.INDEX_UNSET) {
            return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
        }
        manager.requestSeek(positionMs.coerceAtLeast(0))
        return Futures.immediateVoidFuture()
    }

    // Zustand des echten Players → Meldungen an den Server
    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) = onQueueOrItemChanged()

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            sendReadyIfPending()
        } else if (playbackState == Player.STATE_BUFFERING && !pendingReady && System.currentTimeMillis() > ignoreBufferingUntil) {
            pendingReady = true
            pauseWhenReady = false
            manager.sendReady(false, raw.currentPosition, raw.isPlaying)
        }
    }

    /** Läuft hier das Gruppenobjekt: bereit melden. Läuft etwas anderes: es für die Gruppe starten. */
    private fun onQueueOrItemChanged() {
        val itemId = raw.currentMediaItem?.mediaId?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: return
        if (manager.currentQueueItem()?.itemId == itemId) {
            pendingReady = true
            pauseWhenReady = true
            sendReadyIfPending()
        } else if (requestedItem != itemId) {
            requestedItem = itemId
            manager.setNewQueue(itemId, raw.currentPosition)
        }
    }

    private fun sendReadyIfPending() {
        if (!pendingReady || raw.playbackState != Player.STATE_READY) return
        if (pauseWhenReady) raw.pause()
        pendingReady = false
        manager.sendReady(true, raw.currentPosition, raw.isPlaying)
    }

    /** Serverbefehl zum vorgegebenen Zeitpunkt ausführen (wie PlaybackCore.applyCommand in jellyfin-web). */
    private fun apply(command: SendCommand) {
        if (command.playlistItemId != manager.currentQueueItem()?.playlistItemId) return
        commandJob?.cancel()
        commandJob =
            scope.launch {
                val waitMs = (manager.serverToLocalMs(command.`when`) - System.currentTimeMillis()).coerceAtLeast(0)
                val positionMs = (command.positionTicks ?: 0) / TICKS_PER_MS
                ignoreBufferingUntil = System.currentTimeMillis() + waitMs + BUFFERING_GRACE_MS
                when (command.command) {
                    SendCommandType.UNPAUSE -> {
                        pendingReady = false
                        if (waitMs > 0) {
                            seekIfOff(positionMs, UNPAUSE_TOLERANCE_MS)
                            delay(waitMs)
                        } else {
                            seekIfOff(manager.estimatePositionMs(command.positionTicks ?: 0, command.`when`), UNPAUSE_TOLERANCE_MS)
                        }
                        raw.play()
                    }

                    SendCommandType.PAUSE -> {
                        pendingReady = false
                        delay(waitMs)
                        raw.pause()
                        seekIfOff(positionMs, PAUSE_TOLERANCE_MS)
                    }

                    SendCommandType.SEEK -> {
                        delay(waitMs)
                        raw.pause()
                        raw.seekTo(positionMs)
                        pendingReady = true
                        pauseWhenReady = true
                        delay(SEEK_READY_FALLBACK_MS)
                        sendReadyIfPending()
                    }

                    SendCommandType.STOP -> {
                        delay(waitMs)
                        raw.pause()
                    }
                }
            }
    }

    private fun seekIfOff(
        positionMs: Long,
        toleranceMs: Long,
    ) {
        if (abs(raw.currentPosition - positionMs) > toleranceMs) raw.seekTo(positionMs.coerceAtLeast(0))
    }
}
