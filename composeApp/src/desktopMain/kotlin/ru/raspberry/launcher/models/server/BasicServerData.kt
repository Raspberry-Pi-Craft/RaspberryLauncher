package ru.raspberry.launcher.models.server

import kotlinx.serialization.Serializable

@Serializable
data class BasicServerData(
    val name: String,
    val description: String?,
    val imageUrl: String?,
)