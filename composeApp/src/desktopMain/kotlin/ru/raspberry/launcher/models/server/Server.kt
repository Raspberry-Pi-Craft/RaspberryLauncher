package ru.raspberry.launcher.models.server

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.server.files.ServerFile
import ru.raspberry.launcher.models.users.access.UserAccess

@Serializable
data class Server(
    val name: String,
    val description: Map<String, String>,
    val imageUrl: String?,
    val visibleWithoutAuth: Boolean,
    val visibleWithoutAccess: Boolean,
    val access: UserAccess,
    val accessUsers: List<String>,
    val address: String,
    val minecraft: String,
    val files: List<ServerFile>,
    val createdAt: String,
    val updatedAt: String,
    val updatedBy: String = "system"
)