package ru.raspberry.launcher.models.users.access

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.ChangeAction

@Serializable
data class AccessChange(
    val action: ChangeAction,
    val user: String,
    val newUser: String? = null
)