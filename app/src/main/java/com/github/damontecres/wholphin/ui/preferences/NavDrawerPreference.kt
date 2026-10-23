package com.github.damontecres.wholphin.ui.preferences

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import com.github.damontecres.wholphin.ui.components.NordlichtIcons
import com.github.damontecres.wholphin.ui.tryRequestFocus
import kotlinx.coroutines.delay
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerPreferencesDao
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.NavDrawerPinnedItem
import com.github.damontecres.wholphin.data.model.NavPinType
import com.github.damontecres.wholphin.services.BackdropService
import com.github.damontecres.wholphin.services.NavDrawerService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.SeerrServerRepository
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.ui.components.BasicDialog
import com.github.damontecres.wholphin.ui.components.Button
import com.github.damontecres.wholphin.ui.launchDefault
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.nav.NavDrawerItem
import com.github.damontecres.wholphin.util.ExceptionHandler
import com.github.damontecres.wholphin.util.WholphinDispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class NavDrawerPin(
    val id: String,
    val title: String,
    val pinned: Boolean,
    val item: NavDrawerItem,
) {
    companion object {
        fun create(
            context: Context,
            items: Map<NavDrawerItem, Boolean>,
        ) {
            items.map { (item, pinned) ->
                NavDrawerPin(item.id, item.name(context), pinned, item)
            }
        }
    }
}

// nilumio: Menü-Editor mit zwei Listen („Im Menü“ / „Nicht im Menü“)
private enum class PinButton { UP, DOWN, REMOVE, ADD }

private val EditorPanel = Color(0xFF111726)
private val EditorMuted = Color(0xFF8E9AB0)

@Composable
fun NavDrawerPreference(
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    viewModel: NavDrawerPreferencesViewModel = hiltViewModel(),
) {
    val items by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    ClickPreference(
        title = title,
        summary = summary,
        onClick = { showDialog = true },
        interactionSource = interactionSource,
        modifier = modifier,
    )
    if (showDialog) {
        NavDrawerPreferenceDialog(
            items = items,
            onUpdate = viewModel::update,
            onDismissRequest = {
                viewModel.save()
                showDialog = false
            },
        )
    }
}

@Composable
fun NavDrawerPreferenceDialog(
    items: List<NavDrawerPin>,
    onUpdate: (List<NavDrawerPin>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val pinned = items.filter { it.pinned }
    val unpinned = items.filterNot { it.pinned }
    // Ein FocusRequester je Eintrag und Knopf, damit der Fokus nach dem Verschieben mitwandert
    val requesters = remember { mutableMapOf<Pair<String, PinButton>, FocusRequester>() }

    fun requester(
        id: String,
        button: PinButton,
    ) = requesters.getOrPut(id to button) { FocusRequester() }
    var pendingFocus by remember { mutableStateOf<Pair<String, PinButton>?>(null) }
    LaunchedEffect(items.isNotEmpty()) {
        if (pendingFocus == null) {
            pendingFocus = pinned.firstOrNull()?.let { it.id to PinButton.REMOVE } ?: unpinned.firstOrNull()?.let { it.id to PinButton.ADD }
        }
    }
    LaunchedEffect(pendingFocus, items) {
        val target = pendingFocus ?: return@LaunchedEffect
        // Neue Liste wird evtl. erst im nächsten Frame gezeichnet
        repeat(10) {
            if (requesters[target]?.tryRequestFocus() == true) return@LaunchedEffect
            delay(32)
        }
    }

    // Reihenfolge wird immer als „angeheftet, dann übrige“ gespeichert
    fun commit(
        newPinned: List<NavDrawerPin>,
        newUnpinned: List<NavDrawerPin>,
        focus: Pair<String, PinButton>,
    ) {
        onUpdate(newPinned.map { it.copy(pinned = true) } + newUnpinned.map { it.copy(pinned = false) })
        pendingFocus = focus
    }

    BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        elevation = 3.dp,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .width(820.dp)
                    .height(460.dp)
                    .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_menu_title),
                fontSize = 24.sp,
                color = Color.White,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                PinPanel(
                    title = stringResource(R.string.nav_menu_in),
                    emptyText = stringResource(R.string.nav_menu_none_in),
                    isEmpty = pinned.isEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    pinned.forEachIndexed { index, item ->
                        key(item.id) {
                            PinRow(item.title) {
                                PinIconButton(
                                    icon = NordlichtIcons.ChevronUp,
                                    description = stringResource(R.string.nav_menu_move_up),
                                    active = index > 0,
                                    focusRequester = requester(item.id, PinButton.UP),
                                ) {
                                    val list = pinned.toMutableList().apply { add(index - 1, removeAt(index)) }
                                    commit(list, unpinned, item.id to if (index - 1 == 0) PinButton.DOWN else PinButton.UP)
                                }
                                PinIconButton(
                                    icon = NordlichtIcons.ChevronDown,
                                    description = stringResource(R.string.nav_menu_move_down),
                                    active = index < pinned.lastIndex,
                                    focusRequester = requester(item.id, PinButton.DOWN),
                                ) {
                                    val list = pinned.toMutableList().apply { add(index + 1, removeAt(index)) }
                                    commit(list, unpinned, item.id to if (index + 1 == pinned.lastIndex) PinButton.UP else PinButton.DOWN)
                                }
                                PinIconButton(
                                    icon = NordlichtIcons.ArrowRight,
                                    description = stringResource(R.string.nav_menu_remove),
                                    active = true,
                                    focusRequester = requester(item.id, PinButton.REMOVE),
                                ) {
                                    val list = pinned.toMutableList().apply { removeAt(index) }
                                    // Fokus bleibt links auf dem nachrückenden Eintrag
                                    val next = list.getOrNull(index.coerceAtMost(list.lastIndex))
                                    commit(list, unpinned + item, next?.let { it.id to PinButton.REMOVE } ?: (item.id to PinButton.ADD))
                                }
                            }
                        }
                    }
                }
                PinPanel(
                    title = stringResource(R.string.nav_menu_out),
                    emptyText = stringResource(R.string.nav_menu_all_in),
                    isEmpty = unpinned.isEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    unpinned.forEachIndexed { index, item ->
                        key(item.id) {
                            PinRow(item.title, buttonsFirst = true) {
                                PinIconButton(
                                    icon = NordlichtIcons.ArrowLeft,
                                    description = stringResource(R.string.nav_menu_add),
                                    active = true,
                                    focusRequester = requester(item.id, PinButton.ADD),
                                ) {
                                    val list = unpinned.toMutableList().apply { removeAt(index) }
                                    val next = list.getOrNull(index.coerceAtMost(list.lastIndex))
                                    commit(pinned + item, list, next?.let { it.id to PinButton.ADD } ?: (item.id to PinButton.REMOVE))
                                }
                            }
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.nav_menu_more_hint),
                fontSize = 14.sp,
                color = EditorMuted,
            )
        }
    }
}

