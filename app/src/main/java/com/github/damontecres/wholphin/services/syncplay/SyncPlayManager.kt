/*
 * nilumio – SyncPlay, Copyright (C) 2026 nilumio, GPL-2.0.
 *
 * Herkunft (siehe THIRD_PARTY_NOTICES.md):
 * - Protokollablauf, Zeitabgleich (TimeSync: Messung mit kleinster Verzögerung aus den letzten 8,
 *   3 schnelle Pings, danach 60 s), Positionsschätzung und Startposition portiert aus jellyfin-web
 *   (src/plugins/syncPlay/core/Manager.js, QueueCore.js, PlaybackCore.js, timeSync/TimeSync.js,
 *   https://github.com/jellyfin/jellyfin-web, GPL-2.0).
 * - Grundgerüst (Singleton mit Gruppenliste, erstellen/beitreten/verlassen) angelehnt an
 *   services/syncplay/SyncPlayManager.kt aus Wholphin-Enhanced
 *   (https://github.com/AylaTheTanuki/Wholphin-Enhanced, GPL-2.0).
 */
package com.github.damontecres.wholphin.services.syncplay

import android.content.Context
import androidx.annotation.StringRes
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.showToast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.extensions.syncPlayApi
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.timeSyncApi
import org.jellyfin.sdk.api.client.util.ApiSerializer
import org.jellyfin.sdk.api.sockets.SocketConnection
import org.jellyfin.sdk.api.sockets.SocketConnectionFactory
import org.jellyfin.sdk.api.sockets.SocketConnectionState
import org.jellyfin.sdk.api.sockets.subscribe
import org.jellyfin.sdk.model.api.BufferRequestDto
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupUpdateType
import org.jellyfin.sdk.model.api.JoinGroupRequestDto
import org.jellyfin.sdk.model.api.NewGroupRequestDto
import org.jellyfin.sdk.model.api.PingRequestDto
import org.jellyfin.sdk.model.api.PlayQueueUpdate
import org.jellyfin.sdk.model.api.PlayQueueUpdateReason
import org.jellyfin.sdk.model.api.PlayRequestDto
import org.jellyfin.sdk.model.api.ReadyRequestDto
import org.jellyfin.sdk.model.api.SeekRequestDto
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SendCommandType
import org.jellyfin.sdk.model.api.SyncPlayCommandMessage
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal const val TICKS_PER_MS = 10_000L

/** Zeitabgleich: Messungen, aus denen die mit kleinster Verzögerung zählt; erst schnelle Pings, dann selten. */
private const val TIME_SAMPLES = 8
private const val FAST_PINGS = 3
private const val FAST_PING_MS = 1_000L
private const val SLOW_PING_MS = 60_000L

/** Nach Erstellen/Beitreten so oft auf die eigene Gruppe warten (siehe adoptGroup). */
private const val ADOPT_ATTEMPTS = 6
private const val ADOPT_RETRY_MS = 500L

/** Warteschlangen-Änderungen, die nichts neu starten. */
private val QueueOnlyReasons = setOf(PlayQueueUpdateReason.MOVE_ITEM, PlayQueueUpdateReason.QUEUE, PlayQueueUpdateReason.QUEUE_NEXT)

/**
 * Verwaltet die SyncPlay-Gruppe: Gruppenliste, beitreten/verlassen, Zeitabgleich mit dem Server,
 * Warteschlange der Gruppe (startet die Wiedergabe, wenn jemand anderes etwas startet) und die
 * Wiedergabebefehle des Servers. Den Player steuert [SyncPlayPlayer].
 */
