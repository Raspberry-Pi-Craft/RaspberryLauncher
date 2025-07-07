package ru.raspberry.launcher.models.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto (
    val accessToken: String,
    val clientToken: String,
    val availableProfiles: List<ProfileDto>? = null,
    val selectedProfile: ProfileDto

)
