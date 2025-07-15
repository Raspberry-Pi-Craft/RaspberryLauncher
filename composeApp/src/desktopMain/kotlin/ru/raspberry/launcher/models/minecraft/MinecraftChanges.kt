package ru.raspberry.launcher.models.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftChanges(
    val loader: MinecraftLoader? = null,
    val version: String? = null,
    val downloadUrl: String? = null,
    val arguments: String? = null,
    val osSpecificArguments: String? = null,
)