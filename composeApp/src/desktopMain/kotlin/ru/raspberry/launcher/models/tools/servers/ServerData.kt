package ru.raspberry.launcher.models.tools.servers

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.ChangeAction
import ru.raspberry.launcher.models.server.ServerChanges
import ru.raspberry.launcher.models.server.files.ServerFileChange
import ru.raspberry.launcher.models.users.access.AccessChange
import ru.raspberry.launcher.models.users.access.UserAccess

@Serializable
data class ServerData(
    var name: String,
    var address: String,
    var minecraft: String,
    var description: Map<String, String> = emptyMap(),
    var imageUrl: String? = null,
    var visibleWithoutAuth: Boolean = true,
    val visibleWithoutAccess: Boolean = false,
    var access: UserAccess = UserAccess.NotBanned,
    var accessUsers: List<AccessChange> = emptyList(),

    var files: List<ServerFileChange> = emptyList()
) {
    fun generateRoot(): Triple<String, String, String> =
        Triple(name, address, minecraft)
    fun generateChanges(): List<Pair<ServerChanges, String>> {
        val fileChanges = files.map {
            ServerFileChange(
                action = ChangeAction.Add,
                fileId = it.fileId,
                path = it.path,
                downloadUrl = it.downloadUrl,
                side = it.side,
                hashAlgorithm = it.hashAlgorithm,
                hashType = it.hashType,
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
                        files = fileChanges,
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