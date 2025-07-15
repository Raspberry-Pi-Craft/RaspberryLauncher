package ru.raspberry.launcher.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class LauncherInfo(
    val version: String,
    val downloadUrl: String,
    val lastUpdated: String
)