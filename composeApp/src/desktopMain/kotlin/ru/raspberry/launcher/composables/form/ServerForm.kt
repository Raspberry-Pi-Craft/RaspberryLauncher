package ru.raspberry.launcher.composables.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.close
import raspberrylauncher.composeapp.generated.resources.edit
import ru.raspberry.launcher.composables.components.Accordion
import ru.raspberry.launcher.composables.components.Spinner
import ru.raspberry.launcher.models.ChangeAction
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.server.ServerChanges
import ru.raspberry.launcher.models.server.files.ServerFile
import ru.raspberry.launcher.models.server.files.ServerFileChange
import ru.raspberry.launcher.models.server.files.Side
import ru.raspberry.launcher.models.users.access.AccessChange
import ru.raspberry.launcher.models.users.access.UserAccess
import ru.raspberry.launcher.service.repositories.ServerRepository
import kotlin.collections.ifEmpty
import kotlin.collections.set


class ServerForm<S>(
    private val state: WindowData<S>
) : AbstractForm<String> {
    private val repository = ServerRepository(state.launcherService)

    @Composable
    override fun Add(): () -> Unit {
        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
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
        TextField(
            value = address,
            onValueChange = {
                address = it
            },
            singleLine = true,
            label = { Text("Server Address") },
            isError = address.isBlank(),
            supportingText = {
                if (address.isBlank()) {
                    Text(
                        "Address cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )
        val minecraftList = remember { runBlocking { state.launcherService.listMinecraft() } }
        var minecraft by remember { mutableStateOf(minecraftList.firstOrNull() ?: "") }
        Spinner(
            options = minecraftList,
            selectedOption = minecraft,
            onOptionSelected = {
                minecraft = it
            },
            label = { Text("Minecraft") },
            toText = {
                if (it == null || it.isBlank()) "No minecraft selected"
                else it
            },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )

        return remember {
            {
                runBlocking {
                    repository.add(Triple(name, address, minecraft))
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

        var locale by remember { mutableStateOf(state.language) }
        Spinner(
            options = state.languages.values,
            selectedOption = locale,
            onOptionSelected = {
                locale = it
            },
            label = { Text("Language") },
            toText = { it?.name ?: "No language selected" },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )
        Spacer(modifier = Modifier.padding(16.dp))

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
        var description by remember { mutableStateOf(data?.description[locale.id]) }
        TextField(
            value = description ?: "",
            onValueChange = {
                description = it.ifBlank { null }
            },
            label = { Text("Description") },
            modifier = Modifier.padding(8.dp).fillMaxWidth().height(200.dp),
        )
        var imageUrl by remember { mutableStateOf(data?.imageUrl) }
        TextField(
            value = imageUrl ?: "",
            onValueChange = {
                imageUrl = it.ifBlank { null }
            },
            singleLine = true,
            label = { Text("Image URL") },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )

        var address by remember { mutableStateOf(data?.address) }
        TextField(
            value = address ?: "",
            onValueChange = {
                address = it.ifBlank { null }
            },
            singleLine = true,
            label = { Text("Server Address") },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
        )
        val minecrafts = remember { runBlocking { state.launcherService.listMinecraft() } }
        var minecraft by remember { mutableStateOf(data?.minecraft) }
        Spinner(
            options = minecrafts,
            selectedOption = minecraft,
            onOptionSelected = {
                minecraft = it
            },
            label = { Text("Minecraft") },
            toText = {
                if (it == null || it.isBlank()) "No minecraft selected" else it
            },
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            searchable = true,
        )

        val fileChanges = remember { mutableMapOf<String, ServerFileChange>() }
        val files = remember {
            data?.files?.associateBy { it.fileId }?.toMutableMap() ?: mutableMapOf()
        }
        Accordion(
            title = { Text("Files") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                var expanded by remember { mutableStateOf(false) }
                var fileId by remember { mutableStateOf("") }
                var path by remember { mutableStateOf("") }
                var downloadUrl by remember { mutableStateOf("") }
                var clientSide by remember { mutableStateOf(false) }
                var serverSide by remember { mutableStateOf(false) }
                var hashAlgorithm by remember { mutableStateOf("SHA256") }
                var hashType by remember { mutableStateOf("Binary") }
                AnimatedVisibility(
                    visible = expanded,
                ) {
                    Column {
                        TextField(
                            value = fileId,
                            onValueChange = { fileId = it },
                            singleLine = true,
                            label = { Text("File ID") },
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            isError = fileId.isBlank(),
                            supportingText = {
                                if (fileId.isBlank())
                                    Text(
                                        "File ID cannot be empty",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                            }
                        )
                        TextField(
                            value = path,
                            onValueChange = { path = it },
                            singleLine = true,
                            label = { Text("Path") },
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            isError = path.isBlank(),
                            supportingText = {
                                if (path.isBlank())
                                    Text(
                                        "Path cannot be empty",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                            }
                        )
                        TextField(
                            value = downloadUrl,
                            onValueChange = { downloadUrl = it },
                            singleLine = true,
                            label = { Text("Download URL") },
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            isError = downloadUrl.isBlank(),
                            supportingText = {
                                if (downloadUrl.isBlank())
                                    Text(
                                        "Download URL cannot be empty",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Client")
                                Checkbox(
                                    checked = clientSide,
                                    onCheckedChange = { clientSide = it }
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Server")
                                Checkbox(
                                    checked = serverSide,
                                    onCheckedChange = { serverSide = it }
                                )
                            }
                        }
                        Spinner(
                            label = { Text("Hash Algorithm") },
                            options = listOf("MD5", "SHA1", "SHA256", "SHA512"),
                            selectedOption = hashAlgorithm,
                            onOptionSelected = { hashAlgorithm = it },
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            toText = { it ?: "SHA512" }
                        )
                        Spinner(
                            label = { Text("Hash Type") },
                            options = listOf("Auto", "Json", "Binary"),
                            selectedOption = hashType,
                            onOptionSelected = { hashType = it },
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            toText = { it ?: "Binary" }
                        )
                    }
                }
                Button(
                    onClick = {
                        if (expanded) {
                            val side = listOfNotNull(
                                if (clientSide) Side.Client else null,
                                if (serverSide) Side.Server else null,
                            )
                            files[fileId] = ServerFile(
                                fileId = fileId,
                                path = path,
                                downloadUrl = downloadUrl,
                                side = side,
                                hashAlgorithm = hashAlgorithm,
                                hashType = hashType,
                                size = 0,
                                hash = "",
                                createdAt = "",
                                updatedAt = ""
                            )
                            fileChanges[fileId] = ServerFileChange(
                                action = ChangeAction.Add,
                                fileId = fileId,
                                path = path,
                                downloadUrl = downloadUrl,
                                side = side,
                                hashAlgorithm = hashAlgorithm,
                                hashType = hashType,
                            )
                        }
                        expanded = !expanded
                    },
                    modifier = Modifier.padding(8.dp).fillMaxWidth(0.8f),
                ) { Text(if (expanded) "Apply" else "Add File") }
                files.forEach { (key, value) ->
                    var enabled by remember { mutableStateOf(files.containsKey(key)) }
                    AnimatedVisibility(visible = enabled) {
                        var editing by remember { mutableStateOf(false) }
                        var fileId by remember { mutableStateOf<String?>(value.fileId) }
                        var path by remember { mutableStateOf<String?>(value.path) }
                        var downloadUrl by remember { mutableStateOf<String?>(value.downloadUrl) }
                        var clientSide by remember { mutableStateOf(value.side.contains(Side.Client)) }
                        var serverSide by remember { mutableStateOf(value.side.contains(Side.Server)) }
                        var hashAlgorithm by remember { mutableStateOf<String?>(value.hashAlgorithm) }
                        var hashType by remember { mutableStateOf<String?>(value.hashType) }
                        Accordion(
                            title = { Text(value.fileId) },
                            additionalIcons = {
                                Icon(
                                    painter = painterResource(Res.drawable.edit),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable(
                                            onClick = {
                                                if (editing) {
                                                    val side = listOfNotNull(
                                                        if (clientSide) Side.Client else null,
                                                        if (serverSide) Side.Server else null,
                                                    )
                                                    when (fileChanges[key]?.action) {
                                                        ChangeAction.Add -> {
                                                            files.remove(key)
                                                            fileChanges.remove(key)
                                                            val k = fileId ?: key;
                                                            files[k] = ServerFile(
                                                                fileId = k,
                                                                path = if (path == value.path) value.path else path!!,
                                                                downloadUrl = if (downloadUrl == value.downloadUrl) value.downloadUrl else downloadUrl!!,
                                                                side = side,
                                                                hashAlgorithm = if (hashAlgorithm == value.hashAlgorithm) value.hashAlgorithm else hashAlgorithm!!,
                                                                hashType = if (hashType == value.hashType) value.hashType else hashType!!,
                                                                size = 0,
                                                                hash = "",
                                                                createdAt = "",
                                                                updatedAt = ""
                                                            )
                                                            fileChanges[k] = ServerFileChange(
                                                                action = ChangeAction.Add,
                                                                fileId = k,
                                                                path = if (path == value.path) null else path,
                                                                side = side,
                                                                hashAlgorithm = if (hashAlgorithm == value.hashAlgorithm) null else hashAlgorithm!!,
                                                                hashType = if (hashType == value.hashType) null else hashType!!,
                                                            )
                                                        }

                                                        ChangeAction.Remove ->
                                                            throw Exception("WTF? Change after remove!")

                                                        ChangeAction.Change, null -> {
                                                            files[key] = ServerFile(
                                                                fileId = if (fileId == key) key else fileId!!,
                                                                path = if (path == value.path) value.path else path!!,
                                                                downloadUrl = if (downloadUrl == value.downloadUrl) value.downloadUrl else downloadUrl!!,
                                                                side = side,
                                                                hashAlgorithm = if (hashAlgorithm == value.hashAlgorithm) value.hashAlgorithm else hashAlgorithm!!,
                                                                hashType = if (hashType == value.hashType) value.hashType else hashType!!,
                                                                size = 0,
                                                                hash = "",
                                                                createdAt = "",
                                                                updatedAt = ""
                                                            )
                                                            fileChanges[key] = ServerFileChange(
                                                                action = ChangeAction.Change,
                                                                fileId = key,
                                                                newFileId = if (fileId == key) null else fileId,
                                                                path = if (path == value.path) null else path,
                                                                side = side,
                                                                hashAlgorithm = if (hashAlgorithm == value.hashAlgorithm) null else hashAlgorithm!!,
                                                                hashType = if (hashType == value.hashType) null else hashType!!,
                                                            )
                                                        }
                                                    }
                                                }
                                                editing = !editing
                                            }
                                        ),
                                )
                                Icon(
                                    painter = painterResource(Res.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable(
                                            onClick = {
                                                runBlocking {
                                                    files.remove(key)
                                                    when (fileChanges[key]?.action) {
                                                        ChangeAction.Add ->
                                                            fileChanges.remove(key)

                                                        ChangeAction.Remove ->
                                                            throw Exception("WTF? Remove after remove!")

                                                        ChangeAction.Change, null ->
                                                            fileChanges[key] = ServerFileChange(
                                                                action = ChangeAction.Remove,
                                                                fileId = key
                                                            )
                                                    }
                                                    enabled = false
                                                }
                                            }
                                        ),
                                )
                            },
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        ) {
                            Column {
                                TextField(
                                    value = fileId ?: "",
                                    onValueChange = { fileId = it.ifBlank { null } },
                                    singleLine = true,
                                    label = { Text("File ID") },
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    readOnly = !editing,
                                )
                                TextField(
                                    value = path ?: "",
                                    onValueChange = { path = it.ifBlank { null } },
                                    singleLine = true,
                                    label = { Text("Path") },
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    readOnly = !editing,
                                )
                                TextField(
                                    value = downloadUrl ?: "",
                                    onValueChange = { downloadUrl = it.ifBlank { null } },
                                    singleLine = true,
                                    label = { Text("Download URL") },
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    readOnly = !editing,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text("Client")
                                        Checkbox(
                                            checked = clientSide,
                                            onCheckedChange = { clientSide = it },
                                            enabled = editing,
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text("Server")
                                        Checkbox(
                                            checked = serverSide,
                                            onCheckedChange = { serverSide = it },
                                            enabled = editing,
                                        )
                                    }
                                }
                                Spinner(
                                    label = { Text("Hash Algorithm") },
                                    options = listOf("MD5", "SHA1", "SHA256", "SHA512"),
                                    selectedOption = hashAlgorithm,
                                    onOptionSelected = { hashAlgorithm = it },
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    toText = { it ?: "SHA512" },
                                    readOnly = !editing,
                                )
                                Spinner(
                                    label = { Text("Hash Type") },
                                    options = listOf("Auto", "Json", "Binary"),
                                    selectedOption = hashType,
                                    onOptionSelected = { hashType = it },
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    toText = { it ?: "Binary" },
                                    readOnly = !editing,
                                )
                            }
                        }
                    }
                }
            }
        }

        var visibleWithoutAuth by remember { mutableStateOf(data?.visibleWithoutAuth) }
        var visibleWithoutAccess by remember { mutableStateOf(data?.visibleWithoutAccess) }
        val accessUserChanges = remember { mutableMapOf<String, AccessChange>() }
        var access by remember { mutableStateOf(data?.access) }
        Accordion(
            title = { Text("Access") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Visible Without Auth",
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = visibleWithoutAuth ?: true,
                        onCheckedChange = {
                            visibleWithoutAuth = it
                        }
                    )
                }
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Visible Without Access",
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = visibleWithoutAccess ?: true,
                        onCheckedChange = {
                            visibleWithoutAccess = it
                        }
                    )
                }
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
                                label = { Text("User") },
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
                        ) { Text(if (expanded) "Apply" else "Add Accessed User") }

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
                                        label = { Text("URL") },
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
            }
        }
        val coroutine = rememberCoroutineScope()
        return remember {
            {
                coroutine.launch {
                    val accessUsersList = accessUserChanges.values.toList()
                    repository.edit(
                        key, Pair(
                            ServerChanges(
                                name = if (name == data?.name) null else name,
                                description = if (description == data?.description[locale.id]) null else description,
                                imageUrl = if (imageUrl == data?.imageUrl) null else imageUrl,
                                address = if (address == data?.address) null else address,
                                minecraft = if (minecraft == data?.minecraft) null else minecraft,
                                visibleWithoutAuth = if (visibleWithoutAuth == data?.visibleWithoutAuth) null else visibleWithoutAuth,
                                visibleWithoutAccess = if (visibleWithoutAccess == data?.visibleWithoutAccess) null else visibleWithoutAccess,
                                access = if (access == data?.access) null else access,
                                accessUsers = accessUsersList.ifEmpty { null },
                                files = fileChanges.values.toList(),
                            ),
                            locale.id
                        )
                    )
                }
            }
        }
    }
}