package ru.raspberry.launcher.models.users

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val isAdmin: Boolean,
)