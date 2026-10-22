/*
 * nilumio – Seite „Gemeinsam schauen“ (SyncPlay), Copyright (C) 2026 nilumio, GPL-2.0.
 * Aufbau angelehnt an die Gruppenauswahl von jellyfin-web (src/plugins/syncPlay/ui/groupSelectionMenu.js, GPL-2.0).
 */
package com.github.damontecres.wholphin.ui.syncplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.services.syncplay.SyncPlayViewModel
import com.github.damontecres.wholphin.ui.components.TextButton
import com.github.damontecres.wholphin.ui.tryRequestFocus

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
        manager.refreshGroups()
        focusRequester.tryRequestFocus()
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(top = 32.dp),
    ) {
        item { Text(stringResource(R.string.syncplay_title), style = MaterialTheme.typography.headlineMedium) }
        val current = group
        if (current != null) {
            item { Text(current.groupName, style = MaterialTheme.typography.titleLarge) }
            item { Text(stringResource(R.string.syncplay_participants, current.participants.joinToString())) }
            item { Text(stringResource(R.string.syncplay_hint)) }
            item {
                TextButton(R.string.syncplay_leave, onClick = { manager.leaveGroup() }, modifier = Modifier.focusRequester(focusRequester))
            }
        } else {
            item {
                TextButton(R.string.syncplay_create, onClick = { manager.createGroup() }, modifier = Modifier.focusRequester(focusRequester))
            }
            item {
                Text(
                    stringResource(if (groups.isEmpty()) R.string.syncplay_no_groups else R.string.syncplay_join),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(groups, key = { it.groupId }) {
                ListItem(
                    selected = false,
                    onClick = { manager.joinGroup(it.groupId) },
                    headlineContent = { Text(it.groupName) },
                    supportingContent = { Text(stringResource(R.string.syncplay_participants, it.participants.joinToString())) },
                )
            }
            item { TextButton(R.string.syncplay_refresh, onClick = { manager.refreshGroups() }) }
        }
    }
}
