package ru.raspberry.launcher.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class LauncherInfoDto(
    val version: String,
    val downloadUrl: String,
    val lastUpdated: String
)