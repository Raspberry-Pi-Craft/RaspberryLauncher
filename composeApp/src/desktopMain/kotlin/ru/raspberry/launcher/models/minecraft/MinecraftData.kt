package ru.raspberry.launcher.models.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftData(
    val loader: MinecraftLoader,
    val version: String,
    val downloadUrl: String?,
    val arguments: String,
    val osSpecificArguments: String?
)