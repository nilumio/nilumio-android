package com.github.damontecres.wholphin.ui.setup

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.JellyfinServer
import com.github.damontecres.wholphin.data.model.JellyfinUser
import com.github.damontecres.wholphin.services.FriendInvite
import com.github.damontecres.wholphin.ui.components.BasicDialog
import com.github.damontecres.wholphin.ui.components.CircularProgress
import com.github.damontecres.wholphin.ui.components.EditTextBox
import com.github.damontecres.wholphin.ui.components.TextButton
import com.github.damontecres.wholphin.ui.launchIO
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.api.CreateUserByName
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject

data class FriendInviteState(
    val loading: Boolean = true,
    val isAdmin: Boolean = false,
    val suggestedUrl: String = "",
    val working: Boolean = false,
    val error: String? = null,
    val link: String? = null,
)

/**
 * Creates a non-admin account for a friend on the current server and builds a [FriendInvite] link
 */
@HiltViewModel
class FriendInviteViewModel
    @Inject
    constructor(
        private val jellyfin: Jellyfin,
    ) : ViewModel() {
        val state = MutableStateFlow(FriendInviteState())

        private fun api(
            server: JellyfinServer,
            user: JellyfinUser?,
        ) = jellyfin.createApi(server.url, accessToken = user?.takeIf { it.serverId == server.id }?.accessToken)

        fun init(
            server: JellyfinServer,
            user: JellyfinUser?,
        ) {
            state.value = FriendInviteState()
            viewModelScope.launchIO {
                try {
                    val api = api(server, user)
                    val isAdmin = api.accessToken != null && api.userApi.getCurrentUser().content.policy?.isAdministrator == true
                    val localAddress =
                        if (isAdmin) runCatching { api.systemApi.getPublicSystemInfo().content.localAddress }.getOrNull() else null
                    val suggested = listOfNotNull(server.url, localAddress).firstOrNull { !isPrivateAddress(it) }.orEmpty()
                    state.update { it.copy(loading = false, isAdmin = isAdmin, suggestedUrl = suggested) }
                } catch (ex: Exception) {
                    Timber.w(ex, "Error checking admin rights")
                    state.update { it.copy(loading = false, error = ex.localizedMessage) }
                }
            }
        }

        fun createInvite(
            server: JellyfinServer,
            user: JellyfinUser?,
            name: String,
            address: String,
        ) {
            state.update { it.copy(working = true, error = null) }
            viewModelScope.launchIO {
                try {
                    val api = api(server, user)
                    val ownPolicy = api.userApi.getCurrentUser().content.policy
                    val password = randomPassword()
                    val friend by api.userApi.createUserByName(CreateUserByName(name.trim(), password))
                    friend.policy?.let { policy ->
                        api.userApi.updateUserPolicy(
                            friend.id,
                            policy.copy(
                                isAdministrator = false,
                                enableRemoteAccess = true,
                                enableAllFolders = ownPolicy?.enableAllFolders ?: policy.enableAllFolders,
                                enabledFolders = ownPolicy?.enabledFolders ?: policy.enabledFolders,
                            ),
                        )
                    }
                    val link = FriendInvite(address.trim().trimEnd('/'), friend.name ?: name.trim(), password, server.name).toLink()
                    state.update { it.copy(working = false, link = link) }
                } catch (ex: Exception) {
                    Timber.w(ex, "Error creating friend account")
                    state.update { it.copy(working = false, error = ex.localizedMessage) }
                }
            }
        }

        companion object {
            private const val ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"

            fun randomPassword(): String {
                val random = SecureRandom()
                return (1..20).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
            }

            /** True for addresses a friend outside the home network cannot reach */
            fun isPrivateAddress(url: String): Boolean {
                val host = Uri.parse(if ("://" in url) url else "http://$url").host?.lowercase() ?: return true
                return host == "localhost" ||
                    host.endsWith(".local") ||
                    host.endsWith(".lan") ||
                    host.endsWith(".home") ||
                    host.endsWith(".fritz.box") ||
                    Regex("""^(10|127)\..*|^192\.168\..*|^169\.254\..*|^172\.(1[6-9]|2\d|3[01])\..*""").matches(host) ||
                    host.startsWith("[fd") ||
                    host.startsWith("[fe80")
            }

            fun qrCode(text: String): Bitmap {
                val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1))
                val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                return bitmap
            }
        }
    }

@Composable
fun FriendInviteDialog(
    server: JellyfinServer,
    currentUser: JellyfinUser?,
    onDismissRequest: () -> Unit,
    viewModel: FriendInviteViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.init(server, currentUser) }
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var address by remember(state.suggestedUrl) { mutableStateOf(state.suggestedUrl) }

    BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .focusGroup()
                    .padding(24.dp)
                    .fillMaxWidth(.5f),
        ) {
            Text(
                text = stringResource(R.string.add_friend),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val link = state.link
            when {
                state.loading || state.working -> {
                    CircularProgress(Modifier.size(32.dp))
                }

                link != null -> {
                    val qr = remember(link) { FriendInviteViewModel.qrCode(link).asImageBitmap() }
                    Image(
                        bitmap = qr,
                        contentDescription = stringResource(R.string.add_friend),
                        filterQuality = FilterQuality.None,
                        modifier =
                            Modifier
                                .size(240.dp)
                                .background(Color.White),
                    )
                    InfoText(stringResource(R.string.friend_invite_ready))
                    Text(
                        text = link,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                    )
                    InfoText(stringResource(R.string.friend_invite_secret))
                    TextButton(stringRes = R.string.friend_done, onClick = onDismissRequest)
                }

                !state.isAdmin -> {
                    InfoText(state.error ?: stringResource(R.string.friend_only_admins))
                    TextButton(stringRes = R.string.friend_done, onClick = onDismissRequest)
                }

                else -> {
                    Text(text = stringResource(R.string.friend_name), color = MaterialTheme.colorScheme.onSurface)
                    EditTextBox(value = name, onValueChange = { name = it })
                    Text(text = stringResource(R.string.friend_server_address), color = MaterialTheme.colorScheme.onSurface)
                    EditTextBox(value = address, onValueChange = { address = it }, placeholder = { Text("https://…") })
                    InfoText(stringResource(R.string.friend_reachability_hint))
                    state.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
                    TextButton(
                        stringRes = R.string.friend_create_invite,
                        enabled = name.isNotBlank() && address.isNotBlank(),
                        onClick = { viewModel.createInvite(server, currentUser, name, address) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
