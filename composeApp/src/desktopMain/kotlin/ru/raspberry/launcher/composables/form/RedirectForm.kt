package ru.raspberry.launcher.composables.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.close
import ru.raspberry.launcher.composables.components.Accordion
import ru.raspberry.launcher.composables.components.Spinner
import ru.raspberry.launcher.models.ChangeAction
import ru.raspberry.launcher.models.DataAccess
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.redirect.HeaderChange
import ru.raspberry.launcher.models.redirect.RedirectChanges
import ru.raspberry.launcher.models.users.access.AccessChange
import ru.raspberry.launcher.models.users.access.UserAccess
import ru.raspberry.launcher.service.repositories.RedirectRepository


class RedirectForm<S>(
    private val state: WindowData<S>
) : AbstractForm<String> {
    private val repository = RedirectRepository(state.launcherService)

    @Composable
    override fun Add(): () -> Unit {
        var name by remember { mutableStateOf("") }
        TextField(
            value = name,
            onValueChange = {
                name = it
            },
            singleLine = true,
            label = { Text("Name") },
            isError = name.isBlank(),
            supportingText = {
                if (name.isBlank()) {
                    Text(
                        "Name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )
        var url by remember { mutableStateOf("") }
        TextField(
            value = url,
            onValueChange = {
                url = it
            },
            singleLine = true,
            label = { Text("URL") },
            isError = url.isBlank(),
            supportingText = {
                if (url.isBlank()) {
                    Text(
                        "URL cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )
        return remember {
            {
                runBlocking {
                    repository.add(Pair(name, url))
                }
            }
        }
    }

    @Composable
    override fun Update(key: String): () -> Unit {
        val data = remember {
            runBlocking {
                repository.get(key)
            }
        }
        var name by remember { mutableStateOf(data?.name) }
        TextField(
            value = name ?: "",
            onValueChange = {
                name = it.ifBlank { null }
            },
            singleLine = true,
            label = { Text("Name") },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )
        var url by remember { mutableStateOf(data?.url) }
        TextField(
            value = url ?: "",
            onValueChange = {
                url = it.ifBlank { null }
            },
            singleLine = true,
            label = { Text("URL") },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )
        var access by remember { mutableStateOf(data?.access) }
        Spinner(
            label = { Text("User access") },
            options = UserAccess.entries,
            selectedOption = access ?: UserAccess.NotBanned,
            onOptionSelected = {
                access = it
            },
            toText = { (it ?: UserAccess.NotBanned).name },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )
        val accessUserChanges = remember { mutableMapOf<String, AccessChange>() }
        val accessUsers = remember {
            data?.accessUsers?.associateBy { it }?.toMutableMap() ?: mutableMapOf()
        }
        Accordion(
            title = { Text("Accessed Users") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                var expanded by remember { mutableStateOf(false) }
                var value by remember { mutableStateOf("") }
                AnimatedVisibility(
                    visible = expanded,
                ) {
                    TextField(
                        value = value,
                        onValueChange = { value = it },
                        singleLine = true,
                        label = { Text("Username") },
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        isError = value.isBlank(),
                        supportingText = {
                            if (value.isBlank()) {
                                Text(
                                    "User cannot be empty",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
                Button(
                    onClick = {
                        if (expanded) {
                            accessUsers[value] = value
                            accessUserChanges[value] = AccessChange(
                                action = ChangeAction.Add,
                                user = value,
                            )
                        }
                        expanded = !expanded
                    },
                    modifier = Modifier.padding(8.dp).fillMaxWidth(0.8f),
                ) {
                    Text(
                        if (expanded) "Apply" else "Add Accessed User"
                    )
                }
                accessUsers.forEach { (key, value) ->
                    var enabled by remember { mutableStateOf(accessUsers.containsKey(key)) }
                    AnimatedVisibility(visible = enabled) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        ) {
                            var user by remember { mutableStateOf(value) }
                            TextField(
                                value = user,
                                onValueChange = {
                                    user = it
                                    when (accessUserChanges[key]?.action) {
                                        ChangeAction.Add -> {
                                            accessUsers.remove(key)
                                            accessUserChanges.remove(key)
                                            accessUsers[it] = it
                                            accessUserChanges[it] = AccessChange(
                                                action = ChangeAction.Add,
                                                user = it
                                            )
                                        }

                                        ChangeAction.Remove ->
                                            throw Exception("WTF? Change after remove!")

                                        ChangeAction.Change, null -> {
                                            accessUsers[key] = it
                                            accessUserChanges[key] = AccessChange(
                                                action = ChangeAction.Change,
                                                user = key,
                                                newUser = it
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                label = { Text("Username") },
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    accessUsers.remove(key)
                                    when (accessUserChanges[key]?.action) {
                                        ChangeAction.Add ->
                                            accessUserChanges.remove(key)

                                        ChangeAction.Remove ->
                                            throw Exception("WTF? Remove after remove!")

                                        ChangeAction.Change, null ->
                                            accessUserChanges[key] = AccessChange(
                                                action = ChangeAction.Remove,
                                                user = key
                                            )
                                    }
                                    enabled = false
                                },
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.close),
                                    contentDescription = "Delete Accessed User"
                                )
                            }
                        }
                    }
                }
            }
        }
        val headerChanges = remember { mutableMapOf<String, HeaderChange>() }
        if (data?.headers != null) {
            val headers = remember { data.headers.toMutableMap() }
            Accordion(
                title = { Text("Headers") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    var header by remember { mutableStateOf("") }
                    var value by remember { mutableStateOf("") }
                    AnimatedVisibility(
                        visible = expanded,
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        ) {
                            TextField(
                                value = header,
                                onValueChange = { header = it },
                                singleLine = true,
                                label = { Text("Name") },
                                modifier = Modifier.weight(1f),
                                isError = header.isBlank(),
                                supportingText = {
                                    if (header.isBlank()) {
                                        Text(
                                            "Header cannot be empty",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                },
                            )
                            TextField(
                                value = value,
                                onValueChange = { value = it },
                                singleLine = true,
                                label = { Text("Changes") },
                                modifier = Modifier.weight(1f),
                                isError = value.isBlank(),
                                supportingText = {
                                    if (value.isBlank()) {
                                        Text(
                                            "Header value cannot be empty",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                },
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (expanded) {
                                headers.put(header, value)
                                headerChanges[header] = HeaderChange(
                                    action = ChangeAction.Add,
                                    value = value
                                )
                            }
                            expanded = !expanded
                        },
                        modifier = Modifier.padding(8.dp).fillMaxWidth(0.8f),
                    ) {
                        Text(
                            if (expanded) "Apply" else "Add Header"
                        )
                    }
                    headers.forEach { (key, value) ->
                        var enabled by remember { mutableStateOf(headers.containsKey(key)) }
                        AnimatedVisibility(visible = enabled) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            ) {
                                Text(
                                    text = key,
                                    modifier = Modifier.weight(1f),
                                )
                                var headerValue by remember { mutableStateOf(value) }
                                TextField(
                                    value = headerValue,
                                    onValueChange = {
                                        headerValue = it
                                        headers.put(key, it)
                                        when (headerChanges[key]?.action) {
                                            ChangeAction.Add -> {
                                                headerChanges[key] = HeaderChange(
                                                    action = ChangeAction.Add,
                                                    value = it
                                                )
                                            }

                                            ChangeAction.Remove ->
                                                throw Exception("WTF? Change after remove!")

                                            ChangeAction.Change, null ->
                                                headerChanges[key] = HeaderChange(
                                                    action = ChangeAction.Change,
                                                    value = it
                                                )
                                        }
                                    },
                                    singleLine = true,
                                    label = { Text("URL") },
                                    modifier = Modifier.weight(3f),
                                )
                                IconButton(
                                    onClick = {
                                        headers.remove(key)
                                        when (headerChanges[key]?.action) {
                                            ChangeAction.Add ->
                                                headerChanges.remove(key)

                                            ChangeAction.Remove ->
                                                throw Exception("WTF? Remove after remove!")

                                            ChangeAction.Change, null ->
                                                headerChanges[key] = HeaderChange(
                                                    action = ChangeAction.Remove,
                                                    value = key
                                                )
                                        }
                                        enabled = false
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.close),
                                        contentDescription = "Delete Header"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        var dataAccess by remember { mutableStateOf(data?.dataAccess) }
        if (data?.dataAccess != null) {
            Spinner(
                label = { Text("User access") },
                options = DataAccess.entries.toList(),
                selectedOption = dataAccess ?: DataAccess.AuthorOnly,
                onOptionSelected = {
                    dataAccess = it
                },
                toText = { (it ?: DataAccess.AuthorOnly).name },
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
            )
        }
        val dataAccessUserChanges = remember { mutableMapOf<String, AccessChange>() }
        if (data?.dataAccessUsers != null) {
            val dataAccessUsers = remember {
                data.dataAccessUsers.associateBy { it }.toMutableMap()
            }
            Accordion(
                title = { Text("Data Accessed Users") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    var value by remember { mutableStateOf("") }
                    AnimatedVisibility(
                        visible = expanded,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextField(
                            value = value,
                            onValueChange = { value = it },
                            singleLine = true,
                            label = { Text("Changes") },
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            isError = value.isBlank(),
                            supportingText = {
                                if (value.isBlank()) {
                                    Text(
                                        "User cannot be empty",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        )
                    }
                    Button(
                        onClick = {
                            if (expanded) {
                                dataAccessUsers[value] = value
                                dataAccessUserChanges[value] = AccessChange(
                                    action = ChangeAction.Add,
                                    user = value,
                                )
                            }
                            expanded = !expanded
                        },
                        modifier = Modifier.padding(8.dp).fillMaxWidth(0.8f),
                    ) {
                        Text(
                            if (expanded) "Apply" else "Add Accessed User"
                        )
                    }
                    dataAccessUsers.forEach { (key, value) ->
                        var enabled by remember { mutableStateOf(dataAccessUsers.containsKey(key)) }
                        AnimatedVisibility(visible = enabled) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            ) {
                                var user by remember { mutableStateOf(value) }
                                TextField(
                                    value = user,
                                    onValueChange = {
                                        user = it
                                        when (dataAccessUserChanges[key]?.action) {
                                            ChangeAction.Add -> {
                                                dataAccessUsers.remove(key)
                                                dataAccessUserChanges.remove(key)
                                                dataAccessUsers[it] = it
                                                dataAccessUserChanges[it] = AccessChange(
                                                    action = ChangeAction.Add,
                                                    user = it
                                                )
                                            }

                                            ChangeAction.Remove ->
                                                throw Exception("WTF? Change after remove!")

                                            ChangeAction.Change, null -> {
                                                dataAccessUsers[key] = it
                                                dataAccessUserChanges[key] = AccessChange(
                                                    action = ChangeAction.Change,
                                                    user = key,
                                                    newUser = it
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    label = { Text("User") },
                                    modifier = Modifier.weight(3f),
                                )
                                IconButton(
                                    onClick = {
                                        dataAccessUsers.remove(key)
                                        when (dataAccessUserChanges[key]?.action) {
                                            ChangeAction.Add ->
                                                dataAccessUserChanges.remove(key)

                                            ChangeAction.Remove ->
                                                throw Exception("WTF? Remove after remove!")

                                            ChangeAction.Change, null ->
                                                dataAccessUserChanges[key] = AccessChange(
                                                    action = ChangeAction.Remove,
                                                    user = key
                                                )
                                        }
                                        enabled = false
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.close),
                                        contentDescription = "Delete Accessed User"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return remember {
            {
                runBlocking {
                    val accessUsersList = accessUserChanges.values.toList()
                    val dataAccessUsersList = dataAccessUserChanges.values.toList()
                    repository.edit(
                        key, RedirectChanges(
                            name = if (name == data?.name) null else name,
                            url = if (url == data?.url) null else url,
                            access = if (access == data?.access) null else access,
                            accessUsers = accessUsersList.ifEmpty { null },
                            headers = headerChanges.ifEmpty { null },
                            dataAccess = if (dataAccess == data?.dataAccess) null else dataAccess,
                            dataAccessUsers = dataAccessUsersList.ifEmpty { null },
                        )
                    )
                }
            }
        }
    }
}