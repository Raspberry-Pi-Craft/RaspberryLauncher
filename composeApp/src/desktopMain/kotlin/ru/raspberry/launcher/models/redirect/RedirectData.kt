package ru.raspberry.launcher.models.redirect

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.users.access.UserAccess

@Serializable
data class RedirectData(
    val name: String,
    val url: String,
    val headers: Map<String, String>?,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String,
    val updatedBy: String
)
