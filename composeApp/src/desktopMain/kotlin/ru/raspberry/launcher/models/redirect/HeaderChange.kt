package ru.raspberry.launcher.models.redirect

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.ChangeAction

@Serializable
data class HeaderChange(
    val action: ChangeAction,
    val value: String?,
)