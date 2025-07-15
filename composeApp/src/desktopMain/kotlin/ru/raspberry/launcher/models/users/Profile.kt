package ru.raspberry.launcher.models.users

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String
)