package ru.raspberry.launcher.models.server

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.java.JavaData
import ru.raspberry.launcher.models.minecraft.MinecraftData
import ru.raspberry.launcher.models.server.files.ServerFile

@Serializable
data class AdvancedServerData(
    val address: String,
    val java : JavaData?,
    val minecraft : MinecraftData?,
    val serverFiles : List<ServerFile>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val updatedBy: String = "system"
){
}