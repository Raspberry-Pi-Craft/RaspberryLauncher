package ru.raspberry.launcher.models.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val username: String,
    val password: String,
    val clientToken: String,
    val requestUser: Boolean = false
)