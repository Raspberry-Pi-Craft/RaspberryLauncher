package ru.raspberry.launcher.models.server

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.repo.versions.MinecraftVersionInfo
import ru.raspberry.launcher.models.server.files.ServerFile

@Serializable
data class AdvancedServerData(
    val address: String,
    val minecraft : MinecraftVersionInfo?,
    val files : List<ServerFile>,
    val createdAt: String,
    val updatedAt: String,
    val updatedBy: String = "system"
){
}