@Singleton
class SyncPlayManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val api: ApiClient,
        private val navigationManager: NavigationManager,
        private val serverRepository: ServerRepository,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val groups = MutableStateFlow<List<GroupInfoDto>>(emptyList())

        /** Gruppe, in der wir gerade sind (null = keine). */
        val group = MutableStateFlow<GroupInfoDto?>(null)

        /** Aktuelle Warteschlange der Gruppe. */
        val playQueue = MutableStateFlow<PlayQueueUpdate?>(null)

        /** Letzter Wiedergabebefehl des Servers (Unpause/Pause/Seek/Stop). */
        val lastCommand = MutableStateFlow<SendCommand?>(null)

        /** Serverzeit minus lokale Zeit in ms. */
        private var timeOffsetMs = 0L
        private val samples = ArrayDeque<TimeSample>()
        private var groupJob: Job? = null

        init {
            scope.launch { SyncPlaySocketTap.groupUpdates.collect { onGroupUpdate(it) } }
        }

        fun refreshGroups() =
            scope.launch {
                listen() // WebSocket schon verbinden, damit „GroupJoined“ nach dem Erstellen ankommt
                runCatching { groups.value = api.syncPlayApi.syncPlayGetGroups().content }
                    .onFailure { Timber.w(it, "SyncPlay: Gruppen laden fehlgeschlagen") }
            }

        fun createGroup() =
            request(R.string.syncplay_error) {
                listen()
                val user = serverRepository.currentUser?.name ?: "nilumio"
                val owner = lobbyOwner(user.replaceFirstChar { it.uppercase() }, Locale.getDefault().language)
                val groupName = context.getString(R.string.syncplay_group_of, owner)
                api.syncPlayApi.syncPlayCreateGroup(NewGroupRequestDto(groupName))
                // Teilnehmer heißen wie das Konto (Groß-/Kleinschreibung egal), der Lobby-Name ist großgeschrieben
                adoptGroup { g -> g.groupName == groupName && g.participants.any { it.equals(user, ignoreCase = true) } }
            }

        fun joinGroup(groupId: UUID) =
            request(R.string.syncplay_error) {
                listen()
                api.syncPlayApi.syncPlayJoinGroup(JoinGroupRequestDto(groupId))
                adoptGroup { it.groupId == groupId }
            }

        /**
         * Falls „GroupJoined“ über den WebSocket nicht ankommt (Verbindung erst im Aufbau),
         * die eigene Gruppe aus der Gruppenliste des Servers übernehmen.
         */
        private suspend fun adoptGroup(match: (GroupInfoDto) -> Boolean) {
            repeat(ADOPT_ATTEMPTS) {
                delay(ADOPT_RETRY_MS)
                if (group.value != null) return
                val list = api.syncPlayApi.syncPlayGetGroups().content
                groups.value = list
                list.firstOrNull(match)?.let {
                    group.value = it
                    return
                }
            }
        }

        fun leaveGroup() =
            request {
                api.syncPlayApi.syncPlayLeaveGroup()
                reset()
            }

        fun requestPause() = request { api.syncPlayApi.syncPlayPause() }

        fun requestUnpause() = request { api.syncPlayApi.syncPlayUnpause() }

        fun requestSeek(positionMs: Long) = request { api.syncPlayApi.syncPlaySeek(SeekRequestDto(positionMs * TICKS_PER_MS)) }

        /** Neues Objekt für die ganze Gruppe starten (wir haben lokal etwas anderes gestartet). */
        fun setNewQueue(
            itemId: UUID,
            positionMs: Long,
        ) = request {
            api.syncPlayApi.syncPlaySetNewQueue(PlayRequestDto(queueFor(itemId), 0, positionMs * TICKS_PER_MS))
        }

        /**
         * Warteschlange wie jellyfin-web sie baut: bei einer Folge alle Folgen der Serie ab dieser.
         * jellyfin-web erweitert eine einzelne Folge selbst so und scheitert sonst an der kürzeren Liste.
         */
        private suspend fun queueFor(itemId: UUID): List<UUID> {
            val item = api.userLibraryApi.getItem(itemId = itemId).content
            val seriesId = item.seriesId
            if (item.type != BaseItemKind.EPISODE || seriesId == null) return listOf(itemId)
            val episodes =
                api.tvShowsApi
                    .getEpisodes(seriesId = seriesId, isMissing = false)
                    .content.items
                    .filter { it.locationType != LocationType.VIRTUAL }
                    .map { it.id }
            val start = episodes.indexOf(itemId)
            return if (start < 0) listOf(itemId) else episodes.drop(start)
        }

        /** Meldet dem Server „bereit“ bzw. „puffert“ (wie PlaybackCore.sendBufferingRequest in jellyfin-web). */
        fun sendReady(
            ready: Boolean,
            positionMs: Long,
            isPlaying: Boolean,
        ) {
            val playlistItemId = currentQueueItem()?.playlistItemId ?: return
            val now = toServerDate(System.currentTimeMillis() + timeOffsetMs)
            val ticks = positionMs * TICKS_PER_MS
            request {
                if (ready) {
                    api.syncPlayApi.syncPlayReady(ReadyRequestDto(now, ticks, isPlaying, playlistItemId))
                } else {
                    api.syncPlayApi.syncPlayBuffering(BufferRequestDto(now, ticks, isPlaying, playlistItemId))
                }
            }
        }

        fun currentQueueItem() = playQueue.value?.let { it.playlist.getOrNull(it.playingItemIndex) }

        /** Lokale Uhrzeit (ms), zu der ein Server-Zeitpunkt eintritt. */
        fun serverToLocalMs(serverDate: LocalDateTime) = serverDate.toEpochMs() - timeOffsetMs

        /** Geschätzte aktuelle Position (ms), wenn zum Server-Zeitpunkt [whenDate] bei [positionTicks] gestartet wurde. */
        fun estimatePositionMs(
            positionTicks: Long,
            whenDate: LocalDateTime,
        ) = extrapolatePositionMs(positionTicks, serverToLocalMs(whenDate), System.currentTimeMillis())

        private fun request(
            @StringRes errorMessage: Int? = null,
            block: suspend () -> Unit,
        ) = scope.launch {
            runCatching { block() }.onFailure {
                Timber.w(it, "SyncPlay-Anfrage fehlgeschlagen")
                if (errorMessage != null) showToast(context, context.getString(errorMessage))
            }
        }

        /** WebSocket-Befehle abonnieren und Zeitabgleich starten, solange wir (potenziell) in einer Gruppe sind. */
        private fun listen() {
            if (groupJob?.isActive == true) return
            groupJob =
                scope.launch {
                    launch {
                        api.webSocket.subscribe<SyncPlayCommandMessage>().collect { msg ->
                            msg.data?.takeIf { group.value != null }?.let { lastCommand.value = it }
                        }
                    }
                    var pings = 0
                    while (true) {
                        if (group.value != null) {
                            runCatching { syncTime() }.onFailure { Timber.w(it, "SyncPlay: Zeitabgleich fehlgeschlagen") }
                            pings++
                        }
                        delay(if (pings <= FAST_PINGS) FAST_PING_MS else SLOW_PING_MS)
                    }
                }
        }

        /** Eine Zeitmessung (NTP-artig) wie TimeSync/TimeSyncServer in jellyfin-web, Ping an den Server melden. */
        private suspend fun syncTime() {
            val sent = System.currentTimeMillis()
            val response = api.timeSyncApi.getUtcTime().content
            val received = System.currentTimeMillis()
            samples.addLast(
                TimeSample.of(sent, response.requestReceptionTime.toEpochMs(), response.responseTransmissionTime.toEpochMs(), received),
            )
            if (samples.size > TIME_SAMPLES) samples.removeFirst()
            val best = samples.minBy { it.roundTripMs }
            timeOffsetMs = best.offsetMs
            api.syncPlayApi.syncPlayPing(PingRequestDto(best.roundTripMs / 2))
        }

        private fun reset() {
            group.value = null
            playQueue.value = null
            lastCommand.value = null
            samples.clear()
            groupJob?.cancel()
            refreshGroups()
        }

        private suspend fun onGroupUpdate(update: JsonObject) {
            val type = update["Type"]?.jsonPrimitive?.content?.let { GroupUpdateType.fromNameOrNull(it) } ?: return
            val data = update["Data"]
            val json = ApiSerializer.json
            Timber.d("SyncPlay: Gruppen-Update %s", type)
            when (type) {
                GroupUpdateType.GROUP_JOINED -> onJoined(json.decodeFromJsonElement(GroupInfoDto.serializer(), data!!))
                GroupUpdateType.USER_JOINED -> onMemberChange(data?.jsonPrimitive?.content, joined = true)
                GroupUpdateType.USER_LEFT -> onMemberChange(data?.jsonPrimitive?.content, joined = false)
                GroupUpdateType.GROUP_LEFT, GroupUpdateType.NOT_IN_GROUP -> reset()
                GroupUpdateType.PLAY_QUEUE -> onPlayQueue(json.decodeFromJsonElement(PlayQueueUpdate.serializer(), data!!))
                GroupUpdateType.STATE_UPDATE -> Unit
                else -> showToast(context, context.getString(R.string.syncplay_error) + " ($type)")
            }
        }

        private suspend fun onJoined(info: GroupInfoDto) {
            group.value = info
            showToast(context, context.getString(R.string.syncplay_joined, info.groupName))
        }

        private suspend fun onMemberChange(
            user: String?,
            joined: Boolean,
        ) {
            if (user == null) return
            group.update { g -> g?.copy(participants = if (joined) g.participants + user else g.participants - user) }
            showToast(context, context.getString(if (joined) R.string.syncplay_user_joined else R.string.syncplay_user_left, user))
        }

        /** Position eines Serverbefehls jetzt: nach „weiter“ seit dem Befehl weitergezählt, sonst fest. */
        private fun SendCommand.positionNowMs() =
            if (command == SendCommandType.UNPAUSE) estimatePositionMs(positionTicks ?: 0, `when`) else (positionTicks ?: 0) / TICKS_PER_MS

        /** Wie QueueCore.updatePlayQueue/startPlayback in jellyfin-web: Gruppenobjekt hier ebenfalls starten. */
        private suspend fun onPlayQueue(queue: PlayQueueUpdate) {
            val previous = playQueue.value
            if (previous != null && queue.lastUpdate <= previous.lastUpdate) return
            playQueue.value = queue
            if (queue.reason in QueueOnlyReasons) return
            val item = currentQueueItem() ?: return
            val command = lastCommand.value
            val positionMs =
                when {
                    command != null && command.playlistItemId == item.playlistItemId && command.emittedAt >= queue.lastUpdate -> {
                        command
                            .positionNowMs()
                    }

                    queue.isPlaying -> {
                        estimatePositionMs(queue.startPositionTicks, queue.lastUpdate)
                    }

                    else -> {
                        queue.startPositionTicks / TICKS_PER_MS
                    }
                }
            withContext(Dispatchers.Main) {
                val stack = navigationManager.backStack
                val top = stack.lastOrNull()
                if (top is Destination.Playback && top.itemId == item.itemId) return@withContext // läuft schon
                val destination = Destination.Playback(item.itemId, positionMs.coerceAtLeast(0))
                if (top is Destination.Playback || top is Destination.PlaybackList) {
                    stack[stack.lastIndex] = destination
                } else {
                    navigationManager.navigateTo(destination)
                }
            }
        }
    }

