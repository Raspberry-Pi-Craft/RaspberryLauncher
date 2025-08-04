package ru.raspberry.launcher.windows.dialogs.admin

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.admin
import raspberrylauncher.composeapp.generated.resources.banned
import raspberrylauncher.composeapp.generated.resources.close
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.components.Spinner
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.users.UserInfo
import ru.raspberry.launcher.models.users.UserSelector
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun UserDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
) {
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(600.dp, 400.dp)
    )
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = mutableStateOf(null),
            close = close,
            title = "Users Management",
        )
    }
    DialogWindow(
        onCloseRequest = close,
        state = windowState,
        undecorated = true,
        resizable = false
    ) {
        roundCorners(window)
        AppTheme(
            theme = state.theme,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppHeader(
                    dialogData = dialogData
                )
            },
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
        ) {
            var username by remember { mutableStateOf("") }
            var filter by remember { mutableStateOf(UserSelector.Any) }
            var users by remember { mutableStateOf(emptyList<String>()) }
            val cache = remember { mutableMapOf<String, UserInfo>() }
            val tokens = remember { mutableMapOf<String, MutableSet<String>>() }
            val coroutine = rememberCoroutineScope()
            remember {
                coroutine.launch {
                    users = state.launcherService.listUsers(filter)
                    for (user in users) {
                        val t = mutableSetOf<String>()
                        val dim = 3
                        for (i in 0..user.length - dim)
                            t.add(user.substring(i, i + dim))
                        tokens[user] = t
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
            ) {
                item {
                    TextField(
                        value = username,
                        onValueChange = {
                            username = it
                            val qt = mutableSetOf<String>()
                            val dim = 3
                            for (i in 0 .. username.length - dim)
                                qt.add(username.substring(i, i + dim))
                            users = users.sortedBy {
                                qt.size - (tokens[it]?.intersect(qt)?.size ?: 0)
                            }
                        },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
                item {
                    Spinner(
                        options = UserSelector.entries,
                        selectedOption = filter,
                        onOptionSelected = {
                            filter = it
                            coroutine.launch {
                                users = state.launcherService.listUsers(filter)
                                for (user in users) {
                                    val t = mutableSetOf<String>()
                                    val dim = 3
                                    for (i in 0..user.length - dim)
                                        t.add(user.substring(i, i + dim))
                                    tokens[user] = t
                                }
                            }
                        },
                        label = { Text("Filter") },
                        toText = { it?.name ?: "Not selected" },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
                items(users) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(it)
                        val info = runBlocking {
                            val info = state.launcherService.getUserInfo(it)
                            if (info != null) cache[it] = info
                            cache[it]
                        }
                        if (info != null) {
                            var isAdmin by remember(it, info) { mutableStateOf(info.isAdmin) }
                            var isBanned by remember(it, info) { mutableStateOf(info.isBanned) }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    if (isAdmin)
                                        coroutine.launch {
                                            state.launcherService.deop(it)
                                            val info = state.launcherService.getUserInfo(it)
                                            if (info != null) cache[it] = info
                                            isAdmin = false
                                        }
                                    else
                                        coroutine.launch {
                                            state.launcherService.op(it)
                                            val info = state.launcherService.getUserInfo(it)
                                            if (info != null) cache[it] = info
                                            isAdmin = true
                                        }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.admin),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isAdmin)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (isBanned)
                                        coroutine.launch {
                                            state.launcherService.unban(it)
                                            val info = state.launcherService.getUserInfo(it)
                                            if (info != null) cache[it] = info
                                            isBanned = false
                                        }
                                    else
                                        coroutine.launch {
                                            state.launcherService.ban(it)
                                            val info = state.launcherService.getUserInfo(it)
                                            if (info != null) cache[it] = info
                                            isBanned = true
                                        }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.banned),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isBanned)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
