package ru.raspberry.launcher.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerData(
    val serverName: String,
    val description: String,
    val imageUrl: String,
)