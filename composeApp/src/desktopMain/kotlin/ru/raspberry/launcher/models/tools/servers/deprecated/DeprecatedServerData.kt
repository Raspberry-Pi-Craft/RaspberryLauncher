package ru.raspberry.launcher.models.tools.servers.deprecated

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.ChangeAction
import ru.raspberry.launcher.models.server.ServerChanges
import ru.raspberry.launcher.models.server.files.ServerFileChange
import ru.raspberry.launcher.models.users.access.AccessChange
import ru.raspberry.launcher.models.users.access.UserAccess
import java.io.File

@Serializable
data class DeprecatedServerData(
    val name: String,
    var address: String,
    var minecraft: String,
    val description:  Map<String, String> = emptyMap(),
    var imageUrl: String? = null,
    var visibleWithoutAuth: Boolean = true,
    val visibleWithoutAccess: Boolean = false,
    var access: UserAccess = UserAccess.NotBanned,
    var accessUsers: List<AccessChange> = emptyList(),

    val files: List<DeprecatedServerFile> = emptyList(),
) {
    fun generateRoot(): Triple<String, String, String> =
        Triple(name, address, minecraft)
    fun generateChanges(): List<Pair<ServerChanges, String>> {
        val fileChanges = files.map {
            val file = File(it.path)
            ServerFileChange(
                action = ChangeAction.Add,
                fileId = file.nameWithoutExtension,
                path = it.path,
                downloadUrl = it.download,
                side = it.env,
                hashAlgorithm = "SHA256",
                hashType = when (file.extension) {
                    "json" -> "Json"
                    else -> "Binary"
                },
            )
        }
        val changes = mutableListOf<Pair<ServerChanges, String>>()
        if (description.isEmpty()) {
            changes.add(
                Pair(
                    ServerChanges(
                        imageUrl = imageUrl,
                        visibleWithoutAuth = visibleWithoutAuth,
                        visibleWithoutAccess = visibleWithoutAccess,
                        access = access,
                        accessUsers = accessUsers,
                        files = fileChanges
                    ),
                    "en",
                )
            )
        }
        else {
            var first = true
            description.forEach { (key, value) ->
                changes.add(
                    Pair(
                        if (first)
                            ServerChanges(
                                description = value,
                                imageUrl = imageUrl,
                                visibleWithoutAuth = visibleWithoutAuth,
                                visibleWithoutAccess = visibleWithoutAccess,
                                access = access,
                                accessUsers = accessUsers,
                                files = fileChanges,
                            )
                        else
                            ServerChanges(description = value),
                        key,
                    )
                )
                first = false
            }
        }
        return changes
    }
}