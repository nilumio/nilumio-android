package com.github.damontecres.wholphin.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.ui.showToast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Invitation to a friend's server: s = public server address, u = user name, p = password, n = server name.
 * Travels only in the URL fragment, which browsers never send to nilumio.com. Never log it.
 */
@Serializable
data class FriendInvite(
    val s: String,
    val u: String,
    val p: String,
    val n: String? = null,
) {
    fun toLink(): String {
        val json = Json.encodeToString(serializer(), this).toByteArray()
        return LINK + "#" + Base64.encodeToString(json, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    override fun toString() = "FriendInvite(s=$s, u=$u, n=$n)"

    companion object {
        const val LINK = "https://nilumio.com/einladung"

        fun isInvite(uri: Uri?) =
            uri != null &&
                (
                    (uri.scheme == "nilumio" && uri.host == "einladung") ||
                        (uri.scheme == "https" && uri.host == "nilumio.com" && uri.path?.trimEnd('/') == "/einladung")
                )

        fun fromUri(uri: Uri?): FriendInvite? {
            val fragment = uri?.encodedFragment
            if (!isInvite(uri) || fragment.isNullOrBlank()) return null
            return try {
                val json = String(Base64.decode(fragment, Base64.URL_SAFE))
                Json { ignoreUnknownKeys = true }.decodeFromString(serializer(), json)
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Opens a [FriendInvite] link: adds the server and signs in with the invited account
 */
@Singleton
class FriendInviteService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val jellyfin: Jellyfin,
        private val serverRepository: ServerRepository,
        private val setupNavigationManager: SetupNavigationManager,
    ) {
        /**
         * @return null if the intent is no invitation, otherwise true if the user is now signed in with the invited account
         */
        suspend fun handle(intent: Intent?): Boolean? {
            if (!FriendInvite.isInvite(intent?.data)) return null
            return try {
                val invite = FriendInvite.fromUri(intent?.data) ?: throw IllegalArgumentException("Invalid invitation")
                val url =
                    jellyfin.discovery
                        .getRecommendedServers(invite.s)
                        .sortedBy { it.score }
                        .firstOrNull { it.score != RecommendedServerInfoScore.BAD }
                        ?.address ?: invite.s
                val auth by jellyfin.createApi(url).userApi.authenticateUserByName(invite.u, invite.p)
                val current = serverRepository.changeUser(url, auth, null)
                setupNavigationManager.navigateTo(SetupDestination.AppContent(current))
                true
            } catch (ex: Exception) {
                Timber.w("Could not open friend invite: %s", ex.javaClass.simpleName)
                showToast(context, context.getString(R.string.friend_invite_failed) + ": " + ex.localizedMessage)
                false
            }
        }
    }
