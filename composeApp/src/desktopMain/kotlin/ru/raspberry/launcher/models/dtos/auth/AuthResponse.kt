package ru.raspberry.launcher.models.dtos.auth

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.users.Profile

@Serializable
data class AuthResponse (
    val accessToken: String,
    val clientToken: String,
    val availableProfiles: List<Profile>? = null,
    val selectedProfile: Profile

)
