package ru.raspberry.launcher.models.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class SignOutRequestDto(
    val username: String,
    val password: String
)