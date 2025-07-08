package ru.raspberry.launcher.models.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class AdminInfoDto(
    val isAdmin: Boolean,
)