internal fun LocalDateTime.toEpochMs() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

internal fun toServerDate(epochMs: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault())

/** Position (ms) zum Zeitpunkt [nowMs], wenn zur lokalen Zeit [startedAtMs] bei [positionTicks] gestartet wurde. */
internal fun extrapolatePositionMs(
    positionTicks: Long,
    startedAtMs: Long,
    nowMs: Long,
) = positionTicks / TICKS_PER_MS + (nowMs - startedAtMs)

/** „Schnicks Lobby“: deutscher Genitiv ohne Apostroph, außer bei s/ß/x/z am Ende („Jonas’ Lobby“); andere Sprachen unverändert. */
internal fun lobbyOwner(
    name: String,
    language: String,
) = when {
    language != "de" -> name
    name.lowercase().last() in "sßxz" -> "$name’"
    else -> "${name}s"
}

/** Eine NTP-artige Zeitmessung wie TimeSync in jellyfin-web, alle Werte in ms. */
internal data class TimeSample(
    /** Umlaufzeit ohne die Bearbeitungszeit des Servers. */
    val roundTripMs: Long,
    /** Serverzeit minus lokale Zeit. */
    val offsetMs: Long,
) {
    companion object {
        fun of(
            sent: Long,
            serverReceived: Long,
            serverSent: Long,
            received: Long,
        ) = TimeSample(
            roundTripMs = (received - sent) - (serverSent - serverReceived),
            offsetMs = ((serverReceived - sent) + (serverSent - received)) / 2,
        )
    }
}

