package ru.raspberry.launcher.windows.dialogs.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import raspberrylauncher.composeapp.generated.resources.close
import raspberrylauncher.composeapp.generated.resources.edit
import ru.raspberry.launcher.composables.components.AppHeader
import ru.raspberry.launcher.composables.form.AbstractForm
import ru.raspberry.launcher.composables.screens.main.DialogType
import ru.raspberry.launcher.models.DialogData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.service.AsyncRepository
import ru.raspberry.launcher.theme.AppTheme
import ru.raspberry.launcher.tools.roundCorners
import ru.raspberry.launcher.windows.MainWindowScreens
import ru.raspberry.launcher.windows.dialogs.TwoVariantDialog
import kotlin.collections.getOrDefault

private enum class DialogMode {
    VIEW,
    CREATE,
    UPDATE,
    DELETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun<K, U, D, A> CRUDAbstractDialog(
    state: WindowData<MainWindowScreens>,
    close: () -> Unit,
    title: String,
    changeDialog: (DialogType, Map<String, Any>) -> Unit,
    repository: AsyncRepository<K, U, D, A>,
    titlePresenter: (K, D?) -> String,
    form: AbstractForm<K>,
) {
    val windowState = rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(600.dp, 400.dp)
    )
    val currentScreen = remember { mutableStateOf(DialogMode.VIEW) }
    val dialogData = remember {
        DialogData(
            parent = state,
            dialogState = windowState,
            currentScreen = currentScreen,
            close = {
                if (currentScreen.value == DialogMode.VIEW) close()
                else currentScreen.value = DialogMode.VIEW
            },
            title = title
        )
    }
    val cache = remember { mutableMapOf<K, D>() }
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
            var reloader by remember { mutableStateOf(false) }
            val selected = remember { mutableStateOf<K?>(null) }
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,

            ) {
                when (currentScreen.value) {
                    DialogMode.VIEW ->
                        view(
                            repository = repository,
                            titlePresenter = titlePresenter,
                            currentScreen = currentScreen,
                            selected = selected,
                            reload = { reloader = !reloader },
                            cache = cache
                        )

                    DialogMode.CREATE -> {
                        selected.value = null
                        form(
                            repository = repository,
                            form = form,
                            selected = selected,
                            currentScreen = currentScreen,
                            reload = { reloader = !reloader }
                        )
                    }

                    DialogMode.UPDATE -> form(
                        repository = repository,
                        form = form,
                        selected = selected,
                        currentScreen = currentScreen,
                        reload = { reloader = !reloader }
                    )

                    DialogMode.DELETE -> {
                        view(
                            repository = repository,
                            titlePresenter = titlePresenter,
                            currentScreen = currentScreen,
                            selected = selected,
                            reload = { reloader = !reloader },
                            cache = cache
                        )
                        remove(
                            state = state,
                            currentScreen = currentScreen,
                            selected = selected.value,
                            repository = repository,
                            reload = { reloader = !reloader }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun<K, U, D, A> view(
    repository: AsyncRepository<K, U, D, A>,
    titlePresenter: (K, D?) -> String,
    currentScreen: MutableState<DialogMode>,
    selected: MutableState<K?>,
    reload: () -> Unit,
    cache: MutableMap<K, D>,
) {
    var keys by remember { mutableStateOf<List<K>?>(null) }
    val coroutine = rememberCoroutineScope()
    remember { coroutine.launch { keys = repository.list() } }
    if (keys != null) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(keys!!) {
                var data by remember {
                    mutableStateOf(
                        cache.getOrDefault(it, null)
                    )
                }
                remember { coroutine.launch { data = repository.get(it) } }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = titlePresenter(it, data),
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(0.8f)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            selected.value = it
                            currentScreen.value = DialogMode.UPDATE
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.edit),
                            contentDescription = "Edit",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            selected.value = it
                            currentScreen.value = DialogMode.DELETE
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = "Delete",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            item { // Add new button
                Button(
                    onClick = { currentScreen.value = DialogMode.CREATE },
                    modifier = Modifier.fillMaxWidth(0.6f),
                ) {
                    Text("Add")
                }
            }
        }
    }
    else {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.6f),
        )
    }
}

@Composable
private fun<K, U, D, A> form(
    repository: AsyncRepository<K, U, D, A>,
    form: AbstractForm<K>,
    selected: MutableState<K?>,
    currentScreen: MutableState<DialogMode>,
    reload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val key = selected.value
        val action = if (key == null) form.Add() else form.Update(key)
        Button(
            onClick = {
                action()
                currentScreen.value = DialogMode.VIEW
            },
            modifier = Modifier.fillMaxWidth(0.6f),
        ) {
            Text(
                if (key == null) "Add New" else "Edit",
            )
        }
    }
}

@Composable
private fun<K, U, D, A> remove(
    repository: AsyncRepository<K, U, D, A>,
    state: WindowData<MainWindowScreens>,
    currentScreen: MutableState<DialogMode>,
    selected: K?,
    reload: () -> Unit,
) {
    if (selected == null) return
    TwoVariantDialog(
        state,
        { currentScreen.value = DialogMode.VIEW },
        "Delete Confirmation",
        {
            Text("Are you sure you want to delete this?")
        },
        {
            Text("Delete")
        },
        {
            runBlocking {
                repository.remove(selected)
                reload()
            }
            currentScreen.value = DialogMode.VIEW
        },
        {
            Text("Cancel")
        },
        {
            currentScreen.value = DialogMode.VIEW
        },
        reload,
    )
}