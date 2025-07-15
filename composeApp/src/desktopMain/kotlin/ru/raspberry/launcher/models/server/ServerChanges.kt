package ru.raspberry.launcher.models.server

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.server.files.ServerFileChanges
import ru.raspberry.launcher.models.users.access.AccessChange
import ru.raspberry.launcher.models.users.access.UserAccess

@Serializable
data class ServerChanges(
    val name: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val visibleWithoutAuth: Boolean? = null,
    val access: UserAccess? = null,
    val accessUsers: List<AccessChange>? = null,
    val address: String? = null,
    val java: String? = null,
    val minecraft: String? = null,
    val files: List<ServerFileChanges>? = null
)