/**
 * Das Jellyfin-SDK verwirft beim SyncPlay-Gruppen-Update die eigentlichen Daten (Warteschlange,
 * Gruppeninfo). Diese Hülle um die WebSocket-Verbindung liest die Rohnachrichten mit und gibt
 * Gruppen-Updates an [SyncPlayManager] weiter. Eingebunden in AppModule.
 */
class SyncPlaySocketTap(
    private val delegate: SocketConnectionFactory,
) : SocketConnectionFactory {
    override fun create(
        clientOptions: HttpClientOptions,
        scope: CoroutineScope,
    ): SocketConnection {
        val connection = delegate.create(clientOptions, scope)
        return object : SocketConnection by connection {
            override val state: StateFlow<SocketConnectionState> = TappedState(connection.state)
        }
    }

    @OptIn(ExperimentalForInheritanceCoroutinesApi::class)
    private class TappedState(
        private val source: StateFlow<SocketConnectionState>,
    ) : StateFlow<SocketConnectionState> by source {
        override suspend fun collect(collector: FlowCollector<SocketConnectionState>): Nothing =
            source.collect {
                if (it is SocketConnectionState.Message && it.message.contains("\"SyncPlayGroupUpdate\"")) tap(it.message)
                collector.emit(it)
            }
    }

    companion object {
        val groupUpdates = MutableSharedFlow<JsonObject>(extraBufferCapacity = 32)
        private var lastMessageId: String? = null

        @Synchronized
        private fun tap(raw: String) {
            runCatching {
                val message = ApiSerializer.json.parseToJsonElement(raw).jsonObject
                val id = message["MessageId"]?.jsonPrimitive?.content
                if (id == lastMessageId) return // das SDK liest den Zustand zweimal
                lastMessageId = id
                message["Data"]?.jsonObject?.let { groupUpdates.tryEmit(it) }
            }.onFailure { Timber.w(it, "SyncPlay: Gruppen-Update nicht lesbar") }
        }
    }
}