@Composable
private fun PinPanel(
    title: String,
    emptyText: String,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            modifier
                .fillMaxHeight()
                .background(EditorPanel, RoundedCornerShape(14.dp))
                .padding(12.dp),
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            color = EditorMuted,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            if (isEmpty) {
                Text(
                    text = emptyText,
                    fontSize = 15.sp,
                    color = EditorMuted,
                    modifier = Modifier.padding(8.dp),
                )
            }
            content()
        }
    }
}

@Composable
private fun PinRow(
    title: String,
    buttonsFirst: Boolean = false,
    buttons: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
    ) {
        if (buttonsFirst) buttons()
        Text(
            text = title,
            fontSize = 17.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        if (!buttonsFirst) buttons()
    }
}

/** Runder Icon-Knopf; inaktive Knöpfe bleiben fokussierbar, damit die Spalten für das Steuerkreuz stabil bleiben. */
@Composable
private fun PinIconButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = { if (active) onClick() },
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF182033),
                focusedContainerColor = Color(0xFF243049),
            ),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.5.dp, Color.White), inset = 0.dp, shape = shape)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier.size(40.dp).focusRequester(focusRequester),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (active) Color.White else Color.White.copy(alpha = .25f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun MoveButton(
    @StringRes icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) = Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.size(32.dp),
) {
    Text(
        text = stringResource(icon),
        fontSize = 16.sp,
        fontFamily = FontAwesome,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@HiltViewModel
class NavDrawerPreferencesViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        val navigationManager: NavigationManager,
        val backdropService: BackdropService,
        private val serverRepository: ServerRepository,
        private val navDrawerService: NavDrawerService,
        private val serverPreferencesDao: ServerPreferencesDao,
        private val seerrServerRepository: SeerrServerRepository,
    ) : ViewModel() {
        val state = MutableStateFlow<List<NavDrawerPin>>(listOf())

        init {
            viewModelScope.launchDefault {
                val state = navDrawerService.state.value
                val user = serverRepository.currentUser
                val seerr = seerrServerRepository.active.firstOrNull()
                if (user == null || seerr == null) {
                    return@launchDefault
                }
                val navDrawerPins =
                    withContext(WholphinDispatchers.IO) {
                        serverPreferencesDao
                            .getNavDrawerPinnedItems(user)
                            .associateBy { it.itemId }
                    }
                val allItems = state.let { it.items + it.moreItems }
                val pins =
                    allItems
                        .sortedBy {
                            navDrawerPins[it.id]?.order?.takeIf { it >= 0 } ?: Int.MAX_VALUE
                        }.mapNotNull {
                            if (!seerr && it is NavDrawerItem.Discover) {
                                null
                            } else {
                                // Assume pinned if unknown
                                val pinned = navDrawerPins[it.id]?.type ?: NavPinType.PINNED
                                NavDrawerPin(
                                    it.id,
                                    it.name(context),
                                    pinned == NavPinType.PINNED,
                                    it,
                                )
                            }
                        }
                this@NavDrawerPreferencesViewModel.state.value = pins
            }
        }

        fun update(items: List<NavDrawerPin>) {
            state.update { items }
        }

        fun save() {
            viewModelScope.launchIO(ExceptionHandler(true)) {
                serverRepository.currentUser?.let { user ->
                    serverRepository.currentUserDto?.let { userDto ->
                        if (user.id == userDto.id) {
                            val toSave =
                                state.value.mapIndexed { index, item ->
                                    NavDrawerPinnedItem(
                                        user.rowId,
                                        item.id,
                                        if (item.pinned) NavPinType.PINNED else NavPinType.UNPINNED,
                                        index,
                                    )
                                }
                            serverPreferencesDao.saveNavDrawerPinnedItems(*toSave.toTypedArray())
                            val discoverActive = seerrServerRepository.active.firstOrNull() == true
                            navDrawerService.updateNavDrawer(user, userDto, discoverActive)
                        } else {
                            throw IllegalStateException("User IDs do not match")
                        }
                    }
                }
            }
        }
    }
