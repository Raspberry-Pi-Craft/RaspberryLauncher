package ru.raspberry.launcher.models.server

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.server.files.ServerFileChange
import ru.raspberry.launcher.models.users.access.AccessChange
import ru.raspberry.launcher.models.users.access.UserAccess

@Serializable
data class ServerChanges(
    var name: String? = null,
    var description: String? = null,
    var imageUrl: String? = null,
    var visibleWithoutAuth: Boolean? = null,
    var visibleWithoutAccess: Boolean? = null,
    var access: UserAccess? = null,
    var accessUsers: List<AccessChange>? = null,
    var address: String? = null,
    var minecraft: String? = null,
    var files: List<ServerFileChange>? = null
)