package ru.raspberry.launcher.models.repo.versions

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftVersionList(
    val versions: List<MinecraftVersionInfo>,
)