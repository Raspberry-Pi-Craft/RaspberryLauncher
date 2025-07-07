package ru.raspberry.launcher.models.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val name: String
)