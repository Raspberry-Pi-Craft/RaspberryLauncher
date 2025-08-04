package ru.raspberry.launcher.models.repo.versions

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftVersionInfo(
    val id: String,
    val type: VersionType,
    val url: String,
)