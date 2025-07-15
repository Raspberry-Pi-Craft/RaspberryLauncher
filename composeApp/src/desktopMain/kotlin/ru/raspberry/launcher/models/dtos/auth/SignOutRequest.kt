package ru.raspberry.launcher.models.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class SignOutRequest(
    val username: String,
    val password: String
)