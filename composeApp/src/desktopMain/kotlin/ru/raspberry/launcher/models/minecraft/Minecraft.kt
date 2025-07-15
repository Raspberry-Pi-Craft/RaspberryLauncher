package ru.raspberry.launcher.models.minecraft

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.OS

@Serializable
data class Minecraft(
    val loader: MinecraftLoader,
    val version: String,
    val downloadUrl: Map<OS, String>,
    val arguments: String,
    val osSpecificArguments: Map<OS, String>,
)