package ru.raspberry.launcher.models.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class TokenRequestDto(
    val accessToken: String,
    val clientToken: String? = null,
    val requestUser: Boolean = false
)