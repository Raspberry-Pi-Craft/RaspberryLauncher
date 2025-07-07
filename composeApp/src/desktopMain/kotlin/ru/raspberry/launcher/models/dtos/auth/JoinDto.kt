package ru.raspberry.launcher.models.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class JoinDto(
    val accessToken: String,
    val selectedProfile: String,
    val serverId: String
)